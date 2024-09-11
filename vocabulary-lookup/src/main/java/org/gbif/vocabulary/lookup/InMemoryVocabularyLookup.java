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
package org.gbif.vocabulary.lookup;

import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.export.ConceptExportView;
import org.gbif.vocabulary.model.export.Export;
import org.gbif.vocabulary.tools.VocabularyDownloader;

import java.io.InputStream;
import java.util.ArrayList;
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

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeLabel;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeName;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.replaceNonAsciiCharactersWithEquivalents;

/**
 * Class that allows to load a vocabulary export in memory to do fast lookups by concept labels.
 *
 * <p>Instances of this class have to be created by using a {@link InMemoryVocabularyLookupBuilder}.
 * There are 2 ways to create these instances:
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
 * can be reused. They should be set in the {@link InMemoryVocabularyLookup} when creating the
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
public class InMemoryVocabularyLookup implements VocabularyLookup {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final Cache<String, ConceptExportView> namesCache;
  private final Cache<String, LabelMatch> labelsCache;
  private final Cache<String, ConceptExportView> hiddenLabelsCache;
  private final Cache<Long, ConceptExportView> conceptsByKeyCache;
  private final Function<String, String> prefilter;

  private InMemoryVocabularyLookup(InputStream in, Function<String, String> prefilter) {
    Objects.requireNonNull(in);
    this.prefilter = prefilter;
    namesCache =
        Cache2kBuilder.of(String.class, ConceptExportView.class)
            .eternal(true)
            .entryCapacity(Long.MAX_VALUE)
            .build();
    labelsCache =
        new Cache2kBuilder<String, LabelMatch>() {}.eternal(true)
            .entryCapacity(Long.MAX_VALUE)
            .build();
    hiddenLabelsCache =
        Cache2kBuilder.of(String.class, ConceptExportView.class)
            .eternal(true)
            .entryCapacity(Long.MAX_VALUE)
            .build();
    conceptsByKeyCache =
        Cache2kBuilder.of(Long.class, ConceptExportView.class)
            .eternal(true)
            .entryCapacity(Long.MAX_VALUE)
            .build();

    importVocabulary(in);
  }

  /**
   * Same as {@link #lookup(String, LanguageRegion)} but since there are no language provided we
   * only try to use English if there are several candidates.
   *
   * @param value the value whose concept we are looking for
   * @return the {@link LookupConcept} found. Empty {@link Optional} if there was no match.
   */
  @Override
  public Optional<LookupConcept> lookup(String value) {
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
   * @return the {@link LookupConcept} found. Empty {@link Optional} if there was no match.
   */
  @Override
  public Optional<LookupConcept> lookup(String value, LanguageRegion contextLang) {
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
      ConceptExportView nameMatch = namesCache.get(transformedValue);
      if (nameMatch != null) {
        log.debug(
            "value {} matched with concept {} by name", value, nameMatch.getConcept().getName());
        return Optional.of(toLookupConcept(nameMatch));
      }

      // if no match with names we try with labels
      LabelMatch labelMatch = labelsCache.get(transformedValue);
      if (labelMatch != null) {
        if (labelMatch.allMatches.size() == 1) {
          ConceptExportView conceptMatched = labelMatch.allMatches.iterator().next();
          log.debug(
              "value {} matched with concept {} by label",
              value,
              conceptMatched.getConcept().getName());
          return Optional.of(toLookupConcept(conceptMatched));
        }

        // several candidates found. We try to match by using the language received as discriminator
        // or English as fallback
        Optional<ConceptExportView> langMatch = matchByLanguage(labelMatch, contextLang, value);
        if (langMatch.isPresent()) {
          log.debug(
              "value {} matched with concept {} by language {}",
              value,
              langMatch.get().getConcept().getName(),
              contextLang);
          return Optional.of(toLookupConcept(langMatch.get()));
        }

        log.warn(
            "Couldn't resolve match between all the several candidates found for {}: {}",
            value,
            labelMatch.allMatches);
      }

      // if no match we try with the hidden labels
      ConceptExportView hiddenMatch = hiddenLabelsCache.get(transformedValue);
      if (hiddenMatch != null) {
        log.debug(
            "value {} matched with concept {} by hidden label",
            value,
            hiddenMatch.getConcept().getName());
        return Optional.of(toLookupConcept(hiddenMatch));
      }
    }

    log.info("Couldn't find any match for {}", value);
    return Optional.empty();
  }

  private Optional<ConceptExportView> matchByLanguage(
      LabelMatch match, LanguageRegion lang, String value) {
    Set<ConceptExportView> langMatches = null;
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
      ConceptExportView conceptMatched = langMatches.iterator().next();
      log.debug(
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
    if (conceptsByKeyCache != null) {
      conceptsByKeyCache.close();
    }
  }

  @SneakyThrows
  private void importVocabulary(InputStream in) {
    Export export = OBJECT_MAPPER.readValue(in, Export.class);

    for (ConceptExportView conceptExport : export.getConceptExports()) {
      // add to the cache concepts
      conceptsByKeyCache.put(conceptExport.getConcept().getKey(), conceptExport);

      // add name to the cache
      addNameToCache(conceptExport);

      // add labels to the cache
      conceptExport.getLabel().forEach((key, value) -> addLabelToCache(value, conceptExport, key));

      // add alternative labels to the cache
      conceptExport
          .getAlternativeLabels()
          .forEach((key, value) -> addLabelsToCache(value, conceptExport, key));

      // add hidden labels to the cache
      conceptExport.getHiddenLabels().forEach(label -> addHiddenLabelToCache(label, conceptExport));
    }
  }

  private void addNameToCache(ConceptExportView concept) {
    String normalizedValue =
        replaceNonAsciiCharactersWithEquivalents(normalizeName(concept.getConcept().getName()));
    ConceptExportView existing = namesCache.peekAndPut(normalizedValue, concept);

    if (existing != null) {
      log.warn(
          "Incorrect vocabulary: concept names have to be unique. The concept name {} has the same name as {}",
          concept,
          existing);
    }
  }

  private void addLabelsToCache(
      Set<String> values, ConceptExportView concept, LanguageRegion language) {
    values.forEach(v -> addLabelToCache(v, concept, language));
  }

  private void addLabelToCache(String value, ConceptExportView concept, LanguageRegion language) {
    if (prefilter != null) {
      value = prefilter.apply(value);
    }

    String normalizedValue = replaceNonAsciiCharactersWithEquivalents(normalizeLabel(value));

    Boolean added =
        labelsCache.invoke(
            normalizedValue,
            e -> {
              LabelMatch match = e.getValue();
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

    if (Boolean.FALSE.equals(added)) {
      log.warn("Concept {} not added for value {}", concept, normalizedValue);
    }
  }

  private void addHiddenLabelToCache(String hiddenLabel, ConceptExportView concept) {
    if (prefilter != null) {
      hiddenLabel = prefilter.apply(hiddenLabel);
    }

    String normalizedValue = replaceNonAsciiCharactersWithEquivalents(normalizeLabel(hiddenLabel));
    ConceptExportView existing = hiddenLabelsCache.peekAndPut(normalizedValue, concept);

    if (existing != null
        && !existing.getConcept().getName().equals(concept.getConcept().getName())) {
      log.warn(
          "Incorrect vocabulary: different concepts cannot have the same hidden label. "
              + "The concept hidden label: {} in the concept: {} is also present in: {}",
          hiddenLabel,
          concept.toString(),
          existing.toString());
    }
  }

  private LookupConcept toLookupConcept(ConceptExportView conceptExportView) {
    // find parents
    List<LookupConcept.Parent> parents = new ArrayList<>();
    Long parentKey = conceptExportView.getConcept().getParentKey();
    while (parentKey != null) {
      ConceptExportView parent = conceptsByKeyCache.get(parentKey);

      if (parent == null) {
        break;
      }

      parents.add(LookupConcept.Parent.from(parent));

      if (parentKey.equals(parent.getConcept().getParentKey())) {
        // this should never happen but we protect against it
        break;
      }

      parentKey = parent.getConcept().getParentKey();
    }

    return LookupConcept.of(
        conceptExportView.getConcept(), parents, new ArrayList<>(conceptExportView.getTags()));
  }

  private static class LabelMatch {
    Set<ConceptExportView> allMatches = new HashSet<>();
    Map<LanguageRegion, Set<ConceptExportView>> matchesByLanguage =
        new EnumMap<>(LanguageRegion.class);
  }

  public static InMemoryVocabularyLookupBuilder newBuilder() {
    return new InMemoryVocabularyLookupBuilder();
  }

  /**
   * Builder to create instances of {@link InMemoryVocabularyLookup}.
   *
   * <p>It is required to call one of the 2 available from methods.
   */
  public static class InMemoryVocabularyLookupBuilder {
    private InputStream inputStream;
    private String apiUrl;
    private String vocabularyName;
    private Function<String, String> prefilter;

    public InMemoryVocabularyLookupBuilder from(InputStream inputStream) {
      this.inputStream = inputStream;
      return this;
    }

    public InMemoryVocabularyLookupBuilder from(String apiUrl, String vocabularyName) {
      this.apiUrl = apiUrl;
      this.vocabularyName = vocabularyName;
      return this;
    }

    public InMemoryVocabularyLookupBuilder withPrefilter(Function<String, String> prefilter) {
      this.prefilter = prefilter;
      return this;
    }

    public InMemoryVocabularyLookup build() {
      if (inputStream != null) {
        return new InMemoryVocabularyLookup(inputStream, prefilter);
      } else if (apiUrl != null && vocabularyName != null) {
        return new InMemoryVocabularyLookup(
            VocabularyDownloader.downloadLatestVocabularyVersion(apiUrl, vocabularyName),
            prefilter);
      }

      throw new IllegalArgumentException(
          "Either the inputstream or the API URL and the vocabulary name are required");
    }
  }
}
