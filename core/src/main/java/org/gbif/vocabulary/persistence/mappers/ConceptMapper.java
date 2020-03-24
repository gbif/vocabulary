package org.gbif.vocabulary.persistence.mappers;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.search.ChildrenCountResult;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.persistence.parameters.NormalizedValuesParam;

import java.util.List;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Mapper for {@link Concept}. */
@Mapper
public interface ConceptMapper extends BaseMapper<Concept> {

  List<Concept> list(
      @Nullable @Param("query") String query,
      @Nullable @Param("vocabularyKey") Long vocabularyKey,
      @Nullable @Param("parentKey") Long parentKey,
      @Nullable @Param("replacedByKey") Long replacedByKey,
      @Nullable @Param("name") String name,
      @Nullable @Param("deprecated") Boolean deprecated,
      @Nullable @Param("key") Long key,
      @Nullable @Param("hasParent") Boolean hasParent,
      @Nullable @Param("hasReplacement") Boolean hasReplacement,
      @Nullable @Param("page") Pageable page);

  long count(
      @Nullable @Param("query") String query,
      @Nullable @Param("vocabularyKey") Long vocabularyKey,
      @Nullable @Param("parentKey") Long parentKey,
      @Nullable @Param("replacedByKey") Long replacedByKey,
      @Nullable @Param("name") String name,
      @Nullable @Param("deprecated") Boolean deprecated,
      @Nullable @Param("key") Long key,
      @Nullable @Param("hasParent") Boolean hasParent,
      @Nullable @Param("hasReplacement") Boolean hasReplacement);

  Concept getByNameAndVocabulary(
      @Param("name") String name, @Param("vocabularyName") String vocabularyName);

  void deprecateInBulk(
      @Param("keys") List<Long> keys,
      @Param("deprecatedBy") String deprecatedBy,
      @Nullable @Param("replacementKey") Long replacementKey);

  void restoreDeprecatedInBulk(@Param("keys") List<Long> keys);

  void updateParent(@Param("keys") List<Long> conceptKeys, @Param("parentKey") long parentKey);

  List<KeyNameResult> suggest(
      @Param("query") String query, @Param("vocabularyKey") long vocabularyKey);

  /**
   * Given a deprecated concept, it finds the current replacement, that's to say, the first
   * replacement not deprecated.
   *
   * @param key key of the deprecated concept
   * @return the key of the current replacement
   */
  Long findReplacement(@Param("key") long key);

  Long getVocabularyKey(@Param("key") long conceptKey);

  /**
   * Searchs for a similar concept whose name or any of its labels are the same as the ones received
   * as parameter.
   *
   * @param normalizedValues values that we want to check that are unique in the vocabulary. <b>They
   *     must be normalized</b>
   * @param vocabularyKey key of the vocabulary whose concepts we'll check
   * @param conceptKey if we are updating a concept we exclude it from the searh
   */
  List<KeyNameResult> findSimilarities(
      @Param("normalizedValues") List<NormalizedValuesParam> normalizedValues,
      @Param("vocabularyKey") long vocabularyKey,
      @Nullable @Param("conceptKey") Long conceptKey);

  /**
   * Given a concept, it finds all its non-deprecated parents. That's to say, it finds its direct
   * parent and the parents of its parents.
   *
   * @param conceptKey key of the concept whose parents we're looking for
   * @return list with the names of all the parent concepts
   */
  List<String> findParents(@Param("key") long conceptKey);

  /**
   * Given a list of concepts, it finds the number of children that each concept has.
   *
   * @param parentConcepts list with a key of all the concepts to look for
   * @return list of {@link ChildrenCountResult}
   */
  List<ChildrenCountResult> countChildren(@Param("parentConcepts") List<Long> parentConcepts);
}
