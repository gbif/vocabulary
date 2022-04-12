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

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.client.VocabularyClient;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Vocabulary;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
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

              if (conceptsMap.containsKey(conceptName)) {
                errors.add(Error.of("Concept " + conceptName + " already exists", null));
                log.error("Concept {} already exists", conceptName);
                return;
              }

              Concept concept = new Concept();
              concept.setName(conceptName);
              conceptsMap.put(conceptName, concept);

              // read fields
              parseConceptFields(listDelimiter, errors, conceptsMap, values, concept);

              // create concept
              try {
                Concept created = conceptClient.create(createdVocab.getName(), concept);
                conceptsMap.put(created.getName(), created);
                log.info("Created concept {} with key {}", created.getName(), created.getKey());
              } catch (Exception ex) {
                errors.add(Error.of("Error creating concept " + concept.getName(), ex));
                log.error("Cannot create concept {}", concept.getName(), ex);
              }
            });

    // add hidden labels
    parseHiddenLabels(csvDelimiter, vocabName, hiddenLabelsPath, errors, conceptsMap);

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
      Path hiddenLabelsPath) {

    // list to keep the errors and then print them to a file
    List<Error> errors = new ArrayList<>();

    // update vocabulary
    Vocabulary vocab = vocabularyClient.get(vocabName);
    vocab.setName(vocabName);
    vocab.getLabel().put(LanguageRegion.ENGLISH, vocabLabelEN);
    if (!Strings.isNullOrEmpty(vocabDefinitionEN)) {
      vocab.getDefinition().put(LanguageRegion.ENGLISH, vocabDefinitionEN);
    }
    Vocabulary updatedVocab = vocabularyClient.update(vocab);
    log.info("Updated vocabulary {} with key {}", vocabName, updatedVocab.getKey());

    // retrieve all the existing concepts
    Map<String, Concept> existingConcepts = getVocabularyConcepts(vocabName);

    // update the concepts
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

              if (conceptsMap.containsKey(conceptName)) {
                errors.add(Error.of("Concept " + conceptName + " already exists", null));
                log.error("Concept {} already exists", conceptName);
                return;
              }

              Concept concept = existingConcepts.get(conceptName);
              conceptsMap.put(conceptName, concept);
              // reset the hidden labels as they will be set later
              concept.setHiddenLabels(new HashSet<>());
              parseConceptFields(listDelimiter, errors, conceptsMap, values, concept);

              // update concept
              try {
                Concept updated = conceptClient.update(vocabName, concept);
                conceptsMap.put(updated.getName(), updated);
                log.info("Updated concept {} with key {}", updated.getName(), updated.getKey());
              } catch (Exception ex) {
                errors.add(Error.of("Error updating concept " + concept.getName(), ex));
                log.error("Cannot update concept {}", concept.getName(), ex);
              }
            });

    // update hidden labels
    parseHiddenLabels(csvDelimiter, vocabName, hiddenLabelsPath, errors, conceptsMap);

    printErrorsToFile(errors);
  }

  private void parseHiddenLabels(
      String csvDelimiter,
      String vocabName,
      Path hiddenLabelsPath,
      List<Error> errors,
      Map<String, Concept> conceptsMap)
      throws IOException {
    if (hiddenLabelsPath == null) {
      return;
    }

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

              if (!concept.getHiddenLabels().contains(hiddenLabel)) {
                concept.getHiddenLabels().add(hiddenLabel);
                try {
                  Concept updated = conceptClient.update(vocabName, concept.getName(), concept);
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
              }
            });
  }

  private void parseConceptFields(
      String listDelimiter,
      List<Error> errors,
      Map<String, Concept> conceptsMap,
      String[] values,
      Concept concept) {
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
      concept.getLabel().put(LanguageRegion.ENGLISH, values[2].trim());
    }

    // add EN alternative labels
    if (values.length > 3 && !Strings.isNullOrEmpty(values[3])) {
      Set<String> altLabels =
          Stream.of(values[3].split(listDelimiter)).map(String::trim).collect(Collectors.toSet());
      concept.getAlternativeLabels().put(LanguageRegion.ENGLISH, altLabels);
    }

    // add ES labels
    if (values.length > 4 && !Strings.isNullOrEmpty(values[4])) {
      concept.getLabel().put(LanguageRegion.SPANISH, values[4].trim());
    }

    // add ES alternative labels
    if (values.length > 5 && !Strings.isNullOrEmpty(values[5])) {
      Set<String> altLabels =
          Stream.of(values[5].split(listDelimiter)).map(String::trim).collect(Collectors.toSet());

      concept.getAlternativeLabels().put(LanguageRegion.SPANISH, altLabels);
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
