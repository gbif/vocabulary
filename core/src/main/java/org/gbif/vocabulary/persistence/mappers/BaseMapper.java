package org.gbif.vocabulary.persistence.mappers;

import org.gbif.vocabulary.model.VocabularyEntity;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

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
}
