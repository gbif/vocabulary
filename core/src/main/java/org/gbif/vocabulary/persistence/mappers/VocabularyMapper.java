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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import javax.annotation.Nullable;

/** Mapper for {@link Vocabulary}. */
@Mapper
public interface VocabularyMapper extends BaseMapper<Vocabulary> {

  List<Vocabulary> list(
      @Nullable @Param("params") VocabularySearchParams params,
      @Nullable @Param("page") Pageable page);

  long count(@Nullable @Param("params") VocabularySearchParams params);

  Vocabulary getByName(@Param("name") String name);

  Long getKeyByName(@Param("name") String name);

  List<KeyNameResult> suggest(
      @Param("query") String query, @Nullable @Param("lang") LanguageRegion language);

  /**
   * Searches for a similar vocabulary whose name or any of its labels are the same as the ones
   * received as parameter.
   *
   * @param normalizedName name that we want to check that is unique in the vocabulary. <b>It must
   *     be normalized</b>
   * @param vocabularyKey if we are updating a vocabulary we exclude it from the searh
   */
  List<KeyNameResult> findSimilarities(
      @Param("name") String normalizedName, @Nullable @Param("vocabularyKey") Long vocabularyKey);

  void delete(@Param("key") long vocabularyKey);
}
