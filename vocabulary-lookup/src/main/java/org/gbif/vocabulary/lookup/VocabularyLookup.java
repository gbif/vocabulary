package org.gbif.vocabulary.lookup;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.enums.LanguageRegion;
import org.gbif.vocabulary.model.export.ExportMetadata;
import org.gbif.vocabulary.model.export.VocabularyExport;
import org.gbif.vocabulary.model.normalizers.StringNormalizer;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeLabel;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeName;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.replaceNonAsciiCharactersWithEquivalents;

import static com.google.common.base.Preconditions.checkArgument;

/** Class that allows to load a vocabulary export in memory to do fast lookups by concept labels. */
public class VocabularyLookup implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(VocabularyLookup.class);
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  private final Cache<String, Concept> namesCache;
  private final Cache<String, LabelMatch> labelsCache;
  private ExportMetadata exportMetadata;
  private Vocabulary vocabulary;

  private VocabularyLookup(InputStream in) {
    Objects.requireNonNull(in);

    namesCache =
        Cache2kBuilder.of(String.class, Concept.class)
            .eternal(true)
            .entryCapacity(Long.MAX_VALUE)
            .suppressExceptions(false)
            .build();
    labelsCache =
        new Cache2kBuilder<String, LabelMatch>() {}.eternal(true)
            .entryCapacity(Long.MAX_VALUE)
            .suppressExceptions(false)
            .build();

    importVocabulary(in);
  }

  public static VocabularyLookup load(String apiUrl, String vocabularyName) {
    return new VocabularyLookup(
        VocabularyDownloader.downloadLatestVocabularyVersion(apiUrl, vocabularyName));
  }

  /**
   * Creates a {@link VocabularyLookup} from the vocabulary received.
   *
   * @param in {@link VocabularyExport} as a {@link InputStream}.
   * @return {@link VocabularyLookup} with the vocabulary loaded in memory.
   */
  public static VocabularyLookup load(InputStream in) {
    return new VocabularyLookup(in);
  }

  /**
   * Same as {@link #lookup(String, LanguageRegion)} but since there are no language provided we
   * only try to use English if there are several candidates.
   *
   * @param value the value whose concept we are looking for
   * @return the {@link Concept} found. Empty {@link Optional} if there was no match.
   */
  public Optional<Concept> lookup(String value) {
    return lookup(value, null);
  }

  /**
   * Looks up for a value in the vocabulary.
   *
   * <p>If there is more than 1 match we use the contextLang as a discriminator. If there is still
   * no match we try to use English as a fallback.
   *
   * @param value the value whose concept we are looking for
   * @param contextLang {@link LanguageRegion} to break ties
   * @return the {@link Concept} found. Empty {@link Optional} if there was no match.
   */
  public Optional<Concept> lookup(String value, LanguageRegion contextLang) {
    checkArgument(!Strings.isNullOrEmpty(value), "A value to lookup for is required");

    // base normalization
    String normalizedValue = replaceNonAsciiCharactersWithEquivalents(normalizeLabel(value));

    List<UnaryOperator<String>> transformations =
        ImmutableList.of(
            UnaryOperator.identity(), StringNormalizer::stripNonAlphanumericCharacters);

    for (UnaryOperator<String> t : transformations) {
      String transformedValue = t.apply(normalizedValue);

      // matching by name
      Concept nameMatch = namesCache.get(transformedValue);
      if (nameMatch != null) {
        LOG.info("value {} matched with concept {} by name", value, nameMatch.getName());
        return Optional.of(nameMatch);
      }

      // if no match with names we try with labels
      LabelMatch match = labelsCache.get(transformedValue);
      if (match != null) {
        if (match.allMatches.size() == 1) {
          Concept conceptMatched = match.allMatches.iterator().next();
          LOG.info("value {} matched with concept {} by label", value, conceptMatched.getName());
          return Optional.of(conceptMatched);
        }

        // several candidates found. We try to match by using the language received as discriminator
        // or English as fallback
        Optional<Concept> langMatch = matchByLanguage(match, contextLang, value);
        if (langMatch.isPresent()) {
          LOG.info(
              "value {} matched with concept {} by language {}",
              value,
              langMatch.get().getName(),
              contextLang);
          return langMatch;
        }

        LOG.warn(
            "Couldn't resolve match between all the several candidates found for {}: {}",
            value,
            match.allMatches);
      }
    }

    LOG.info("Couldn't find any match for {}", value);
    return Optional.empty();
  }

  private Optional<Concept> matchByLanguage(LabelMatch match, LanguageRegion lang, String value) {
    Set<Concept> langMatches = null;
    if (lang != null) {
      langMatches = match.matchesByLanguage.get(lang);
    }

    // we try with English as fallback
    if ((langMatches == null || langMatches.size() != 1) && lang != LanguageRegion.ENGLISH) {
      lang = LanguageRegion.ENGLISH;
      langMatches = match.matchesByLanguage.get(lang);
    }

    if (langMatches == null || langMatches.isEmpty()) {
      return Optional.empty();
    }

    if (langMatches.size() == 1) {
      Concept conceptMatched = langMatches.iterator().next();
      LOG.info(
          "Value {} matched with concept {} by using language {}", value, conceptMatched, lang);
      return Optional.of(conceptMatched);
    }

    return Optional.empty();
  }

  @Override
  public void close() throws Exception {
    if (namesCache != null) {
      namesCache.close();
    }
    if (labelsCache != null) {
      labelsCache.close();
    }
  }

  private void importVocabulary(InputStream in) {
    try (JsonParser parser = OBJECT_MAPPER.getFactory().createParser(in)) {
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
          addNameToCache(concept);

          // add labels to the cache
          concept.getLabel().forEach((key, value) -> addLabelToCache(value, concept, key));

          // add alternative labels to the cache
          concept
              .getAlternativeLabels()
              .forEach((key, value) -> addLabelsToCache(value, concept, key));

          // add misapplied labels to the cache
          concept
              .getMisappliedLabels()
              .forEach((key, value) -> addLabelsToCache(value, concept, key));

          parser.nextValue();
        }
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Couldn't parse json vocabulary", e);
    }
  }

  private void addNameToCache(Concept concept) {
    String normalizedValue =
        replaceNonAsciiCharactersWithEquivalents(normalizeName(concept.getName()));
    Concept existing = namesCache.peekAndPut(normalizedValue, concept);

    if (existing != null) {
      throw new IllegalArgumentException(
          "Incorrect vocabulary: concept names have to be unique. The concept name "
              + concept.toString()
              + " has the same name as "
              + existing.toString());
    }
  }

  private void addLabelsToCache(List<String> values, Concept concept, LanguageRegion language) {
    values.forEach(v -> addLabelToCache(v, concept, language));
  }

  private void addLabelToCache(String value, Concept concept, LanguageRegion language) {
    String normalizedValue = replaceNonAsciiCharactersWithEquivalents(normalizeLabel(value));

    boolean added =
        labelsCache.invoke(
            normalizedValue,
            e -> {
              LabelMatch match = e.getOldValue();
              if (match == null) {
                match = new LabelMatch();
              }
              e.setValue(match);

              match.allMatches.add(concept);
              return match
                  .matchesByLanguage
                  .computeIfAbsent(language, l -> new HashSet<>())
                  .add(concept);
            });

    if (!added) {
      LOG.warn("Concept {} not added for value {}", concept, normalizedValue);
    }
  }

  public ExportMetadata getExportMetadata() {
    return exportMetadata;
  }

  public Vocabulary getVocabulary() {
    return vocabulary;
  }

  private static class LabelMatch {
    Set<Concept> allMatches = new HashSet<>();
    Map<LanguageRegion, Set<Concept>> matchesByLanguage = new EnumMap<>(LanguageRegion.class);
  }
}
