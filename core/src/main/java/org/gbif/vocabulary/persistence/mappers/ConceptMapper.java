package org.gbif.vocabulary.persistence.mappers;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.search.KeyNameResult;

import java.util.List;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Mapper for {@link Concept}. */
@Mapper
public interface ConceptMapper extends BaseMapper<Concept> {

  // TODO: create an object for params
  List<Concept> list(
      @Nullable @Param("query") String query,
      @Nullable @Param("vocabularyKey") Integer vocabularyKey,
      @Nullable @Param("parentKey") Integer parentKey,
      @Nullable @Param("replacedByKey") Integer replacedByKey,
      @Nullable @Param("name") String name,
      @Nullable @Param("deprecated") Boolean deprecated,
      @Nullable @Param("page") Pageable page);

  long count(
      @Nullable @Param("query") String query,
      @Nullable @Param("vocabularyKey") Integer vocabularyKey,
      @Nullable @Param("parentKey") Integer parentKey,
      @Nullable @Param("replacedByKey") Integer replacedByKey,
      @Nullable @Param("name") String name,
      @Nullable @Param("deprecated") Boolean deprecated);

  Concept getByNameAndVocabulary(@Param("name") String name, @Param("vocabularyName") String vocabularyName);

  void deprecateInBulk(
      @Param("keys") List<Integer> keys,
      @Param("deprecatedBy") String deprecatedBy,
      @Nullable @Param("replacementKey") Integer replacementKey);

  void restoreDeprecatedInBulk(@Param("keys") List<Integer> keys);

  void updateParent(@Param("keys") List<Integer> conceptKeys, @Param("parentKey") int parentKey);

  List<KeyNameResult> suggest(
      @Param("query") String query, @Param("vocabularyKey") int vocabularyKey);

  /**
   * Given a deprecated concept, it finds the current replacement, that's to say, the first
   * replacement not deprecated.
   *
   * @param key key of the deprecated concept
   * @return the key of the current replacement
   */
  Integer findReplacement(@Param("key") int key);

  Integer getVocabularyKey(@Param("key") int conceptKey);
}
