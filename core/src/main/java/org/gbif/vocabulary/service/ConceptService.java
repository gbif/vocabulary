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
package org.gbif.vocabulary.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Definition;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.search.ChildrenResult;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.LookupResult;
import org.gbif.vocabulary.model.search.SuggestResult;

import java.util.List;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** Services for a {@link Concept}. */
public interface ConceptService extends BaseService<Concept> {

  /**
   * Retrieves a concept by its name and its vocabulary name.
   *
   * @param name name of the concept
   * @param vocabularyName name of the vocabulary
   * @return concept
   */
  Concept getByNameAndVocabulary(@NotBlank String name, @NotBlank String vocabularyName);

  /**
   * Retrieves pages of {@link Concept} that match with the {@link ConceptSearchParams} received.
   *
   * @param params to do the search
   * @param page paging parameters
   * @return a list of {@link Concept} ordered by their creation date, newest coming first
   */
  PagingResponse<Concept> list(@Nullable ConceptSearchParams params, @Nullable Pageable page);

  /**
   * Returns suggestions for the given query. It only checks for matches in the name field of the
   * concept within the specified vocabulary.
   *
   * @param query suggestion
   * @param vocabularyKey key of the vocabulary
   * @param languageRegion locale to filter by
   * @param fallbackLanguageRegion fallback locale to show in the response
   * @param limit to limit the results, up to 20
   * @return a list of up to 20 suggested concepts
   */
  List<SuggestResult> suggest(
      String query,
      long vocabularyKey,
      @Nullable LanguageRegion languageRegion,
      @Nullable LanguageRegion fallbackLanguageRegion,
      @Nullable Integer limit);

  /**
   * Deprecates a concept with a replacement.
   *
   * @param key key of the concept to be deprecated
   * @param deprecatedBy name of the actor who deprecates the concept
   * @param replacementKey key of the replacement
   * @param deprecateChildren if true the children of the concept will be deprecated too
   */
  void deprecate(
      long key,
      @NotBlank String deprecatedBy,
      @Nullable Long replacementKey,
      boolean deprecateChildren);

  /**
   * Deprecates a concept without replacement.
   *
   * @param key key of the concept to be deprecated
   * @param deprecatedBy name of the actor who deprecates the concept
   * @param deprecateChildren if true the children of the concept will be deprecated too
   */
  void deprecateWithoutReplacement(
      long key, @NotBlank String deprecatedBy, boolean deprecateChildren);

  /**
   * Restores a deprecated concept.
   *
   * @param key key of the concept to undeprecate.
   * @param restoreDeprecatedChildren if true it restores the deprecated children of the concept
   */
  void restoreDeprecated(long key, boolean restoreDeprecatedChildren);

  /**
   * Finds the parents of a concept. It includes not only its direct parent, but also the parents of
   * each parent.
   *
   * @param conceptKey key of the concept whose parents we're looking for
   * @return list with the names of the parents
   */
  List<String> findParents(long conceptKey);

  /**
   * Counts the number of children of each of the concept parents specified.
   *
   * @param conceptParents keys of the concepts whose children we'll count
   * @return list of {@link ChildrenResult}
   */
  List<ChildrenResult> countChildren(List<Long> conceptParents);

  /**
   * Adds a {@link org.gbif.vocabulary.model.Tag} to the specified concept.
   *
   * @param conceptKey key of the concept where the tag will be added to
   * @param tagKey key of the tag to add
   */
  void addTag(long conceptKey, int tagKey);

  /**
   * Removes a {@link org.gbif.vocabulary.model.Tag} from the specified concept.
   *
   * @param conceptKey key of the concept where the tag will be removed from
   * @param tagKey key of the tag to remove
   */
  void removeTag(long conceptKey, int tagKey);

  /**
   * Lists all the tags of the concept.
   *
   * @param conceptKey key of the concept whose tags we're retrieving.
   * @return list of {@link Tag}
   */
  List<Tag> listTags(long conceptKey);

  long addAlternativeLabel(long entityKey, @NotNull @Valid Label label);

  void deleteAlternativeLabel(long entityKey, long key);

  PagingResponse<Label> listAlternativeLabels(
      long entityKey, @Nullable List<LanguageRegion> languageRegions, @Nullable Pageable page);

  long addHiddenLabel(long entityKey, @NotNull @Valid HiddenLabel label);

  void deleteHiddenLabel(long entityKey, long key);

