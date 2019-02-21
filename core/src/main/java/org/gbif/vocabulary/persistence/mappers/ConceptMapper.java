package org.gbif.vocabulary.persistence.mappers;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.Concept;

import java.util.List;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Mapper for {@link Concept}. */
@Mapper
public interface ConceptMapper extends BaseMapper<Concept> {

  // FIXME: create an object for params
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

  void deprecateInBulk(
      @Param("keys") List<Integer> keys,
      @Param("deprecatedBy") String deprecatedBy,
      @Nullable @Param("replacementKey") Integer replacementKey);

  void restoreDeprecatedInBulk(@Param("keys") List<Integer> keys);

  void updateParent(@Param("keys") List<Integer> conceptKeys, @Param("parentKey") int parentKey);

  /**
   * Given a deprecated concept, it finds the current replacement, that's to say, the first
   * replacement not deprecated.
   *
   * @param key key of the deprecated concept
   * @return the key of the current replacement
   */
  Integer findReplacement(@Param("key") int key);
}
