package org.gbif.vocabulary.service;

import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.model.search.KeyNameResult;

import java.util.List;
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
  int create(@NotNull @Valid T entity);

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
   */
  void update(@NotNull @Valid T entity);

  /**
   * Returns suggestions for the given query. It only checks for matches in the name field of the
   * entity.
   *
   * @param query suggestion
   * @return a list of up to 20 suggested entities
   */
  List<KeyNameResult> suggest(@NotNull String query);
}
