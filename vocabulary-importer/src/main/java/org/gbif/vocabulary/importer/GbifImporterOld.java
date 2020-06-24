package org.gbif.vocabulary.importer;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.gbif.vocabulary.importer.http.VocabularyClient;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.enums.LanguageRegion;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GbifImporterOld {

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
    VocabularyClient client =
        VocabularyClient.create("http://localhost:8080/", "****", "****");

//    VocabularyClient client =
//        VocabularyClient.create("https://api.gbif-dev.org/v1/", "****", "****");

    final Path path = Paths.get("/Users/rgz522/dev/vocabs/typeStatus.csv");
    String vocab = path.getFileName().toString().replace(".csv", "");
    String vocabularyName = StringUtils.capitalize(vocab);
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(vocabularyName);

    Map<String, Concept> conceptMap = new HashMap<>();
    PORTAL_LANGS.forEach(
        (portalLang, langRegion) -> {
          final String vocabsTranslationsUrl = String.format(VOCABS_TRANSLATIONS_URL, portalLang);
          JsonNode vocabsTranslations = null;
          try {
            vocabsTranslations =
                OBJECT_MAPPER.readTree(new URL(vocabsTranslationsUrl)).path("filterNames");
          } catch (IOException e) {
            log.error("Couldn't load translations from {}", vocabsTranslations);
            return;
          }

          vocabulary.getLabel().put(langRegion, vocabsTranslations.path(vocab).asText());

          final String conceptsTranslationsUrl =
              String.format(CONCEPT_TRANSLATIONS_URL, portalLang, vocab);
          JsonNode conceptTranslations = null;
          try {
            conceptTranslations = OBJECT_MAPPER.readTree(new URL(conceptsTranslationsUrl));
          } catch (IOException e) {
            log.error("Couldn't load translations from {}", conceptsTranslationsUrl);
            return;
          }

          Iterator<Entry<String, JsonNode>> iterator =
              conceptTranslations.iterator().next().fields();
          while (iterator.hasNext()) {
            Entry<String, JsonNode> node = iterator.next();
            Concept concept = conceptMap.get(node.getKey());

            if (concept == null) {
              concept = new Concept();
              concept.setName(toPascalCase(node.getValue().asText()));
              conceptMap.put(node.getKey(), concept);
            }

            concept.getLabel().put(langRegion, node.getValue().asText());
          }
        });

    Files.lines(path)
        .skip(1) // we skip the column names
        .filter(l -> !Strings.isNullOrEmpty(l))
        .forEach(
            l -> {
              String[] values = l.split(";");
              String conceptName = values[0];

              Concept concept = conceptMap.get(conceptName);
              if (concept == null) {
                concept = new Concept();
                concept.setName(toPascalCase(conceptName));
                conceptMap.put(conceptName, concept);
              }

              // add label
              if (!Strings.isNullOrEmpty(values[1])) {
                concept
                    .getAlternativeLabels()
                    .computeIfAbsent(LanguageRegion.ENGLISH, k -> new ArrayList<>())
                    .add(toTitleCase(values[1]));
              }
            });

    // create in WS
    client.createVocabulary(vocabulary);
    conceptMap
        .values()
        .forEach(
            c -> {
              try {
                client.createConcept(vocabularyName, c);
              } catch (Exception ex) {
                log.error("Couldn't create concept: {}", c, ex);
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
