package org.gbif.vocabulary.persistence.mappers;

import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.model.search.KeyNameResult;

import java.util.List;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

/**
 * Base mappers for {@link VocabularyEntity} entities.
 *
 * @param <T> type of the mapper. It has to implement {@link VocabularyEntity}
 */
public interface BaseMapper<T extends VocabularyEntity> {

  T get(@Param("key") int key);

  void create(T entity);

  void update(T entity);

  /** Searchs for a similar entity. */
  List<KeyNameResult> findSimilarities(T entity);

  void deprecate(
      @Param("key") int key,
      @Param("deprecatedBy") String deprecatedBy,
      @Nullable @Param("replacementKey") Integer replacementKey);

  void restoreDeprecated(@Param("key") int key);

  boolean isDeprecated(@Param("key") int key);
}
