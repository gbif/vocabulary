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
package org.gbif.vocabulary.importer.geotime;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.AddTagAction;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.client.TagClient;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Definition;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Tag;
import org.gbif.ws.client.ClientBuilder;
import org.slf4j.helpers.MessageFormatter;

@Slf4j
public class GeoTimeImporter {

  static final String ICS_CHART_SOURCE =
      "https://github.com/i-c-stratigraphy/chart/blob/main/chart.ttl";
  static final String DEFAULT_GEOTIME_API_URL = "https://api.gbif-dev.org/v1/";
  static final String GEOTIME_VOCABULARY_NAME = "GeoTime";
  static final long GEOTIME_VOCABULARY_KEY = 165;
  private static final int PAGE_SIZE = 100;
  private static final Path WARNINGS_REPORT_PATH = Path.of("geotime-changes.log");
  private static final Path CLIENT_ERRORS_REPORT_PATH = Path.of("geotime-client-errors.log");

  public static void main(String[] args) {
    String apiUrl = args.length > 0 ? args[0] : DEFAULT_GEOTIME_API_URL;
    String apiUsername = args.length > 1 ? args[1] : null;
    String apiPassword = args.length > 2 ? args[2] : null;

    if ((apiUsername == null) != (apiPassword == null)) {
      throw new IllegalArgumentException(
          "If credentials are provided, both username and password must be passed as args");
    }

    initializeReports();
    ObjectMapper mapper = createMapper();
    ClientBuilder clientBuilder =
        new ClientBuilder()
            .withUrl(apiUrl)
            .withExponentialBackoffRetry(Duration.ofMillis(1000), 2, 4)
            .withObjectMapper(mapper);
    if (apiUsername != null
        && !apiUsername.isBlank()
        && apiPassword != null
        && !apiPassword.isBlank()) {
      clientBuilder.withCredentials(apiUsername, apiPassword);
    }
    ConceptClient conceptClient = clientBuilder.build(ConceptClient.class);
    TagClient tagClient = clientBuilder.build(TagClient.class);

    // Load all GeoTime concepts from the API with pagination into memory at startup.
    Map<String, Concept> geoTimeConceptsByName = loadGeoTimeConcepts(conceptClient);
    log.info("Loaded {} GeoTime API concepts in memory", geoTimeConceptsByName.size());
    log.info("Writing validation warnings to {}", WARNINGS_REPORT_PATH.toAbsolutePath());
    log.info("Writing client call errors to {}", CLIENT_ERRORS_REPORT_PATH.toAbsolutePath());

    SkosTraversalService traversalService = new SkosTraversalService();
    List<SkosElement> elements = traversalService.readElements(ICS_CHART_SOURCE);

    traversalService.walkConcepts(
        elements,
        (depth, element) -> {
          String conceptName = element.getConceptName();
          if (conceptName != null && !conceptName.isBlank()) {
            if (!geoTimeConceptsByName.containsKey(conceptName)) {
              warnAndPersist("Concept '{}' is missing in GeoTime API", conceptName);
              Concept createdConcept =
                  createConcept(conceptName, element, conceptClient, tagClient);
              if (createdConcept != null) {
                geoTimeConceptsByName.put(conceptName, createdConcept);
              }
            } else {
              Concept concept = geoTimeConceptsByName.get(conceptName);
              if (concept != null) {
                // add uri
                Set<URI> uniqueURIs = new HashSet<>(concept.getSameAsUris());
                URI elementUri = URI.create(element.getUri());
                if (!uniqueURIs.contains(elementUri)) {
                  concept.getSameAsUris().add(elementUri);
                  try {
                    conceptClient.update(GEOTIME_VOCABULARY_NAME, concept);
                  } catch (Exception ex) {
                    errorAndPersistClientCall(
                        "Unable to update concept '{}': {}", conceptName, ex.getMessage());
                  }
                }

                syncConceptTags(conceptName, element, concept, conceptClient, tagClient);
                syncConceptDefinitions(conceptName, element, concept, conceptClient);
                syncConceptLabels(conceptName, element, concept, conceptClient);
              }
            }
          }
        });
  }

