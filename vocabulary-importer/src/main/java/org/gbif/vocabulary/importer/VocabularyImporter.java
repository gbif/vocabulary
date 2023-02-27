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
import org.gbif.vocabulary.model.Definition;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Vocabulary;

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
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
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
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vocabLabelEN));
    Objects.requireNonNull(conceptsPath);

    // list to keep the errors and then print them to a file
    List<Error> errors = new ArrayList<>();

    // create vocabulary
    Vocabulary vocab = new Vocabulary();
    vocab.setName(vocabName);
    Vocabulary createdVocab = vocabularyClient.create(vocab);
    log.info("Created vocabulary {} with key {}", vocabName, createdVocab.getKey());

    if (!Strings.isNullOrEmpty(vocabDefinitionEN)) {
      vocabularyClient.addDefinition(
        vocabName,
        Definition.builder().language(LanguageRegion.ENGLISH).value(vocabDefinitionEN).build());
    }

    Long vocabLabelKey =
      vocabularyClient.addLabel(
        vocabName,
        Label.builder().language(LanguageRegion.ENGLISH).value(vocabLabelEN).build());
    log.info("Added vocabulary label with key {}", vocabLabelKey);

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
            ConceptData conceptData =
              parseConceptFields(listDelimiter, errors, conceptsMap, values, concept);

            // create concept
            try {
              ConceptView created =
                conceptClient.create(createdVocab.getName(), conceptData.concept);
              conceptsMap.put(created.getConcept().getName(), created.getConcept());
              log.info(
                "Created concept {} with key {}",
                created.getConcept().getName(),
                created.getConcept().getKey());
            } catch (Exception ex) {
              errors.add(Error.of("Error creating concept " + concept.getName(), ex));
              log.error("Cannot create concept {}", concept.getName(), ex);
            }

            conceptData.definitions.forEach(
              d -> addDefinition(vocabName, conceptName, d, errors));
            conceptData.labels.forEach(lab -> addLabel(vocabName, conceptName, lab, errors));
            conceptData.alternativeLabels.forEach(
              lab -> addAlternativeLabel(vocabName, conceptName, lab, errors));
          });
    }

    // add hidden labels
    parseHiddenLabels(csvDelimiter, vocabName, hiddenLabelsPath, errors, conceptsMap::get, charset);

    printErrorsToFile(errors);
  }

  @SneakyThrows
  public void importHiddenLabels(
    String csvDelimiter, String vocabName, Path hiddenLabelsPath,
    Charset charset) {

    Function<String, Concept> getConceptFn = conceptName -> {
      ConceptView conceptView = conceptClient
        .get(vocabName, conceptName, false, false);
      return conceptView != null ? conceptView.getConcept() : null;
    };

    // list to keep the errors and then print them to a file
    List<Error> errors = new ArrayList<>();
    parseHiddenLabels(csvDelimiter, vocabName, hiddenLabelsPath, errors, getConceptFn, charset);
    printErrorsToFile(errors);
  }

  private void parseHiddenLabels(
    String csvDelimiter,
    String vocabName,
    Path hiddenLabelsPath,
    List<Error> errors,
    Function<String, Concept> getConceptFn,
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
            Concept concept = getConceptFn.apply(conceptName);

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

  private ConceptData parseConceptFields(
    String listDelimiter,
    List<Error> errors,
    Map<String, Concept> conceptsMap,
    String[] values,
    Concept concept) {

    List<Label> labels = new ArrayList<>();
    List<Label> alternativeLabels = new ArrayList<>();
    List<Definition> definitions = new ArrayList<>();

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
      labels.add(Label.builder().language(LanguageRegion.ENGLISH).value(values[2].trim()).build());
    }

    // add EN alternative labels
    if (values.length > 3 && !Strings.isNullOrEmpty(values[3])) {
      Stream.of(values[3].split(listDelimiter))
        .map(String::trim)
        .forEach(
          l ->
            alternativeLabels.add(
              Label.builder().language(LanguageRegion.ENGLISH).value(l).build()));
    }

    // add ES labels
    if (values.length > 4 && !Strings.isNullOrEmpty(values[4])) {
      labels.add(Label.builder().language(LanguageRegion.SPANISH).value(values[4].trim()).build());
    }

    // add ES alternative labels
    if (values.length > 5 && !Strings.isNullOrEmpty(values[5])) {
      Stream.of(values[5].split(listDelimiter))
        .map(String::trim)
        .forEach(
          l ->
            alternativeLabels.add(
              Label.builder().language(LanguageRegion.SPANISH).value(l).build()));
    }

    // add EN definitions
    if (values.length > 6 && !Strings.isNullOrEmpty(values[6])) {
      definitions.add(
        Definition.builder().language(LanguageRegion.ENGLISH).value(values[6].trim()).build());
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

    return ConceptData.of(concept, definitions, labels, alternativeLabels, null);
  }

  private void addDefinition(
    String vocabName, String conceptName, Definition definition, List<Error> errors) {
    try {
      conceptClient.addDefinition(vocabName, conceptName, definition);
    } catch (Exception ex) {
      errors.add(
        Error.of(
          "Error adding definition "
            + definition.getLanguage()
            + "->"
            + definition.getValue()
            + " in concept "
            + conceptName,
          ex));
      log.error("Couldn't add definition {} in concept {}", definition, conceptName, ex);
    }
  }

  private void addLabel(String vocabName, String conceptName, Label label, List<Error> errors) {
    try {
      conceptClient.addLabel(vocabName, conceptName, label);
    } catch (Exception ex) {
      errors.add(
        Error.of(
          "Error adding label "
            + label.getLanguage()
            + "->"
            + label.getValue()
            + " in concept "
            + conceptName,
          ex));
      log.error("Couldn't add label {} in concept {}", label, conceptName, ex);
    }
  }

  private void addAlternativeLabel(
    String vocabName, String conceptName, Label label, List<Error> errors) {
    try {
      conceptClient.addAlternativeLabel(vocabName, conceptName, label);
    } catch (Exception ex) {
      errors.add(
        Error.of(
          "Error adding alternative label "
            + label.getLanguage()
            + "->"
            + label.getValue()
            + " in concept "
            + conceptName,
          ex));
      log.error("Couldn't add alternative label {} in concept {}", label, conceptName, ex);
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

  @AllArgsConstructor(staticName = "of")
  private static class ConceptData {
    private Concept concept;
    private List<Definition> definitions;
    private List<Label> labels;
    private List<Label> alternativeLabels;
    private List<HiddenLabel> hiddenLabels;
  }
}
