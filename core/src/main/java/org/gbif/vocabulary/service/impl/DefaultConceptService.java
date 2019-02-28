package org.gbif.vocabulary.service.impl;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;
import org.gbif.vocabulary.service.ConceptService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static java.util.Objects.requireNonNull;

import static com.google.common.base.Preconditions.checkArgument;

/** Default implementation for {@link ConceptService}. */
@Service
@Validated
public class DefaultConceptService implements ConceptService {

  private final ConceptMapper conceptMapper;
  private final VocabularyMapper vocabularyMapper;

  @Autowired
  public DefaultConceptService(ConceptMapper conceptMapper, VocabularyMapper vocabularyMapper) {
    this.conceptMapper = conceptMapper;
    this.vocabularyMapper = vocabularyMapper;
  }

  @Override
  public Concept get(int key) {
    return conceptMapper.get(key);
  }

  @Transactional
  @Override
  public int create(@NotNull @Valid Concept concept) {
    checkArgument(concept.getKey() == null, "Can't create a concept which already has a key");

    // checking if there is another similar concept.
    List<KeyNameResult> similarities =
        conceptMapper.findSimilarities(concept.getName(), concept.getVocabularyKey());
    if (!similarities.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot create concept because it conflicts with other entities, e.g.: "
              + similarities.toString());
    }

    checkArgument(
        !vocabularyMapper.isDeprecated(concept.getVocabularyKey()),
        "Cannot create a concept for a deprecated vocabulary");

    if (concept.getParentKey() != null) {
      checkArgument(
          concept.getVocabularyKey().equals(conceptMapper.getVocabularyKey(concept.getParentKey())),
          "A concept and its parent must belong to the same vocabulary");
      checkArgument(
          !conceptMapper.isDeprecated(concept.getParentKey()),
          "Cannot create a concept with a deprecated parent");
    }

    conceptMapper.create(concept);

    return concept.getKey();
  }

  @Transactional
  @Override
  public void update(@NotNull @Valid Concept concept) {
    requireNonNull(concept.getKey());

    Concept oldConcept = conceptMapper.get(concept.getKey());
    requireNonNull(oldConcept, "Couldn't find concept with key: " + concept.getKey());

    if (!Objects.equals(oldConcept.getVocabularyKey(), concept.getVocabularyKey())) {
      throw new IllegalArgumentException("A concept cannot be transferred to another vocabulary");
    }

    if (!Objects.equals(oldConcept.getParentKey(), concept.getParentKey())) {
      // parent is being updated
      checkArgument(
          concept.getVocabularyKey().equals(conceptMapper.getVocabularyKey(concept.getParentKey())),
          "A concept and its parent must belong to the same vocabulary");
      checkArgument(
          !conceptMapper.isDeprecated(concept.getParentKey()),
          "Cannot update a concept to a deprecated parent");
    }

    checkArgument(oldConcept.getDeprecated() == null, "Cannot update a deprecated entity");
    checkArgument(
        Objects.equals(oldConcept.getDeprecated(), concept.getDeprecated()),
        "Cannot deprecate or restore a deprecated concept while updating");
    checkArgument(Objects.equals(oldConcept.getDeprecatedBy(), concept.getDeprecatedBy()));
    checkArgument(Objects.equals(oldConcept.getReplacedByKey(), concept.getReplacedByKey()));

    checkArgument(oldConcept.getDeleted() == null, "Cannot update a deleted entity");
    checkArgument(
        Objects.equals(oldConcept.getDeleted(), concept.getDeleted()),
        "Cannot delete or restore an entity while updating");

    // update the concept
    conceptMapper.update(concept);
  }

  @Override
  public PagingResponse<Concept> list(
      @Nullable ConceptSearchParams params, @Nullable Pageable page) {
    page = page != null ? page : new PagingRequest();
    params = params != null ? params : ConceptSearchParams.empty();

    return new PagingResponse<>(
        page,
        conceptMapper.count(
            params.getQuery(),
            params.getVocabularyKey(),
            params.getParentKey(),
            params.getReplacedByKey(),
            params.getName(),
            params.getDeprecated()),
        conceptMapper.list(
            params.getQuery(),
            params.getVocabularyKey(),
            params.getParentKey(),
            params.getReplacedByKey(),
            params.getName(),
            params.getDeprecated(),
            page));
  }

  @Override
  public List<KeyNameResult> suggest(@NotNull String query, int vocabularyKey) {
    return conceptMapper.suggest(query, vocabularyKey);
  }

  @Transactional
  @Override
  public void deprecate(
      int key, @NotBlank String deprecatedBy, int replacementKey, boolean deprecateChildren) {

    checkArgument(
        conceptMapper.getVocabularyKey(key).equals(conceptMapper.getVocabularyKey(replacementKey)),
        "A concept and its replacement must belong to the same vocabulary");

    // find children
    List<Integer> children = findChildrenKeys(key, false);
    if (!children.isEmpty()) {
      if (deprecateChildren) {
        // deprecate children without replacement
        conceptMapper.deprecateInBulk(children, deprecatedBy, null);
      } else {
        // reassigning children to the replacement
        conceptMapper.updateParent(children, replacementKey);
      }
    }

    // deprecate concept
    conceptMapper.deprecate(key, deprecatedBy, replacementKey);
  }

  @Transactional
  @Override
  public void deprecate(int key, @NotBlank String deprecatedBy, boolean deprecateChildren) {
    List<Integer> children = findChildrenKeys(key, false);
    if (!children.isEmpty()) {
      if (!deprecateChildren) {
        throw new IllegalArgumentException(
            "A concept can be deprecated without replacement only if it has no children");
      }

      // deprecate children
      conceptMapper.deprecateInBulk(children, deprecatedBy, null);
    }

    conceptMapper.deprecate(key, deprecatedBy, null);
  }

  @Transactional
  @Override
  public void restoreDeprecated(int key, boolean restoreDeprecatedChildren) {
    // get the concept
    Concept concept = Objects.requireNonNull(conceptMapper.get(key));

    // check if the vocabulary is not deprecated
    if (vocabularyMapper.isDeprecated(concept.getVocabularyKey())) {
      throw new IllegalArgumentException("Cannot restore a concept whose vocabulary is deprecated");
    }

    // restore the concept
    conceptMapper.restoreDeprecated(key);

    // set the parent. If the parent is deprecated we look for its replacement
    Optional.ofNullable(concept.getParentKey())
        .ifPresent(p -> concept.setParentKey(conceptMapper.findReplacement(p)));

    if (restoreDeprecatedChildren) {
      conceptMapper.restoreDeprecatedInBulk(findChildrenKeys(key, true));
    }
  }

  /** Returns the keys of all the children of the given concept. */
  private List<Integer> findChildrenKeys(int parentKey, boolean deprecated) {
    return conceptMapper.list(null, null, parentKey, null, null, deprecated, null).stream()
        .map(Concept::getKey)
        .collect(Collectors.toList());
  }
}
