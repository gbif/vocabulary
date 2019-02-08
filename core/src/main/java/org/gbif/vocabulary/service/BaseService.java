package org.gbif.vocabulary.service;

import org.gbif.vocabulary.model.VocabularyEntity;

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
  int create(T entity);

  /**
   * Retrieves an entity by its key.
   *
   * @param key key of the entity to retrieve
   * @return entity
   */
  T get(int key);

  /**
   * Updates an entity.
   *
   * @param entity to be updated.
   * @return entity updated.
   */
  T update(T entity);

  /**
   * Deletes a entity by its key.
   *
   * @param key of the entity to delete
   */
  void delete(int key);
}
