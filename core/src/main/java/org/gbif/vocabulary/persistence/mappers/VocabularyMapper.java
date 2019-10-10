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

  Vocabulary getByName(@Param("name") String name);

  List<KeyNameResult> suggest(@Param("query") String query);

  /**
   * Searchs for a similar vocabulary whose name or any of its labels are the same as the ones
   * received as parameter.
   *
   * @param values values that we want to check that are unique in the vocabulary
   * @param vocabularyKey if we are updating a vocabulary we exclude it from the searh
   */
  List<KeyNameResult> findSimilarities(
      @Param("values") List<String> values,
      @Nullable @Param("vocabularyKey") Integer vocabularyKey);
}
