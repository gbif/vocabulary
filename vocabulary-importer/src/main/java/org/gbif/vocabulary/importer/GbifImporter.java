package org.gbif.vocabulary.importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.gbif.vocabulary.importer.http.VocabularyClient;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.enums.LanguageRegion;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GbifImporter {

  private static final Map<String, LanguageRegion> PORTAL_LANGS =
      new TreeMap<>(
          (o1, o2) -> {
            // we're not expecting null values
            if ("en".equals(o1)) {
              return -1;
            }
            if ("en".equals(o2)) {
              return 1;
            }
            return o1.compareTo(o2);
          });

  private static final String VOCABS_TRANSLATIONS_URL =
      "https://raw.githubusercontent.com/gbif/portal16/master/locales/translations/%s/components/filterNames.json";
  private static final String CONCEPT_TRANSLATIONS_URL =
      "https://raw.githubusercontent.com/gbif/portal16/master/locales/translations/%s/enums/%s.json";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    PORTAL_LANGS.put("en", LanguageRegion.ENGLISH);
    PORTAL_LANGS.put("fr", LanguageRegion.FRENCH);
    PORTAL_LANGS.put("ja", LanguageRegion.JAPANESE);
    PORTAL_LANGS.put("pt", LanguageRegion.PORTUGUESE);
    PORTAL_LANGS.put("ru", LanguageRegion.RUSSIAN);
    PORTAL_LANGS.put("es", LanguageRegion.SPANISH);
    PORTAL_LANGS.put("zh", LanguageRegion.CHINESE_SIMPLIFIED);

    // TODO: use all the langs from the portal??
  }

  public static void main(String[] args) throws IOException {
    //    VocabularyClient client =
    //        VocabularyClient.create("http://localhost:8080/", "", "");

    VocabularyClient client =
        VocabularyClient.create("https://api.gbif-dev.org/v1/", "", "");

    // create vocabulary
    final Path vocabPath = Paths.get("/Users/rgz522/dev/vocabs/lifestage-vocabulary-export.csv");
    Vocabulary vocab = new Vocabulary();
    Files.lines(vocabPath)
        .skip(1)
        .limit(1) // we skip the column names
        .filter(l -> !Strings.isNullOrEmpty(l))
        .forEach(
            l -> {
              String[] values = l.split(";");

              vocab.setName(values[0]);
              vocab.getLabel().put(LanguageRegion.ENGLISH, values[1]);
              vocab.getDefinition().put(LanguageRegion.ENGLISH, values[2]);
            });

    // create vocabulary in ws
    Vocabulary vocabulary = client.createVocabulary(vocab);

    final Path conceptsPath = Paths.get("/Users/rgz522/dev/vocabs/lifestage-concepts-export.csv");
    Map<String, Concept> conceptsMap = new HashMap<>();
    // we first create the concepts
    Files.lines(conceptsPath)
        .skip(1) // we skip the column names
        .filter(l -> !Strings.isNullOrEmpty(l))
        .forEach(
            l -> {
              String[] values = l.split(";");
              String conceptName = values[0].trim();

              if (conceptsMap.containsKey(conceptName)) {
                throw new IllegalArgumentException("Concept duplicated: " + conceptName);
              }

              Concept concept = new Concept();
              concept.setName(conceptName);
              conceptsMap.put(conceptName, concept);

              // parent
              if (!Strings.isNullOrEmpty(values[1])) {
                Concept parent = conceptsMap.get(values[1].trim());
                concept.setParentKey(parent.getKey());
              }

              // add EN labels
              if (!Strings.isNullOrEmpty(values[2])) {
                concept.getLabel().put(LanguageRegion.ENGLISH, values[2].trim());
              }

              // add EN alternative labels
              if (!Strings.isNullOrEmpty(values[3])) {
                concept
                    .getAlternativeLabels()
                    .computeIfAbsent(LanguageRegion.ENGLISH, k -> new ArrayList<>())
                    .add(values[3].trim());
              }

              // add ES labels
              if (!Strings.isNullOrEmpty(values[4])) {
                concept.getLabel().put(LanguageRegion.SPANISH, values[4].trim());
              }

              // add ES alternative labels
              if (!Strings.isNullOrEmpty(values[5])) {
                concept
                    .getAlternativeLabels()
                    .computeIfAbsent(LanguageRegion.SPANISH, k -> new ArrayList<>())
                    .add(values[5].trim());
              }

              // add EN definitions
              if (!Strings.isNullOrEmpty(values[6])) {
                concept.getDefinition().put(LanguageRegion.ENGLISH, values[6].trim());
              }

              Concept created = client.createConcept(vocabulary.getName(), concept);
              conceptsMap.put(created.getName(), created);
            });

    // add hidden labels
    final Path misappliedsPath =
        Paths.get("/Users/rgz522/dev/vocabs/lifestage-misapplied-export.csv");
    Files.lines(misappliedsPath)
        .skip(1) // we skip the column names
        .filter(l -> !Strings.isNullOrEmpty(l))
        .forEach(
            l -> {
              String[] values = l.split(";");
              String hiddenLabel = values[1].trim();
              //              log.info("Hidden label: {}", hiddenLabel);
              Concept concept = conceptsMap.get(values[0].trim());
              concept.getHiddenLabels().add(hiddenLabel);
              try {
                Concept updated =
                    client.updateConcept(vocabulary.getName(), concept.getName(), concept);
                conceptsMap.put(concept.getName(), updated);
              } catch (Exception ex) {
                concept.getHiddenLabels().remove(hiddenLabel);
                log.error(
                    "Couldn't add label: {} in concept: {}", hiddenLabel, concept.getName(), ex);
              }
            });
  }

  private static String toPascalCase(String s) {
    if (Strings.isNullOrEmpty(s)) {
      return s;
    }

    StringBuilder pascalCase = new StringBuilder();

    boolean afterSpace = true;
    for (char ch : s.toCharArray()) {
      if (Character.isSpaceChar(ch) || ch == '-') {
        afterSpace = true;
        continue;
      }

      if (afterSpace) {
        pascalCase.append(Character.toUpperCase(ch));
        afterSpace = false;
      } else {
        pascalCase.append(Character.toLowerCase(ch));
      }
    }

    return pascalCase.toString();
  }

  private static String toTitleCase(String s) {
    return StringUtils.capitalize(s.toLowerCase());
  }
}
