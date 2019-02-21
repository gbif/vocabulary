package org.gbif.vocabulary.service.impl;

import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.persistence.mappers.BaseMapper;
import org.gbif.vocabulary.service.BaseService;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import static java.util.Objects.requireNonNull;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Base implementation for {@link BaseService}.
 *
 * @param <T>
 */
abstract class AbstractBaseService<T extends VocabularyEntity> implements BaseService<T> {

  private final BaseMapper<T> baseMapper;

  AbstractBaseService(BaseMapper<T> baseMapper) {
    this.baseMapper = baseMapper;
  }

  @Transactional
  @Override
  public int create(T entity) {
    checkArgument(entity.getKey() == null, "Can't create an entity which already has a key");

    // checking if there is another similar entity.
    checkSimilarities(entity);

    baseMapper.create(entity);

    return entity.getKey();
  }

  @Transactional
  @Override
  public void update(T entity) {
    requireNonNull(entity.getKey());

    T oldEntity = baseMapper.get(entity.getKey());
    requireNonNull(oldEntity, "Couldn't find entity with key: " + entity.getKey());

    checkArgument(oldEntity.getDeprecated() == null, "Cannot update a deprecated entity");
    checkArgument(
        Objects.equals(oldEntity.getDeprecated(), entity.getDeprecated()),
        "Cannot deprecate or restore a deprecated entity while updating");
    checkArgument(Objects.equals(oldEntity.getDeprecatedBy(), entity.getDeprecatedBy()));
    checkArgument(Objects.equals(oldEntity.getReplacedByKey(), entity.getReplacedByKey()));

    checkArgument(oldEntity.getDeleted() == null, "Cannot update a deleted entity");
    checkArgument(
        Objects.equals(oldEntity.getDeleted(), entity.getDeleted()),
        "Cannot delete or restore an entity while updating");

    // update the concept
    baseMapper.update(entity);
  }

  @Override
  public T get(int key) {
    return baseMapper.get(key);
  }

  @Override
  public List<KeyNameResult> suggest(String query) {
    return baseMapper.suggest(query);
  }

  private void checkSimilarities(T entity) {
    List<KeyNameResult> similarities = baseMapper.findSimilarities(entity);
    if (!similarities.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot create entity because it conflicts with other entities, e.g.: "
              + similarities.toString());
    }
  }
}
