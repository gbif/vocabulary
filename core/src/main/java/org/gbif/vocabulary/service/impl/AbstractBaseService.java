package org.gbif.vocabulary.service.impl;

import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.persistence.mappers.BaseMapper;
import org.gbif.vocabulary.service.BaseService;

import java.util.List;
import javax.validation.constraints.NotNull;

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

  @Override
  public T get(int key) {
    return baseMapper.get(key);
  }

  @Override
  public List<KeyNameResult> suggest(@NotNull String query) {
    return baseMapper.suggest(query);
  }

  protected void checkSimilarities(T entity) {
    List<KeyNameResult> similarities = baseMapper.findSimilarities(entity);
    if (!similarities.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot create entity because it conflicts with other entities, e.g.: "
              + similarities.toString());
    }
  }
}
