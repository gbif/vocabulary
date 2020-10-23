/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.vocabulary.lookup;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.enums.LanguageRegion;
import org.gbif.vocabulary.model.export.ExportMetadata;
import org.gbif.vocabulary.model.export.VocabularyExport;
import org.gbif.vocabulary.tools.VocabularyDownloader;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeLabel;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeName;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.replaceNonAsciiCharactersWithEquivalents;

/**
 * Class that allows to load a vocabulary export in memory to do fast lookups by concept labels.
 *
 * <p>Instances of this class have to be created by using a {@link VocabularyLookupBuilder}. There
 * are 2 ways to create these instances:
 *
 * <ul>
 *   <li>Load the vocabulary from an {@link InputStream}:
 *       <pre>
 *            VocabularyLookup.newBuilder().from(new InputStream(...)).build();
 *       </pre>
 *   <li>Download the latest version of the vocabulary via the vocabulary API:
 *       <pre>
 *            VocabularyLookup.newBuilder().from("http://api.gbif.org/v1/", "LifeStage").build();
 *       </pre>
 * </ul>
 *
 * Optionally, a pre-filter can be added. The pre-filter will be applied to the value received
 * before performing the lookup. There are some predefined pre-filters in {@link PreFilters} that
 * can be reused. They should be set in the {@link VocabularyLookupBuilder} when creating the
 * instance and they will be applied to all the lookups:
 *
 * <pre>
 *      VocabularyLookup.newBuilder().from(new InputStream(...))
 *          .withPrefilter(PreFilters.REMOVE_NON_ALPHANUMERIC).build();
 * </pre>
 *
 * Also, multiple prefilters can be chained:
 *
 * <pre>
 *        VocabularyLookup.newBuilder().from(new InputStream(...))
 *            .withPrefilter(
 *             PreFilters.REMOVE_NON_ALPHANUMERIC.andThen(
 *                 PreFilters.REMOVE_PARENTHESIS_CONTENT_SUFFIX)).build();
 * </pre>
 *
 * Notice that there is no need to remove whitespaces or take care of non-ASCII characters. This is
 * already handled by this class and will be normalized before performing a lookup.
 */
