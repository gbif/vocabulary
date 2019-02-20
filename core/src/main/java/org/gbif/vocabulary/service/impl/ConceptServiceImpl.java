package org.gbif.vocabulary.service.impl;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.service.ConceptService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static java.util.Objects.requireNonNull;

import static com.google.common.base.Preconditions.checkArgument;

/** Default implementation for {@link ConceptService}. */
@Service
@Validated
public class ConceptServiceImpl extends AbstractBaseService<Concept> implements ConceptService {

  private final ConceptMapper conceptMapper;

  @Autowired
  public ConceptServiceImpl(ConceptMapper conceptMapper) {
    super(conceptMapper);
    this.conceptMapper = conceptMapper;
  }

  @Override
  public PagingResponse<Concept> list(ConceptSearchParams params, Pageable page) {
    page = page != null ? page : new PagingResponse<>();

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

  /**
   * Updates a concept.
   *
   * <p>It takes into consideration the following cases:
   *
   * <ul>
   *   <li>When restoring a concept, if it has a parent replaced by another concept, we update it
   *       with the current replacement.
   *   <li>When restoring a concept, if it has a replacement replaced by another concept, we update
   *       it with the current replacement.
   *   <li>When replacing a concept, if it has children we reassign them to the new replacement.
   *   <li>When replacing a concept, if there are concepts that were replaced by this concept, we
   *       set their replacement to the new one.
   *       <ul/>
   *
   * @param concept new concept
   * @return concept updated.
   */
  @Transactional
  @Override
  public void update(Concept concept) {
    requireNonNull(concept.getKey());

    Concept oldConcept = conceptMapper.get(concept.getKey());
    requireNonNull(oldConcept, "Couldn't find entity with key: " + concept.getKey());

    // deffensive checks... the mapper won't update deprecation fields in the DB anyway
    checkArgument(Objects.equals(oldConcept.getDeprecated(), concept.getDeprecated()));
    checkArgument(Objects.equals(oldConcept.getDeprecatedBy(), concept.getDeprecatedBy()));
    checkArgument(Objects.equals(oldConcept.getReplacedByKey(), concept.getReplacedByKey()));

    // cannot update deleted concept
    checkArgument(oldConcept.getDeleted() == null, "Cannot update a deleted concept");
    // delete and restoring a concept here is not allowed
    checkArgument(
        Objects.equals(oldConcept.getDeleted(), concept.getDeleted()),
        "Cannot delete or restore concept while updating");

    // update the concept
    conceptMapper.update(concept);
  }

  @Transactional
  @Override
  public void deprecate(
      int key, String deprecatedBy, int replacementKey, boolean deprecateChildren) {
    // deprecate concept
    conceptMapper.deprecate(key, deprecatedBy, replacementKey);

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
  }

  @Transactional
  @Override
  public void deprecate(int key, String deprecatedBy) {
    if (hasChildren(key)) {
      throw new IllegalArgumentException(
          "A concept can be deprecated without replacement only if it has no children");
    }

    conceptMapper.deprecate(key, deprecatedBy, null);
  }

  @Transactional
  @Override
  public void restoreDeprecated(int key, boolean restoreDeprecatedChildren) {
    // restore the concept
    conceptMapper.restoreDeprecated(key);

    // get the concept
    Concept concept = conceptMapper.get(key);

    // set the parent. If the parent is deprecated we look for its replacement
    concept.setParentKey(conceptMapper.findReplacement(concept.getParentKey()));

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

  private boolean hasChildren(int parentKey) {
    return conceptMapper.count(null, null, parentKey, null, null, false) > 0;
  }
}