  private static Concept createConcept(
      String conceptName, SkosElement element, ConceptClient conceptClient, TagClient tagClient) {
    try {
      Concept concept = new Concept();
      concept.setName(conceptName);
      concept.setSameAsUris(List.of(URI.create(element.getUri())));
      concept.setVocabularyKey(GEOTIME_VOCABULARY_KEY);

      ConceptView created = conceptClient.create(GEOTIME_VOCABULARY_NAME, concept);
      addConceptDefinitions(conceptName, element, conceptClient);
      addConceptLabels(conceptName, element, conceptClient);

      String rankTag = "rank: " + element.getRank();
      addTag(conceptName, Tag.of(rankTag), conceptClient, tagClient);
      String startAgeTag = "startAge: " + element.getHasBeginning().inMYA();
      addTag(conceptName, Tag.of(startAgeTag), conceptClient, tagClient);
      String endAgeTag = "endAge: " + element.getHasEnd().inMYA();
      addTag(conceptName, Tag.of(endAgeTag), conceptClient, tagClient);

      return created != null ? created.getConcept() : null;
    } catch (Exception ex) {
      errorAndPersistClientCall(
          "Unable to create concept '{}' in GeoTime API: {}", conceptName, ex.getMessage());
      return null;
    }
  }

  private static void removeTag(
      String conceptName, Tag tag, ConceptClient conceptClient, TagClient tagClient) {
    conceptClient.removeTag(GeoTimeImporter.GEOTIME_VOCABULARY_NAME, conceptName, tag.getName());

    PagingResponse<Tag> unusedTag =
        tagClient.listTags(null, tag.getName(), false, new PagingRequest());
    unusedTag.getResults().forEach(r -> tagClient.delete(r.getName()));
  }

  private static void addTag(
      String conceptName, Tag tag, ConceptClient conceptClient, TagClient tagClient) {
    try {
      Tag existingTag = tagClient.getTag(tag.getName());
      if (existingTag == null) {
        tag.setKey(null);
        existingTag = tagClient.create(tag);
      }
      conceptClient.addTag(
          GeoTimeImporter.GEOTIME_VOCABULARY_NAME,
          conceptName,
          new AddTagAction(existingTag.getName()));
    } catch (Exception ex) {
      errorAndPersistClientCall(
          "Couldn't add tag {} to concept {}: {}", tag.getName(), conceptName, ex.getMessage());
    }
  }

  private static void addConceptDefinitions(
      String conceptName, SkosElement element, ConceptClient conceptClient) {
    element
        .getDefinitions()
        .forEach(
            (lang, def) -> {
              LanguageRegion language = resolveLanguageRegion(lang);
              if (language == LanguageRegion.UNKNOWN) {
                warnAndPersist(
                    "Concept '{}' has definition with unsupported language '{}': unable to map to LanguageRegion",
                    conceptName,
                    lang);
              }

              try {
                conceptClient.addDefinition(
                    GEOTIME_VOCABULARY_NAME,
                    conceptName,
                    Definition.builder().language(language).value(def).build());
              } catch (Exception ex) {
                errorAndPersistClientCall(
                    "Unable to create definition for concept '{}' and language '{}': {}",
                    conceptName,
                    language.getLocale(),
                    ex.getMessage());
              }
            });
  }

