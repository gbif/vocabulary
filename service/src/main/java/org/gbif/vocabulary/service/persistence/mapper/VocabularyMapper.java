package org.gbif.vocabulary.service.persistence.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.Vocabulary;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VocabularyMapper extends BaseMapper<Vocabulary> {

  List<Vocabulary> list(
      @Nullable @Param("query") String query, @Nullable @Param("page") Pageable page);

  long count(@Nullable @Param("query") String query);

  // TODO: suggest
}
