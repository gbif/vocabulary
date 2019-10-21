package org.gbif.vocabulary.lookup;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.export.ExportMetadata;
import org.gbif.vocabulary.model.export.VocabularyExport;
import org.gbif.vocabulary.model.normalizers.StringNormalizer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;

import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeLabel;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeName;

import static com.google.common.base.Preconditions.checkArgument;

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

  public Optional<Concept> lookup(String value) {
    checkArgument(!Strings.isNullOrEmpty(value), "A value to lookup for is required");

    // base normalization
    String normalizedValue = normalizeLabel(value);

    List<UnaryOperator<String>> normalizations =
        ImmutableList.of(
            UnaryOperator.identity(),
            StringNormalizer::normalizeName,
            StringNormalizer::replaceNonAsciiCharactersWithEquivalents,
            StringNormalizer::replaceNonAlphanumericCharacters);

    return normalizations.stream()
        .map(n -> conceptCache.get(n.apply(normalizedValue)))
        .filter(Objects::nonNull)
        .findFirst();
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

          // add name to the cache
          conceptCache.put(normalizeName(concept.getName()), concept);

          // add labels to the cache
          concept.getLabel().values().stream()
              .map(StringNormalizer::normalizeLabel)
              .forEach(v -> conceptCache.put(v, concept));

          // add alternative and misapplied labels to the cache
          Stream.concat(
                  concept.getAlternativeLabels().values().stream(),
                  concept.getMisappliedLabels().values().stream())
              .flatMap(Collection::stream)
              .map(StringNormalizer::normalizeLabel)
              .forEach(v -> conceptCache.put(v, concept));

          parser.nextValue();
        }
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Couldn't parse json vocabulary", e);
    }
  }
}
