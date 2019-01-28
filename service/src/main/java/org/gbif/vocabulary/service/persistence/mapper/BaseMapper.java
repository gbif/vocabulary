package org.gbif.vocabulary.service.persistence.mapper;

import org.gbif.api.model.common.paging.Pageable;

import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface BaseMapper<T> {

  T get(@Param("key") Integer key);

  void create(T entity);

  void delete(@Param("key") Integer key);

  void update(T entity);

  List<T> deleted(@Param("page") Pageable page);

  long countDeleted();
}
