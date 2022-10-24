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

import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.VocabularyEntity;

import org.apache.ibatis.annotations.Param;

import javax.annotation.Nullable;

/**
 * Base mappers for {@link VocabularyEntity} entities.
 *
 * @param <T> type of the mapper. It has to implement {@link VocabularyEntity}
 */
public interface BaseMapper<T extends VocabularyEntity> {

  T get(@Param("key") long key);

  void create(T entity);

  void update(T entity);

  void deprecate(
      @Param("key") long key,
      @Param("deprecatedBy") String deprecatedBy,
      @Nullable @Param("replacementKey") Long replacementKey);

  void restoreDeprecated(@Param("key") long key);

  boolean isDeprecated(@Param("key") long key);

  void addLabel(Label label);

  void updateLabel(Label label);

  void deleteLabel(@Param("key") long key);

  List<Label> listLabels(
      @Param("entityKey") long entityKey, @Nullable @Param("lang") LanguageRegion languageRegion);

  Label getLabel(@Param("key") long labelKey);
}
