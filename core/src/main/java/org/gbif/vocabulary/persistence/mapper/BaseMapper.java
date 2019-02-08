package org.gbif.vocabulary.persistence.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.model.search.KeyNameResult;

import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * Base mapper for {@link VocabularyEntity} entities.
 *
 * @param <T>
 */
public interface BaseMapper<T extends VocabularyEntity> {

  T get(@Param("key") Integer key);

  void create(T entity);

  void delete(@Param("key") Integer key);

  void update(T entity);

  List<T> deleted(@Param("page") Pageable page);

  long countDeleted();

  List<KeyNameResult> suggest(@Param("query") String query);
}
