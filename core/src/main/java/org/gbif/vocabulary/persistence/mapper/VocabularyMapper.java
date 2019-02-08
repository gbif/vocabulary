package org.gbif.vocabulary.persistence.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.Vocabulary;

import java.util.List;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Mapper for {@link Vocabulary}. */
@Mapper
public interface VocabularyMapper extends BaseMapper<Vocabulary> {

  List<Vocabulary> list(
      @Nullable @Param("query") String query,
      @Nullable @Param("name") String name,
      @Nullable @Param("namespace") String namespace,
      @Nullable @Param("page") Pageable page);

  long count(
      @Nullable @Param("query") String query,
      @Nullable @Param("name") String name,
      @Nullable @Param("namespace") String namespace);
}
