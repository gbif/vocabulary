package org.gbif.vocabulary.service.impl;

import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.persistence.mappers.BaseMapper;
import org.gbif.vocabulary.service.BaseService;

import java.util.List;

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