  private static void addConceptLabels(
      String conceptName, SkosElement element, ConceptClient conceptClient) {
    element
        .getPrefLabels()
        .forEach(
            (lang, label) -> {
              LanguageRegion language = resolveLanguageRegion(lang);
              if (language == LanguageRegion.UNKNOWN) {
                warnAndPersist(
                    "Concept '{}' has label with unsupported language '{}': unable to map to LanguageRegion",
                    conceptName,
                    lang);
              }

              try {
                conceptClient.addLabel(
                    GEOTIME_VOCABULARY_NAME,
                    conceptName,
                    Label.builder().language(language).value(label).build());
              } catch (Exception ex) {
                errorAndPersistClientCall(
                    "Unable to create label for concept '{}' and language '{}': {}",
                    conceptName,
                    language.getLocale(),
                    ex.getMessage());
              }
            });
  }

  private static void syncConceptDefinitions(
      String conceptName, SkosElement element, Concept concept, ConceptClient conceptClient) {
    Map<LanguageRegion, String> skosDefinitionsByLanguage =
        mapSkosTextsByLanguageRegion(conceptName, "definition", element.getDefinitions());
    Map<LanguageRegion, Definition> conceptDefinitionsByLanguage =
        mapConceptDefinitionsByLanguageRegion(concept);

    for (Map.Entry<LanguageRegion, String> skosEntry : skosDefinitionsByLanguage.entrySet()) {
      LanguageRegion language = skosEntry.getKey();
      String skosValue = skosEntry.getValue();
      Definition conceptDefinition = conceptDefinitionsByLanguage.get(language);

      if (conceptDefinition == null) {
        warnAndPersist(
            "Concept '{}' is missing definition for language '{}': SkosElement={}",
            conceptName,
            language.getLocale(),
            skosValue);
        try {
          conceptClient.addDefinition(
              GEOTIME_VOCABULARY_NAME,
              conceptName,
              Definition.builder().language(skosEntry.getKey()).value(skosValue).build());
        } catch (Exception ex) {
          errorAndPersistClientCall(
              "Unable to add missing definition for concept '{}' and language '{}': {}",
              conceptName,
              language.getLocale(),
              ex.getMessage());
        }
        continue;
      }

      if (!Objects.equals(normalizeText(skosValue), normalizeText(conceptDefinition.getValue()))) {
        warnAndPersist(
            "Concept '{}' has mismatched definition for language '{}': SkosElement={}, Concept={}",
            conceptName,
            language.getLocale(),
            skosValue,
            conceptDefinition.getValue());

        try {
          conceptClient.deleteDefinition(
              GEOTIME_VOCABULARY_NAME, conceptName, conceptDefinition.getKey());
        } catch (Exception ex) {
          errorAndPersistClientCall(
              "Unable to delete outdated definition for concept '{}' and language '{}': {}",
              conceptName,
              language.getLocale(),
              ex.getMessage());
        }

        try {
          conceptClient.addDefinition(
              GEOTIME_VOCABULARY_NAME,
              conceptName,
              Definition.builder().language(skosEntry.getKey()).value(skosValue).build());
        } catch (Exception ex) {
          errorAndPersistClientCall(
              "Unable to add updated definition for concept '{}' and language '{}': {}",
              conceptName,
              language.getLocale(),
              ex.getMessage());
        }
      }
    }
  }

