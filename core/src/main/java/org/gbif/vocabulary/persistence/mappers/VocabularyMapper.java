package org.gbif.vocabulary.persistence.mappers;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.KeyNameResult;

import java.util.List;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Mapper for {@link Vocabulary}. */
@Mapper
public interface VocabularyMapper extends BaseMapper<Vocabulary> {

  // FIXME: create an object for params??
  List<Vocabulary> list(
      @Nullable @Param("query") String query,
      @Nullable @Param("name") String name,
      @Nullable @Param("namespace") String namespace,
      @Nullable @Param("deprecated") Boolean deprecated,
      @Nullable @Param("page") Pageable page);

  long count(
      @Nullable @Param("query") String query,
      @Nullable @Param("name") String name,
      @Nullable @Param("namespace") String namespace,
      @Nullable @Param("deprecated") Boolean deprecated);

  /** Searchs for a similar entity. */
  List<KeyNameResult> findSimilarities(@Param("name") String name);

  List<KeyNameResult> suggest(@Param("query") String query);
}
