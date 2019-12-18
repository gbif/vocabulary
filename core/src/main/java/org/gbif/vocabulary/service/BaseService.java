package org.gbif.vocabulary.service;

import org.gbif.vocabulary.model.VocabularyEntity;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Base interface for the services of {@link VocabularyEntity}.
 *
 * @param <T>
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
}