  private static void syncConceptLabels(
      String conceptName, SkosElement element, Concept concept, ConceptClient conceptClient) {
    Map<LanguageRegion, String> skosLabelsByLanguage =
        mapSkosTextsByLanguageRegion(conceptName, "label", element.getPrefLabels());
    Map<LanguageRegion, Label> conceptLabelsByLanguage = mapConceptLabelsByLanguageRegion(concept);

    for (Map.Entry<LanguageRegion, String> skosEntry : skosLabelsByLanguage.entrySet()) {
      LanguageRegion language = skosEntry.getKey();
      String skosValue = skosEntry.getValue();
      Label conceptLabel = conceptLabelsByLanguage.get(language);

      if (conceptLabel == null) {
        warnAndPersist(
            "Concept '{}' is missing label for language '{}': SkosElement={}",
            conceptName,
            language.getLocale(),
            skosValue);
        try {
          conceptClient.addLabel(
              GEOTIME_VOCABULARY_NAME,
              conceptName,
              Label.builder().language(skosEntry.getKey()).value(skosValue).build());
        } catch (Exception ex) {
          errorAndPersistClientCall(
              "Unable to add missing label for concept '{}' and language '{}': {}",
              conceptName,
              language.getLocale(),
              ex.getMessage());
        }
        continue;
      }

      if (!Objects.equals(normalizeText(skosValue), normalizeText(conceptLabel.getValue()))) {
        warnAndPersist(
            "Concept '{}' has mismatched label for language '{}': SkosElement={}, Concept={}",
            conceptName,
            language.getLocale(),
            skosValue,
            conceptLabel.getValue());

        try {
          conceptClient.deleteLabel(GEOTIME_VOCABULARY_NAME, conceptName, conceptLabel.getKey());
        } catch (Exception ex) {
          errorAndPersistClientCall(
              "Unable to delete outdated label for concept '{}' and language '{}': {}",
              conceptName,
              language.getLocale(),
              ex.getMessage());
        }

        try {
          conceptClient.addLabel(
              GEOTIME_VOCABULARY_NAME,
              conceptName,
              Label.builder().language(skosEntry.getKey()).value(skosValue).build());
        } catch (Exception ex) {
          errorAndPersistClientCall(
              "Unable to add updated label for concept '{}' and language '{}': {}",
              conceptName,
              language.getLocale(),
              ex.getMessage());
        }
      }
    }
  }

  private static Map<LanguageRegion, String> mapSkosTextsByLanguageRegion(
      String conceptName, String valueType, Map<String, String> valuesByLanguage) {
    Map<LanguageRegion, String> result = new LinkedHashMap<>();
    Set<String> unknownLanguages = new LinkedHashSet<>();

    for (Map.Entry<String, String> entry : valuesByLanguage.entrySet()) {
      String rawLanguage = entry.getKey();
      String value = entry.getValue();
      if (value == null || value.isBlank()) {
        continue;
      }

      LanguageRegion language = resolveLanguageRegion(rawLanguage);
      if (language == LanguageRegion.UNKNOWN) {
        unknownLanguages.add(rawLanguage == null ? "<null>" : rawLanguage);
        continue;
      }

      result.put(language, value);
    }

    for (String unknownLanguage : unknownLanguages) {
      warnAndPersist(
          "Concept '{}' has {} with unsupported language '{}': unable to map to LanguageRegion",
          conceptName,
          valueType,
          unknownLanguage);
    }
    return result;
  }

  private static Map<LanguageRegion, Label> mapConceptLabelsByLanguageRegion(Concept concept) {
    Map<LanguageRegion, Label> result = new LinkedHashMap<>();
    if (concept.getLabel() == null) {
      return result;
    }

    for (Label label : concept.getLabel()) {
      if (label.getLanguage() == null || label.getValue() == null || label.getValue().isBlank()) {
        continue;
      }
      result.put(label.getLanguage(), label);
    }
    return result;
  }

  private static Map<LanguageRegion, Definition> mapConceptDefinitionsByLanguageRegion(
      Concept concept) {
    Map<LanguageRegion, Definition> result = new LinkedHashMap<>();
    if (concept.getDefinition() == null) {
      return result;
    }

    for (Definition definition : concept.getDefinition()) {
      if (definition.getLanguage() == null
          || definition.getValue() == null
          || definition.getValue().isBlank()) {
        continue;
      }
      result.put(definition.getLanguage(), definition);
    }
    return result;
  }