  /**
   * Lists all hidden labels of a concept.
   *
   * @param entityKey key of the concept
   * @param query optional search term to filter hidden labels
   * @param page paging parameters
   * @return list of hidden labels
   */
  PagingResponse<HiddenLabel> listHiddenLabels(
      long entityKey, @Nullable String query, @Nullable Pageable page);

  /**
   * Checks if the views of the latest release of a vocabulary exist. They should always exist
   * unless the vocabulary hasn't been released yet.
   *
   * @param vocabularyName vocabulary to check the views for
   * @return true if the views exist, false otherwise
   */
  boolean existsLatestReleaseView(String vocabularyName);

  /**
   * Creates the views of the latest release of the vocabulary.
   *
   * @param vocabularyName vocabulary to create the views for
   * @param vocabularyKey key of the vocabulary
   */
  void createLatestReleaseView(String vocabularyName, long vocabularyKey);

  /**
   * Updates the views of the latest release of the vocabulary.
   *
   * @param vocabularyName vocabulary whose views will be updated
   */
  void updateLatestReleaseView(String vocabularyName);

  /**
   * It works as {@link #list(ConceptSearchParams, Pageable)} but it queries the latest release of *
   * the vocabulary instead of the actual data.
   */
  PagingResponse<Concept> listLatestRelease(
      @Nullable ConceptSearchParams params, @Nullable Pageable page, String vocabularyName);

  /**
   * It works as {@link #suggest(String, long, LanguageRegion, LanguageRegion, Integer)} but it
   * queries the latest release of the vocabulary instead of the actual data.
   */
  List<SuggestResult> suggestLatestRelease(
      String query,
      long vocabularyKey,
      @Nullable LanguageRegion languageRegion,
      @Nullable LanguageRegion fallbackLanguageRegion,
      String vocabularyName,
      Integer limit);

  /**
   * It works as {@link #getByNameAndVocabulary(String, String)} but it queries the latest release
   * of the vocabulary instead of the actual data.
   */
  Concept getByNameLatestRelease(@NotBlank String name, @NotBlank String vocabularyName);

  /**
   * It works as {@link #findParentsLatestRelease(long, String)} but it queries the latest release
   * of the vocabulary instead of the actual data.
   */
  List<String> findParentsLatestRelease(long conceptKey, String vocabularyName);

  /**
   * It works as {@link #countChildrenLatestRelease(List, String)} but it queries the latest release
   * of the vocabulary instead of the actual data.
   */
  List<ChildrenResult> countChildrenLatestRelease(List<Long> conceptParents, String vocabularyName);

  /**
   * It works as {@link #listDefinitions(long, List)} but it queries the latest release * of the
   * vocabulary instead of the actual data.
   */
  List<Definition> listDefinitionsLatestRelease(
      long entityKey, @Nullable List<LanguageRegion> languageRegions, String vocabularyName);

  /**
   * It works as {@link #listLabels(long, List)} but it queries the latest release * of the
   * vocabulary instead of the actual data.
   */
  List<Label> listLabelsLatestRelease(
      long entityKey, @Nullable List<LanguageRegion> languageRegions, String vocabularyName);

  /**
   * It works as {@link #listAlternativeLabels(long, List, Pageable)} but it queries the latest
   * release * of the vocabulary instead of the actual data.
   */
  PagingResponse<Label> listAlternativeLabelsLatestRelease(
      long entityKey,
      @Nullable List<LanguageRegion> languageRegions,
      @Nullable Pageable page,
      String vocabularyName);

  /**
   * It works as {@link #listHiddenLabels(long, String, Pageable)} but it queries the latest release
   * of the vocabulary instead of the actual data.
   */
  PagingResponse<HiddenLabel> listHiddenLabelsLatestRelease(
      long entityKey, @Nullable String query, @Nullable Pageable page, String vocabularyName);

  /**
   * Lookups concepts that match the given value.
   *
   * @param value value to match against the concepts
   * @param vocabularyName vocabulary to use in the lookup
   * @param languageRegion language to use as discriminator. English is used as fallback.
   * @return list of the concepts found
   */
  List<LookupResult> lookup(
      String value, String vocabularyName, @Nullable LanguageRegion languageRegion);

  /**
   * It works as
   *
   * @param value value to match against the concepts
   * @param vocabularyName vocabulary to use in the lookup
   * @param languageRegion language to use as discriminator. English is used as fallback.
   * @return list of the concepts found
   */
  List<LookupResult> lookupLatestRelease(
      String value, String vocabularyName, @Nullable LanguageRegion languageRegion);
}
