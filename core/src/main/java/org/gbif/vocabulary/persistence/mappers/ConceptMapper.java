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
package org.gbif.vocabulary.persistence.mappers;

import java.util.List;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Definition;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.search.ChildrenResult;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.persistence.dto.LookupDto;
import org.gbif.vocabulary.persistence.dto.SuggestDto;

/** Mapper for {@link Concept}. */
@Mapper
public interface ConceptMapper extends BaseMapper<Concept> {

  List<Concept> list(
      @Nullable @Param("params") ConceptSearchParams params,
      @Nullable @Param("page") Pageable page);

  long count(@Nullable @Param("params") ConceptSearchParams params);

  Concept getByNameAndVocabulary(
      @Param("name") String name, @Param("vocabularyName") String vocabularyName);

  void deprecateInBulk(
      @Param("keys") List<Long> keys,
      @Param("deprecatedBy") String deprecatedBy,
      @Nullable @Param("replacementKey") Long replacementKey);

  void restoreDeprecatedInBulk(@Param("keys") List<Long> keys);

  void updateParent(@Param("keys") List<Long> conceptKeys, @Param("parentKey") long parentKey);

  List<SuggestDto> suggest(
      @Param("query") String query,
      @Param("vocabularyKey") long vocabularyKey,
      @Nullable @Param("lang") LanguageRegion language,
      @Nullable @Param("fallbackLang") LanguageRegion fallbackLang,
      @Param("limit") int limit);

  /**
   * Given a deprecated concept, it finds the current replacement, that's to say, the first
   * replacement not deprecated.
   *
   * @param key key of the deprecated concept
   * @return the key of the current replacement
   */
  Long findReplacement(@Param("key") long key);

  Long getVocabularyKey(@Param("key") long conceptKey);

  /**
   * Searchs for a similar concept whose name or any of its labels are the same as the ones received
   * as parameter.
   *
   * @param normalizedValue value that we want to check that is unique in the vocabulary. <b>It must
   *     be normalized</b>
   * @param languageRegion language to filter by
   * @param vocabularyKey key of the vocabulary whose concepts we'll check
   * @param conceptKey if we are updating a concept we exclude it from the search
   */
  List<KeyNameResult> findSimilarities(
      @Param("normalizedValue") String normalizedValue,
      @Nullable @Param("lang") LanguageRegion languageRegion,
      @Param("vocabularyKey") long vocabularyKey,
      @Nullable @Param("conceptKey") Long conceptKey);

  /**
   * Given a concept, it finds all its non-deprecated parents. That's to say, it finds its direct
   * parent and the parents of its parents.
   *
   * @param conceptKey key of the concept whose parents we're looking for
   * @return list with the names of all the parent concepts
   */
  List<String> findParents(@Param("key") long conceptKey);

  /**
   * Given a list of concepts, it finds the number of children that each concept has.
   *
   * @param parentConcepts list with a key of all the concepts to look for
   * @return list of {@link ChildrenResult}
   */
  List<ChildrenResult> countChildren(@Param("parentConcepts") List<Long> parentConcepts);

  void addTag(@Param("conceptKey") long conceptKey, @Param("tagKey") int tagKey);

  void removeTag(@Param("conceptKey") long conceptKey, @Param("tagKey") int tagKey);

  List<Tag> listTags(@Param("key") long conceptKey);

  void deleteAllConcepts(@Param("vocabularyKey") long vocabularyKey);

  void addAlternativeLabel(@Param("entityKey") long entityKey, @Param("label") Label label);

  void deleteAlternativeLabel(@Param("entityKey") long entityKey, @Param("key") long key);

  List<Label> listAlternativeLabels(
      @Param("entityKey") long entityKey,
      @Nullable @Param("langs") List<LanguageRegion> languageRegions,
      @Nullable @Param("page") Pageable page);

  long countAlternativeLabels(
      @Param("entityKey") long entityKey,
      @Nullable @Param("langs") List<LanguageRegion> languageRegions);

  void addHiddenLabel(@Param("entityKey") long entityKey, @Param("label") HiddenLabel label);

  void deleteHiddenLabel(@Param("entityKey") long entityKey, @Param("key") long key);

  List<HiddenLabel> listHiddenLabels(
      @Param("entityKey") long entityKey, @Nullable @Param("page") Pageable page);

  long countHiddenLabels(@Param("entityKey") long entityKey);

  boolean existsReleaseView(@Param("vocabName") String vocabularyName);

  void createLatestReleaseView(
      @Param("vocabName") String vocabularyName, @Param("vocabKey") long vocabularyKey);

  void updateReleaseViews(@Param("vocabName") String vocabularyName);

  List<Concept> listLatestRelease(
      @Nullable @Param("params") ConceptSearchParams params,
      @Nullable @Param("page") Pageable page,
      @Param("vocabName") String vocabularyName);

  long countLatestRelease(
      @Nullable @Param("params") ConceptSearchParams params,
      @Param("vocabName") String vocabularyName);

  List<SuggestDto> suggestLatestRelease(
      @Param("query") String query,
      @Param("vocabularyKey") long vocabularyKey,
      @Nullable @Param("lang") LanguageRegion languageFilter,
      @Nullable @Param("fallbackLang") LanguageRegion fallbackLang,
      @Param("vocabName") String vocabularyName,
      @Param("limit") int limit);

  List<String> findParentsLatestRelease(
      @Param("key") long conceptKey, @Param("vocabName") String vocabularyName);

  List<ChildrenResult> countChildrenLatestRelease(
      @Param("parentConcepts") List<Long> parentConcepts,
      @Param("vocabName") String vocabularyName);

  Concept getByNameLatestRelease(
      @Param("name") String name, @Param("vocabName") String vocabularyName);

  List<Definition> listDefinitionsLatestRelease(
      @Param("entityKey") long entityKey,
      @Nullable @Param("langs") List<LanguageRegion> languageRegions,
      @Param("vocabName") String vocabularyName);

  List<Label> listLabelsLatestRelease(
      @Param("entityKey") long entityKey,
      @Nullable @Param("langs") List<LanguageRegion> languageRegions,
      @Param("vocabName") String vocabularyName);

  List<Label> listAlternativeLabelsLatestRelease(
      @Param("entityKey") long entityKey,
      @Nullable @Param("langs") List<LanguageRegion> languageRegions,
      @Nullable @Param("page") Pageable page,
      @Param("vocabName") String vocabularyName);

  long countAlternativeLabelsLatestRelease(
      @Param("entityKey") long entityKey,
      @Nullable @Param("langs") List<LanguageRegion> languageRegions,
      @Param("vocabName") String vocabularyName);

  List<HiddenLabel> listHiddenLabelsLatestRelease(
      @Param("entityKey") long entityKey,
      @Nullable @Param("page") Pageable page,
      @Param("vocabName") String vocabularyName);

  long countHiddenLabelsLatestRelease(
      @Param("entityKey") long entityKey, @Param("vocabName") String vocabularyName);

  List<LookupDto> lookup(@Param("value") String value, @Param("vocabularyKey") long vocabularyKey);

  List<LookupDto> lookupLatestRelease(
      @Param("value") String value,
      @Param("vocabularyKey") long vocabularyKey,
      @Param("vocabName") String vocabularyName);
}