  private static LanguageRegion resolveLanguageRegion(String rawLanguage) {
    if (rawLanguage == null || rawLanguage.isBlank()) {
      return LanguageRegion.UNKNOWN;
    }

    String original = rawLanguage.trim();
    LanguageRegion iso2Match = null;
    int iso2Matches = 0;
    for (LanguageRegion languageRegion : LanguageRegion.values()) {
      String iso2 = languageRegion.getIso2LetterCode();
      if (iso2 != null && !iso2.isBlank() && iso2.equalsIgnoreCase(original)) {
        iso2Match = languageRegion;
        iso2Matches++;
      }
    }
    if (iso2Matches == 1) {
      return iso2Match;
    }

    LanguageRegion directMatch = LanguageRegion.fromLocale(original);
    if (directMatch != LanguageRegion.UNKNOWN) {
      return directMatch;
    }

    // Business rule: treat bare zh as Chinese Simplified.
    if ("zh".equalsIgnoreCase(original)) {
      return LanguageRegion.CHINESE_SIMPLIFIED;
    }

    String normalized = original.replace('_', '-');
    if (!normalized.contains("-")) {
      String localeFallback = normalized.toLowerCase() + "-" + normalized.toLowerCase();
      LanguageRegion fallbackMatch = LanguageRegion.fromLocale(localeFallback);
      if (fallbackMatch != LanguageRegion.UNKNOWN) {
        return fallbackMatch;
      }
    }

    return LanguageRegion.UNKNOWN;
  }

  private static String normalizeText(String value) {
    if (value == null) {
      return null;
    }
    return value.trim().replaceAll("\\s+", " ");
  }

  private static void syncConceptTags(
      String conceptName,
      SkosElement element,
      Concept concept,
      ConceptClient conceptClient,
      TagClient tagClient) {
    if (concept.getTags() == null || concept.getTags().isEmpty()) {
      return;
    }

    // Validate rank
    if (element.getRank() != null) {
      String rankValue = extractTagValueFromName(concept, "rank");
      if (!Objects.equals(element.getRank(), rankValue)) {
        warnAndPersist(
            "Concept '{}' has mismatched rank: SkosElement={}, Tag={}",
            conceptName,
            element.getRank(),
            rankValue != null ? rankValue : "missing");

        try {
          if (rankValue != null && !rankValue.isBlank()) {
            removeTag(conceptName, Tag.of("rank: " + rankValue), conceptClient, tagClient);
          }
        } catch (Exception ex) {
          errorAndPersistClientCall(
              "Unable to remove previous rank tag from concept '{}': {}",
              conceptName,
              ex.getMessage());
        }

        addTag(conceptName, Tag.of("rank: " + element.getRank()), conceptClient, tagClient);
      }
    }

    // Validate hasBeginning (startAge)
    if (element.getHasBeginning() != null) {
      Double expectedStartAge = element.getHasBeginning().inMYA();
      String startAgeValue = extractTagValueFromName(concept, "startAge");
      if (!matchesNumericValue(expectedStartAge, startAgeValue)) {
        warnAndPersist(
            "Concept '{}' has mismatched startAge: SkosElement={}, Tag={}",
            conceptName,
            expectedStartAge,
            startAgeValue != null ? startAgeValue : "missing");

        try {
          if (startAgeValue != null && !startAgeValue.isBlank()) {
            removeTag(conceptName, Tag.of("startAge: " + startAgeValue), conceptClient, tagClient);
          }
        } catch (Exception ex) {
          errorAndPersistClientCall(
              "Unable to remove previous startAge tag from concept '{}': {}",
              conceptName,
              ex.getMessage());
        }

        addTag(conceptName, Tag.of("startAge: " + expectedStartAge), conceptClient, tagClient);
      }
    }

    // Validate hasEnd (endAge)
    if (element.getHasEnd() != null) {
      Double expectedEndAge = element.getHasEnd().inMYA();
      String endAgeValue = extractTagValueFromName(concept, "endAge");
      if (!matchesNumericValue(expectedEndAge, endAgeValue)) {
        warnAndPersist(
            "Concept '{}' has mismatched endAge: SkosElement={}, Tag={}",
            conceptName,
            expectedEndAge,
            endAgeValue != null ? endAgeValue : "missing");

        try {
          if (endAgeValue != null && !endAgeValue.isBlank()) {
            removeTag(conceptName, Tag.of("endAge: " + endAgeValue), conceptClient, tagClient);
          }
        } catch (Exception ex) {
          errorAndPersistClientCall(
              "Unable to remove previous endAge tag from concept '{}': {}",
              conceptName,
              ex.getMessage());
        }

        addTag(conceptName, Tag.of("endAge: " + expectedEndAge), conceptClient, tagClient);
      }
    }
  }

