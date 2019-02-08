package org.gbif.vocabulary.service.impl;

import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.persistence.mapper.BaseMapper;
import org.gbif.vocabulary.service.BaseService;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
  public int create(@NotNull @Valid T entity) {
    requireNonNull(entity);
    checkArgument(entity.getKey() == null, "Can't create an entity which already has a key");

    baseMapper.create(entity);

    return entity.getKey();
  }

  @Override
  public T get(int key) {
    return baseMapper.get(key);
  }

  @Transactional
  @Override
  public T update(T entity) {
    requireNonNull(entity);
    requireNonNull(entity.getKey());

    T oldEntity = baseMapper.get(entity.getKey());
    requireNonNull(oldEntity, "Couldn't find entity with key: " + entity.getKey());

    if (oldEntity.getDeleted() != null) {
      checkArgument(
          entity.getDeleted() == null,
          "Unable to update a previously deleted entity unless you clear the deletion timestamp");
    } else {
      checkArgument(entity.getDeleted() == null, "Can't delete a entity when updating");
    }

    baseMapper.update(entity);

    return baseMapper.get(entity.getKey());
  }

  @Transactional
  @Override
  public void delete(int key) {
    baseMapper.delete(key);
  }
}
