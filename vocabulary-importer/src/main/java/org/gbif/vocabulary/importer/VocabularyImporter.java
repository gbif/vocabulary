package org.gbif.vocabulary.importer;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.client.VocabularyClient;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.enums.LanguageRegion;

import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VocabularyImporter {

  private final VocabularyClient vocabularyClient;
  private final ConceptClient conceptClient;

  public VocabularyImporter(VocabularyClient vocabularyClient, ConceptClient conceptClient) {
    this.vocabularyClient = vocabularyClient;
    this.conceptClient = conceptClient;
  }

  @SneakyThrows
  public void importVocabulary(
      String csvDelimiter,
      String vocabName,
      String vocabLabelEN,
      String vocabDefinitionEN,
      Path conceptsPath,
      Path hiddenLabelsPath) {

    // list to keep the errors and then print them to a file
    List<Error> errors = new ArrayList<>();

    // create vocabulary
    Vocabulary vocab = new Vocabulary();
    vocab.setName(vocabName);
    vocab.getLabel().put(LanguageRegion.ENGLISH, vocabLabelEN);
    if (!Strings.isNullOrEmpty(vocabDefinitionEN)) {
      vocab.getDefinition().put(LanguageRegion.ENGLISH, vocabDefinitionEN);
    }
    Vocabulary createdVocab = vocabularyClient.create(vocab);
    log.info("Created vocabulary {} with key {}", vocabName, createdVocab.getKey());

    // create the concepts
    Map<String, Concept> conceptsMap = new HashMap<>();
    Files.lines(conceptsPath)
        .skip(1) // we skip the column names
        .filter(l -> !Strings.isNullOrEmpty(l))
        .forEach(
            l -> {
              String[] values = l.split(csvDelimiter);
              String conceptName = values[0].trim();

              if (Strings.isNullOrEmpty(conceptName)) {
                log.error("Empty concept in line: {}", l);
                return;
              }

              Concept concept;
              boolean conceptExists = false;
              if (conceptsMap.containsKey(conceptName)) {
                concept = conceptsMap.get(conceptName);
                conceptExists = true;
              } else {
                concept = new Concept();
                concept.setName(conceptName);
                conceptsMap.put(conceptName, concept);
              }

              // parent
              if (values.length > 1 && !Strings.isNullOrEmpty(values[1])) {
                String parentName = values[1].trim();
                Concept parent = conceptsMap.get(parentName);
                if (parent != null) {
                  concept.setParentKey(parent.getKey());
                } else {
                  errors.add(
                      Error.of(
                          "Parent " + parentName + " not found for concept " + conceptName, null));
                  log.error("Parent {} not found for concept {}", parentName, conceptName);
                }
              }

              // add EN labels
              if (values.length > 2 && !Strings.isNullOrEmpty(values[2])) {
                concept.getLabel().put(LanguageRegion.ENGLISH, values[2].trim());
              }

              // add EN alternative labels
              if (values.length > 3 && !Strings.isNullOrEmpty(values[3])) {
                concept
                    .getAlternativeLabels()
                    .computeIfAbsent(LanguageRegion.ENGLISH, k -> new ArrayList<>())
                    .add(values[3].trim());
              }

              // add ES labels
              if (values.length > 4 && !Strings.isNullOrEmpty(values[4])) {
                concept.getLabel().put(LanguageRegion.SPANISH, values[4].trim());
              }

              // add ES alternative labels
              if (values.length > 5 && !Strings.isNullOrEmpty(values[5])) {
                concept
                    .getAlternativeLabels()
                    .computeIfAbsent(LanguageRegion.SPANISH, k -> new ArrayList<>())
                    .add(values[5].trim());
              }

              // add EN definitions
              if (values.length > 6 && !Strings.isNullOrEmpty(values[6])) {
                concept.getDefinition().put(LanguageRegion.ENGLISH, values[6].trim());
              }

              if (!conceptExists) {
                // create concept
                try {
                  Concept created = conceptClient.create(createdVocab.getName(), concept);
                  conceptsMap.put(created.getName(), created);
                  log.info("Created concept {} with key {}", created.getName(), created.getKey());
                } catch (Exception ex) {
                  errors.add(Error.of("Error creating concept " + concept.getName(), ex));
                  log.error("Cannot create concept {}", concept.getName(), ex);
                }
              } else {
                // update concept
                try {
                  Concept updated =
                      conceptClient.update(createdVocab.getName(), concept.getName(), concept);
                  log.info("Updated concept {} with key {}", updated.getName(), updated.getKey());
                } catch (Exception ex) {
                  errors.add(Error.of("Error updating concept " + concept.getName(), ex));
                  log.error("Cannot update concept {}", concept.getName(), ex);
                }
              }
            });

    // add hidden labels
    Files.lines(hiddenLabelsPath)
        .skip(1) // we skip the column names
        .filter(l -> !Strings.isNullOrEmpty(l))
        .forEach(
            l -> {
              String[] values = l.split(csvDelimiter);

              if (values.length < 2) {
                log.error("Missing fields for hidden value: {}", l);
                return;
              }

              String hiddenLabel = values[1].trim();
              log.info("Hidden label: {}", hiddenLabel);
              String conceptName = values[0].trim();
              Concept concept = conceptsMap.get(conceptName);

              if (concept == null) {
                errors.add(
                    Error.of(
                        conceptName + " concept doesn't exist for hidden label" + hiddenLabel,
                        null));
                log.error(
                    "Couldn't add hidden label {} because the concept {} doesn't exist",
                    hiddenLabel,
                    conceptName);
                return;
              }

              concept.getHiddenLabels().add(hiddenLabel);
              try {
                Concept updated =
                    conceptClient.update(createdVocab.getName(), concept.getName(), concept);
                conceptsMap.put(concept.getName(), updated);
              } catch (Exception ex) {
                concept.getHiddenLabels().remove(hiddenLabel);
                errors.add(
                    Error.of(
                        "Error adding hidden label "
                            + hiddenLabel
                            + " in concept "
                            + concept.getName(),
                        ex));
                log.error(
                    "Couldn't add hidden label {} in concept {}",
                    hiddenLabel,
                    concept.getName(),
                    ex);
              }
            });

    printErrorsToFile(errors);
  }

  @SneakyThrows
  private void printErrorsToFile(List<Error> errors) {
    if (errors.isEmpty()) {
      return;
    }

    Path errorsFile = Paths.get("errors_" + System.currentTimeMillis());
    try (BufferedWriter writer = Files.newBufferedWriter(errorsFile)) {
      for (Error error : errors) {
        writer.write(error.message);
        writer.newLine();
        if (error.exception != null) {
          writer.write(error.exception.getMessage());
          writer.newLine();
        }
        writer.newLine();
      }
    }
  }

  @AllArgsConstructor(staticName = "of")
  private static class Error {
    private final String message;
    private final Exception exception;
  }
}
