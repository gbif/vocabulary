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
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;

import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** Services for a {@link Vocabulary}. */
public interface VocabularyService extends BaseService<Vocabulary> {

  /**
   * Retrives a vocabulary by its name.
   *
   * @param name name of the vocabulary to retrieve.
   * @return vocabulary
   */
  Vocabulary getByName(@NotBlank String name);

  /**
   * Retrieves pages of {@link Vocabulary} that match with the {@link VocabularySearchParams}
   * received.
   *
   * @param params to do the search.
   * @param page paging parameters
   * @return a list of {@link Vocabulary} ordered by their creation date, newest coming first
   */
  PagingResponse<Vocabulary> list(@Nullable VocabularySearchParams params, @Nullable Pageable page);

  /**
   * Returns suggestions for the given query. It only checks for matches in the name field of the
   * vocabulary.
   *
   * @param query suggestion
   * @param languageRegion locale to filter by
   * @return a list of up to 20 suggested vocabularies
   */
  List<KeyNameResult> suggest(String query, @Nullable LanguageRegion languageRegion);

  /**
   * Deprecates a vocabulary with a replacement.
   *
   * @param key key of the vocabulary to be deprecated
   * @param deprecatedBy name of the actor who deprecates the vocabulary
   * @param replacementKey key of the replacement
   * @param deprecateConcepts if true the concepts of the vocabulary will be deprecated too
   */
  void deprecate(
      long key,
      @NotBlank String deprecatedBy,
      @Nullable Long replacementKey,
      boolean deprecateConcepts);

  /**
   * Deprecates a concept without replacement.
   *
   * @param key key of the concept to be deprecated
   * @param deprecatedBy name of the actor who deprecates the concept
   * @param deprecateConcepts if true the concepts of the concept will be deprecated too
   */
  void deprecateWithoutReplacement(
      long key, @NotBlank String deprecatedBy, boolean deprecateConcepts);

  /**
   * Restores a deprecated concept.
   *
   * @param key key of the concept to undeprecate.
   * @param restoreDeprecatedConcepts if true it restores the deprecated concepts of the vocabulary
   */
  void restoreDeprecated(long key, boolean restoreDeprecatedConcepts);

  /**
   * Deletes a vocabulary and all its concepts.
   *
   * @param key vocabulary key
   */
  void deleteVocabulary(long key);
}
