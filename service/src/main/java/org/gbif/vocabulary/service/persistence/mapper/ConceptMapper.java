package org.gbif.vocabulary.service.persistence.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.Concept;

import java.util.List;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Mapper for {@link Concept}. */
@Mapper
public interface ConceptMapper extends BaseMapper<Concept> {

  List<Concept> list(
      @Nullable @Param("query") String query,
      @Nullable @Param("vocabularyKey") Integer vocabularyKey,
      @Nullable @Param("parentKey") Integer parentKey,
      @Nullable @Param("replacedByKey") Integer replacedByKey,
      @Nullable @Param("name") String name,
      @Nullable @Param("page") Pageable page);

  long count(
      @Nullable @Param("query") String query,
      @Nullable @Param("vocabularyKey") Integer vocabularyKey,
      @Nullable @Param("parentKey") Integer parentKey,
      @Nullable @Param("replacedByKey") Integer replacedByKey,
      @Nullable @Param("name") String name);
}
