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

import org.gbif.vocabulary.model.Definition;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.VocabularyEntity;

import java.util.List;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Base interface for the services of {@link VocabularyEntity}.
 *
 * @param <T> as the {@link VocabularyEntity}
 */
public interface BaseService<T extends VocabularyEntity> {

  /**
   * Persists the entity received as parameter.
   *
   * @param entity to create
   * @return key of the newly created entity
   */
  long create(@NotNull @Valid T entity);

  /**
   * Retrieves an entity by its key.
   *
   * @param key key of the entity to retrieve
   * @return entity
   */
  T get(long key);

  /**
   * Updates an entity.
   *
   * @param entity to be updated.
   */
  void update(@NotNull @Valid T entity);

  long addDefinition(long entityKey, @NotNull @Valid Definition definition);

  void updateDefinition(long entityKey, @NotNull @Valid Definition definition);

  void deleteDefinition(long entityKey, long key);

  Definition getDefinition(long entityKey, long key);

  List<Definition> listDefinitions(long entityKey, @Nullable List<LanguageRegion> languageRegions);

  long addLabel(long entityKey, @NotNull @Valid Label label);

  void deleteLabel(long entityKey, long key);

  List<Label> listLabels(long entityKey, @Nullable List<LanguageRegion> languageRegions);
}