@Slf4j
public class VocabularyLookup implements AutoCloseable, Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(VocabularyLookup.class);
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  private final Cache<String, Concept> namesCache;
  private final Cache<String, LabelMatch> labelsCache;
  private final Cache<String, Concept> hiddenLabelsCache;
  private final Function<String, String> prefilter;
  private ExportMetadata exportMetadata;
  private Vocabulary vocabulary;

  private VocabularyLookup(InputStream in, Function<String, String> prefilter) {
    Objects.requireNonNull(in);
    this.prefilter = prefilter;

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
    hiddenLabelsCache =
        Cache2kBuilder.of(String.class, Concept.class)
            .eternal(true)
            .entryCapacity(Long.MAX_VALUE)
            .suppressExceptions(false)
            .build();

    importVocabulary(in);
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
   * no match it tries to use English as a fallback. Otherwise, it returns an empty {@link
   * Optional}.
   *
   * @param value the value whose concept we are looking for
   * @param contextLang {@link LanguageRegion} to break ties
   * @return the {@link Concept} found. Empty {@link Optional} if there was no match.
   */
  public Optional<Concept> lookup(String value, LanguageRegion contextLang) {
    if (value == null || value.isEmpty()) {
      return Optional.empty();
    }

    // apply the pre-filters
    if (prefilter != null) {
      value = prefilter.apply(value);
    }

    // base normalization
    String normalizedValue = replaceNonAsciiCharactersWithEquivalents(normalizeLabel(value));

    List<UnaryOperator<String>> transformations =
        Collections.singletonList(UnaryOperator.identity());

    for (UnaryOperator<String> t : transformations) {
      String transformedValue = t.apply(normalizedValue);

      // matching by name
      Concept nameMatch = namesCache.get(transformedValue);
      if (nameMatch != null) {
        LOG.info("value {} matched with concept {} by name", value, nameMatch.getName());
        return Optional.of(nameMatch);
      }

      // if no match with names we try with labels
      LabelMatch labelMatch = labelsCache.get(transformedValue);
      if (labelMatch != null) {
        if (labelMatch.allMatches.size() == 1) {
          Concept conceptMatched = labelMatch.allMatches.iterator().next();
          LOG.info("value {} matched with concept {} by label", value, conceptMatched.getName());
          return Optional.of(conceptMatched);
        }

        // several candidates found. We try to match by using the language received as discriminator
        // or English as fallback
        Optional<Concept> langMatch = matchByLanguage(labelMatch, contextLang, value);
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
            labelMatch.allMatches);
      }

      // if no match we try with the hidden labels
      Concept hiddenMatch = hiddenLabelsCache.get(transformedValue);
      if (hiddenMatch != null) {
        LOG.info("value {} matched with concept {} by hidden label", value, hiddenMatch);
        return Optional.of(hiddenMatch);
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
  public void close() {
    if (namesCache != null) {
      namesCache.close();
    }
    if (labelsCache != null) {
      labelsCache.close();
    }
    if (hiddenLabelsCache != null) {
      hiddenLabelsCache.close();
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

          // add hidden labels to the cache
          concept.getHiddenLabels().forEach(label -> addHiddenLabelToCache(label, concept));

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
      log.warn(
          "Incorrect vocabulary: concept names have to be unique. The concept name {} has the same name as {}",
          concept.toString(),
          existing.toString());
    }
  }

  private void addLabelsToCache(List<String> values, Concept concept, LanguageRegion language) {
    values.forEach(v -> addLabelToCache(v, concept, language));
  }

  private void addLabelToCache(String value, Concept concept, LanguageRegion language) {
    if (prefilter != null) {
      value = prefilter.apply(value);
    }

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

  private void addHiddenLabelToCache(String hiddenLabel, Concept concept) {
    if (prefilter != null) {
      hiddenLabel = prefilter.apply(hiddenLabel);
    }

    String normalizedValue = replaceNonAsciiCharactersWithEquivalents(normalizeLabel(hiddenLabel));
    Concept existing = hiddenLabelsCache.peekAndPut(normalizedValue, concept);

    if (existing != null && !existing.getName().equals(concept.getName())) {
      log.warn(
          "Incorrect vocabulary: different concepts cannot have the same hidden label. "
              + "The concept hidden label: {} in the concept: {} is also present in: {}",
          hiddenLabel,
          concept.toString(),
          existing.toString());
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

  public static VocabularyLookupBuilder newBuilder() {
    return new VocabularyLookupBuilder();
  }

  /**
   * Builder to create instances of {@link VocabularyLookup}.
   *
   * <p>It is required to call one of the 2 available from methods.
   */
  public static class VocabularyLookupBuilder {
    private InputStream inputStream;
    private String apiUrl;
    private String vocabularyName;
    private Function<String, String> prefilter;

    public VocabularyLookupBuilder from(InputStream inputStream) {
      this.inputStream = inputStream;
      return this;
    }

    public VocabularyLookupBuilder from(String apiUrl, String vocabularyName) {
      this.apiUrl = apiUrl;
      this.vocabularyName = vocabularyName;
      return this;
    }

    public VocabularyLookupBuilder withPrefilter(Function<String, String> prefilter) {
      this.prefilter = prefilter;
      return this;
    }

    public VocabularyLookup build() {
      if (inputStream != null) {
        return new VocabularyLookup(inputStream, prefilter);
      } else if (apiUrl != null && vocabularyName != null) {
        return new VocabularyLookup(
            VocabularyDownloader.downloadLatestVocabularyVersion(apiUrl, vocabularyName),
            prefilter);
      }

      throw new IllegalArgumentException(
          "Either the inputstream or the api url and the vocabulary name are required");
    }
  }
}