  private static void errorAndPersistClientCall(String messageTemplate, Object... args) {
    log.error(messageTemplate, args);
    String resolvedMessage = MessageFormatter.arrayFormat(messageTemplate, args).getMessage();
    appendLineToReport(CLIENT_ERRORS_REPORT_PATH, resolvedMessage);
  }

  private static void warnAndPersist(String messageTemplate, Object... args) {
    log.warn(messageTemplate, args);
    String resolvedMessage = MessageFormatter.arrayFormat(messageTemplate, args).getMessage();
    appendLineToReport(WARNINGS_REPORT_PATH, resolvedMessage);
  }

  private static void initializeReports() {
    initializeReport(WARNINGS_REPORT_PATH);
    initializeReport(CLIENT_ERRORS_REPORT_PATH);
  }

  private static void initializeReport(Path reportPath) {
    try {
      Files.writeString(
          reportPath, "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Unable to initialize report at " + reportPath.toAbsolutePath(), e);
    }
  }

  private static synchronized void appendLineToReport(Path reportPath, String message) {
    try {
      Files.writeString(
          reportPath,
          message + System.lineSeparator(),
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Unable to write report to " + reportPath.toAbsolutePath(), e);
    }
  }

  private static String extractTagValueFromName(Concept concept, String expectedTagName) {
    for (org.gbif.vocabulary.model.Tag tag : concept.getTags()) {
      String rawTagName = tag.getName();
      if (rawTagName == null) {
        continue;
      }

      String[] parts = rawTagName.split(":", 2);
      if (parts.length != 2) {
        continue;
      }

      String tagName = parts[0].trim();
      String tagValue = parts[1].trim();
      if (expectedTagName.equals(tagName)) {
        return tagValue.isEmpty() ? null : tagValue;
      }
    }
    return null;
  }

  private static boolean matchesNumericValue(Double expectedValue, String actualValue) {
    if (expectedValue == null) {
      return actualValue == null;
    }
    if (actualValue == null || actualValue.isBlank()) {
      return false;
    }
    try {
      return Double.compare(expectedValue, Double.parseDouble(actualValue)) == 0;
    } catch (NumberFormatException e) {
      return Objects.equals(expectedValue.toString(), actualValue);
    }
  }

  private static Map<String, Concept> loadGeoTimeConcepts(ConceptClient conceptClient) {
    Map<String, Concept> conceptsByName = new LinkedHashMap<>();

    int offset = 0;
    boolean endOfRecords = false;

    while (!endOfRecords) {
      ConceptListParams params =
          ConceptListParams.builder().offset(offset).limit(PAGE_SIZE).build();
      PagingResponse<ConceptView> response =
          conceptClient.listConcepts(GEOTIME_VOCABULARY_NAME, params);
      if (response.getResults() == null || response.getResults().isEmpty()) {
        break;
      }

      for (ConceptView result : response.getResults()) {
        Concept concept = result != null ? result.getConcept() : null;
        if (concept == null || concept.getName() == null || concept.getName().isBlank()) {
          continue;
        }
        conceptsByName.put(concept.getName(), concept);
      }

      endOfRecords = response.isEndOfRecords();
      offset += PAGE_SIZE;
    }

    return conceptsByName;
  }

  private static ObjectMapper createMapper() {
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }
}
