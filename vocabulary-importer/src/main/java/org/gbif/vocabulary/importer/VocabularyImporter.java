/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.vocabulary.importer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.api.VocabularyView;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.client.VocabularyClient;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Vocabulary;

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
      String listDelimiter,
      String vocabName,
      String vocabLabelEN,
      String vocabDefinitionEN,
      Path conceptsPath,
      Path hiddenLabelsPath,
      Charset charset) {

    // list to keep the errors and then print them to a file
    List<Error> errors = new ArrayList<>();

    // create vocabulary
    Vocabulary vocab = new Vocabulary();
    vocab.setName(vocabName);
    if (!Strings.isNullOrEmpty(vocabDefinitionEN)) {
      vocab.getDefinition().put(LanguageRegion.ENGLISH, vocabDefinitionEN);
    }
    VocabularyView createdVocab = vocabularyClient.create(vocab);
    log.info("Created vocabulary {} with key {}", vocabName, createdVocab.getVocabulary().getKey());

    Label vocabLabel =
        vocabularyClient.addLabel(
            vocabName,
            Label.builder().language(LanguageRegion.ENGLISH).value(vocabLabelEN).build());
    log.info("Added vocabulary label with key {}", vocabLabel.getKey());

    // create the concepts
    Map<String, Concept> conceptsMap = new HashMap<>();
    try (Stream<String> lines = Files.lines(conceptsPath)) {
      lines
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

                if (conceptsMap.containsKey(conceptName)) {
                  errors.add(Error.of("Concept " + conceptName + " already exists", null));
                  log.error("Concept {} already exists", conceptName);
                  return;
                }

                Concept concept = new Concept();
                concept.setName(conceptName);
                conceptsMap.put(conceptName, concept);

                // read fields
                parseConceptFields(listDelimiter, errors, conceptsMap, values, concept, vocabName);

                // create concept
                try {
                  ConceptView created =
                      conceptClient.create(createdVocab.getVocabulary().getName(), concept);
                  conceptsMap.put(created.getConcept().getName(), created.getConcept());
                  log.info(
                      "Created concept {} with key {}",
                      created.getConcept().getName(),
                      created.getConcept().getKey());
                } catch (Exception ex) {
                  errors.add(Error.of("Error creating concept " + concept.getName(), ex));
                  log.error("Cannot create concept {}", concept.getName(), ex);
                }
              });
    }

    // add hidden labels
    parseHiddenLabels(csvDelimiter, vocabName, hiddenLabelsPath, errors, conceptsMap, charset);

    printErrorsToFile(errors);
  }

  @SneakyThrows
  public void reimportVocabulary(
      String csvDelimiter,
      String listDelimiter,
      String vocabName,
      String vocabLabelEN,
      String vocabDefinitionEN,
      Path conceptsPath,
      Path hiddenLabelsPath,
      Charset charset) {

    // list to keep the errors and then print them to a file
    List<Error> errors = new ArrayList<>();

    // update vocabulary
    VocabularyView vocab = vocabularyClient.get(vocabName);
    vocab.getVocabulary().setName(vocabName);
    if (!Strings.isNullOrEmpty(vocabDefinitionEN)) {
      vocab.getVocabulary().getDefinition().put(LanguageRegion.ENGLISH, vocabDefinitionEN);
    }
    VocabularyView updatedVocab = vocabularyClient.update(vocab.getVocabulary());
    log.info("Updated vocabulary {} with key {}", vocabName, updatedVocab.getVocabulary().getKey());

    List<Label> labels =
        vocabularyClient.listLabels(vocab.getVocabulary().getName(), LanguageRegion.ENGLISH);
    if (!labels.isEmpty()) {
      Label englishLabel = labels.get(0);
      englishLabel.setValue(vocabLabelEN);
      vocabularyClient.updateLabel(vocabName, englishLabel);
    } else {
      vocabularyClient.addLabel(
          vocabName, Label.builder().language(LanguageRegion.ENGLISH).value(vocabLabelEN).build());
    }

    // retrieve all the existing concepts
    Map<String, Concept> existingConcepts = getVocabularyConcepts(vocabName);

    // update the concepts
    Map<String, Concept> conceptsMap = new HashMap<>();
    try (Stream<String> lines = Files.lines(conceptsPath, charset)) {
      lines
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

                if (conceptsMap.containsKey(conceptName)) {
                  errors.add(Error.of("Concept " + conceptName + " already exists", null));
                  log.error("Concept {} already exists", conceptName);
                  return;
                }

                Concept concept = existingConcepts.get(conceptName);
                conceptsMap.put(conceptName, concept);
                // reset the hidden labels as they will be set later
                int offset = 0;
                int limit = 1000;
                PagingResponse<HiddenLabel> responseHiddenLabels;
                do {
                  responseHiddenLabels =
                      conceptClient.listHiddenLabels(
                          vocabName, conceptName, new PagingRequest(offset, limit));
                  offset += limit;

                  responseHiddenLabels
                      .getResults()
                      .forEach(
                          hl ->
                              conceptClient.deleteHiddenLabel(vocabName, conceptName, hl.getKey()));
                } while (!responseHiddenLabels.isEndOfRecords());

                parseConceptFields(listDelimiter, errors, conceptsMap, values, concept, vocabName);

                // update concept
                try {
                  ConceptView updated = conceptClient.update(vocabName, concept);
                  conceptsMap.put(updated.getConcept().getName(), updated.getConcept());
                  log.info(
                      "Updated concept {} with key {}",
                      updated.getConcept().getName(),
                      updated.getConcept().getKey());
                } catch (Exception ex) {
                  errors.add(Error.of("Error updating concept " + concept.getName(), ex));
                  log.error("Cannot update concept {}", concept.getName(), ex);
                }
              });
    }

    // update hidden labels
    parseHiddenLabels(csvDelimiter, vocabName, hiddenLabelsPath, errors, conceptsMap, charset);

    printErrorsToFile(errors);
  }

  private void parseHiddenLabels(
      String csvDelimiter,
      String vocabName,
      Path hiddenLabelsPath,
      List<Error> errors,
      Map<String, Concept> conceptsMap,
      Charset charset)
      throws IOException {
    if (hiddenLabelsPath == null) {
      return;
    }

    try (Stream<String> lines = Files.lines(hiddenLabelsPath, charset)) {
      lines
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

                try {
                  conceptClient.addHiddenLabel(
                      vocabName, conceptName, HiddenLabel.builder().value(hiddenLabel).build());
                } catch (Exception ex) {
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
    }
  }

  private void parseConceptFields(
      String listDelimiter,
      List<Error> errors,
      Map<String, Concept> conceptsMap,
      String[] values,
      Concept concept,
      String vocabName) {
    // parent
    if (values.length > 1 && !Strings.isNullOrEmpty(values[1])) {
      String parentName = values[1].trim();
      Concept parent = conceptsMap.get(parentName);
      if (parent != null) {
        concept.setParentKey(parent.getKey());
      } else {
        errors.add(
            Error.of("Parent " + parentName + " not found for concept " + concept.getName(), null));
        log.error("Parent {} not found for concept {}", parentName, concept.getName());
      }
    }

    // add EN labels
    if (values.length > 2 && !Strings.isNullOrEmpty(values[2])) {
      addLabel(
          vocabName,
          concept,
          Label.builder().language(LanguageRegion.ENGLISH).value(values[2].trim()).build(),
          errors);
    }

    // add EN alternative labels
    if (values.length > 3 && !Strings.isNullOrEmpty(values[3])) {
      Stream.of(values[3].split(listDelimiter))
          .map(String::trim)
          .forEach(
              l ->
                  addAlternativeLabel(
                      vocabName,
                      concept,
                      Label.builder().language(LanguageRegion.ENGLISH).value(l).build(),
                      errors));
    }

    // add ES labels
    if (values.length > 4 && !Strings.isNullOrEmpty(values[4])) {
      addLabel(
          vocabName,
          concept,
          Label.builder().language(LanguageRegion.SPANISH).value(values[4].trim()).build(),
          errors);
    }

    // add ES alternative labels
    if (values.length > 5 && !Strings.isNullOrEmpty(values[5])) {
      Stream.of(values[5].split(listDelimiter))
          .map(String::trim)
          .forEach(
              l ->
                  addAlternativeLabel(
                      vocabName,
                      concept,
                      Label.builder().language(LanguageRegion.SPANISH).value(l).build(),
                      errors));
    }

    // add EN definitions
    if (values.length > 6 && !Strings.isNullOrEmpty(values[6])) {
      concept.getDefinition().put(LanguageRegion.ENGLISH, values[6].trim());
    }

    // add sameAs URIs
    if (values.length > 7 && !Strings.isNullOrEmpty(values[7])) {
      Set<URI> sameAsUris = new HashSet<>();
      String[] urisValues = values[7].split(listDelimiter);
      for (String uri : urisValues) {
        try {
          sameAsUris.add(URI.create(uri.trim()));
        } catch (Exception ex) {
          errors.add(Error.of("Incorrect URI " + uri, ex));
          log.error("Incorrect URI {}", uri, ex);
        }
      }
      concept.setSameAsUris(new ArrayList<>(sameAsUris));
    }

    // add external definitions
    if (values.length > 8 && !Strings.isNullOrEmpty(values[8])) {
      Set<URI> externalDefinitions = new HashSet<>();
      String[] externalDefsValues = values[8].split(listDelimiter);
      for (String definition : externalDefsValues) {
        try {
          externalDefinitions.add(URI.create(definition.trim()));
        } catch (Exception ex) {
          errors.add(Error.of("Incorrect external definition " + definition, ex));
          log.error("Incorrect external definition {}", definition, ex);
        }
      }
      concept.setExternalDefinitions(new ArrayList<>(externalDefinitions));
    }
  }

  private void addLabel(String vocabName, Concept concept, Label label, List<Error> errors) {
    try {
      conceptClient.addLabel(vocabName, concept.getName(), label);
    } catch (Exception ex) {
      errors.add(
          Error.of(
              "Error adding label "
                  + label.getLanguage()
                  + "->"
                  + label.getValue()
                  + " in concept "
                  + concept.getName(),
              ex));
      log.error("Couldn't add label {} in concept {}", label, concept.getName(), ex);
    }
  }

  private void addAlternativeLabel(
      String vocabName, Concept concept, Label label, List<Error> errors) {
    try {
      conceptClient.addAlternativeLabel(vocabName, concept.getName(), label);
    } catch (Exception ex) {
      errors.add(
          Error.of(
              "Error adding alternative label "
                  + label.getLanguage()
                  + "->"
                  + label.getValue()
                  + " in concept "
                  + concept.getName(),
              ex));
      log.error("Couldn't add alternative label {} in concept {}", label, concept.getName(), ex);
    }
  }

  private Map<String, Concept> getVocabularyConcepts(String vocabularyName) {
    int offset = 0;
    int limit = 100;

    boolean endOfRecords = false;
    List<ConceptView> conceptViews = new ArrayList<>();
    while (!endOfRecords) {
      ConceptListParams params = ConceptListParams.builder().offset(offset).limit(limit).build();
      PagingResponse<ConceptView> response = conceptClient.listConcepts(vocabularyName, params);
      endOfRecords = response.isEndOfRecords();
      conceptViews.addAll(response.getResults());
      offset += limit;
    }

    return conceptViews.stream()
        .map(ConceptView::getConcept)
        .collect(Collectors.toMap(Concept::getName, c -> c));
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
