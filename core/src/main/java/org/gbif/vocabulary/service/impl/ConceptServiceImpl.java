package org.gbif.vocabulary.service.impl;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;
import org.gbif.vocabulary.service.ConceptService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
  private final VocabularyMapper vocabularyMapper;

  @Autowired
  public ConceptServiceImpl(ConceptMapper conceptMapper, VocabularyMapper vocabularyMapper) {
    super(conceptMapper);
    this.conceptMapper = conceptMapper;
    this.vocabularyMapper = vocabularyMapper;
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
  public void deprecate(int key, String deprecatedBy, boolean deprecateChildren) {
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
}
