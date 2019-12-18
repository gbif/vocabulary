package org.gbif.vocabulary.persistence.mappers;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.search.ChildrenCountResult;
import org.gbif.vocabulary.model.search.KeyNameResult;

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
      @Nullable @Param("deprecated") Boolean deprecated,
      @Nullable @Param("key") Integer key,
      @Nullable @Param("hasParent") Boolean hasParent,
      @Nullable @Param("hasReplacement") Boolean hasReplacement,
      @Nullable @Param("page") Pageable page);

  long count(
      @Nullable @Param("query") String query,
      @Nullable @Param("vocabularyKey") Integer vocabularyKey,
      @Nullable @Param("parentKey") Integer parentKey,
      @Nullable @Param("replacedByKey") Integer replacedByKey,
      @Nullable @Param("name") String name,
      @Nullable @Param("deprecated") Boolean deprecated,
      @Nullable @Param("key") Integer key,
      @Nullable @Param("hasParent") Boolean hasParent,
      @Nullable @Param("hasReplacement") Boolean hasReplacement);

  Concept getByNameAndVocabulary(
      @Param("name") String name, @Param("vocabularyName") String vocabularyName);

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
      @Param("values") List<String> normalizedValues,
      @Param("vocabularyKey") int vocabularyKey,
      @Nullable @Param("conceptKey") Integer conceptKey);

  /**
   * Given a concept, it finds all its non-deprecated parents. That's to say, it finds its direct
   * parent and the parents of its parents.
   *
   * @param conceptKey key of the concept whose parents we're looking for
   * @return list with the names of all the parent concepts
   */
  List<String> findParents(@Param("key") int conceptKey);

  /**
   * Given a list of concepts, it finds the number of children that each concept has.
   *
   * @param parentConcepts list with a key of all the concepts to look for
   * @return list of {@link ChildrenCountResult}
   */
  List<ChildrenCountResult> countChildren(@Param("parentConcepts") List<Integer> parentConcepts);
}
