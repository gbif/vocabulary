package org.gbif.vocabulary.lookup;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.export.ExportMetadata;
import org.gbif.vocabulary.model.export.VocabularyExport;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;

/** Class that allows to load a vocabulary export in memory to do fast lookups by concept labels. */
public class VocabularyLookup {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  private final Cache<String, Concept> conceptCache;
  private ExportMetadata exportMetadata;
  private Vocabulary vocabulary;

  private VocabularyLookup(InputStream in) {
    conceptCache =
        Cache2kBuilder.of(String.class, Concept.class)
            .eternal(true)
            .entryCapacity(Long.MAX_VALUE)
            .suppressExceptions(false)
            .build();

    processVocabularyExport(in);
  }

  public static VocabularyLookup load(String apiUrl, String vocabularyName) {
    return new VocabularyLookup(VocabularyDownloader.downloadVocabulary(apiUrl, vocabularyName));
  }

  /**
   * Creates a {@link VocabularyLookup} from the vocabulary received.
   *
   * @param in {@link VocabularyExport} as a {@link InputStream}.
   * @return {@link VocabularyLookup} with the vocabulary loaded in memory.
   */
  public static VocabularyLookup load(InputStream in) {
    return new VocabularyLookup(Objects.requireNonNull(in));
  }

  private void processVocabularyExport(InputStream in) {
    JsonFactory jsonFactory = OBJECT_MAPPER.getFactory();

    try (JsonParser parser = jsonFactory.createParser(in)) {
      // root
      parser.nextToken();

      // first element
      parser.nextToken();
      if (parser.getCurrentName().equals(VocabularyExport.METADATA_PROP)) {
        parser.nextToken();
        exportMetadata = OBJECT_MAPPER.readValue(parser, ExportMetadata.class);
        parser.nextValue();
      }

      if (parser.getCurrentName().equals(VocabularyExport.VOCABULARY_PROP)) {
        parser.nextToken();
        vocabulary = OBJECT_MAPPER.readValue(parser, Vocabulary.class);
        parser.nextValue();
      }

      if (parser.getCurrentName().equals(VocabularyExport.CONCEPTS_PROP)) {
        parser.nextToken();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
          Concept concept = OBJECT_MAPPER.readValue(parser, Concept.class);
          concept.getLabel().values().forEach(v -> conceptCache.put(v, concept));
          concept.getAlternativeLabels().values().stream()
              .flatMap(Collection::stream)
              .forEach(v -> conceptCache.put(v, concept));
          concept.getMisspeltLabels().values().stream()
              .flatMap(Collection::stream)
              .forEach(v -> conceptCache.put(v, concept));
          parser.nextValue();
        }
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Couldn't parse json vocabulary", e);
    }
  }
}
