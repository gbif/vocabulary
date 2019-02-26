package org.gbif.vocabulary.service.impl;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;
import org.gbif.vocabulary.service.VocabularyService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static java.util.Objects.requireNonNull;

import static com.google.common.base.Preconditions.checkArgument;

/** Default implementation for {@link VocabularyService}. */
@Service
@Validated
public class DefaultVocabularyService extends AbstractBaseService<Vocabulary>
    implements VocabularyService {

  private final VocabularyMapper vocabularyMapper;
  private final ConceptMapper conceptMapper;

  @Autowired
  public DefaultVocabularyService(VocabularyMapper vocabularyMapper, ConceptMapper conceptMapper) {
    super(vocabularyMapper);
    this.vocabularyMapper = vocabularyMapper;
    this.conceptMapper = conceptMapper;
  }

  @Transactional
  @Override
  public int create(@NotNull @Valid Vocabulary vocabulary) {
    checkArgument(vocabulary.getKey() == null, "Can't create a vocabulary which already has a key");

    // checking if there is another similar vocabulary.
    checkSimilarities(vocabulary);

    vocabularyMapper.create(vocabulary);

    return vocabulary.getKey();
  }

  @Transactional
  @Override
  public void update(@NotNull @Valid Vocabulary vocabulary) {
    requireNonNull(vocabulary.getKey());

    Vocabulary oldVocabulary = vocabularyMapper.get(vocabulary.getKey());
    requireNonNull(oldVocabulary, "Couldn't find vocabulary with key: " + vocabulary.getKey());

    checkArgument(oldVocabulary.getDeprecated() == null, "Cannot update a deprecated vocabulary");
    checkArgument(
        Objects.equals(oldVocabulary.getDeprecated(), vocabulary.getDeprecated()),
        "Cannot deprecate or restore a deprecated vocabulary while updating");
    checkArgument(Objects.equals(oldVocabulary.getDeprecatedBy(), vocabulary.getDeprecatedBy()));
    checkArgument(Objects.equals(oldVocabulary.getReplacedByKey(), vocabulary.getReplacedByKey()));

    checkArgument(oldVocabulary.getDeleted() == null, "Cannot update a deleted vocabulary");
    checkArgument(
        Objects.equals(oldVocabulary.getDeleted(), vocabulary.getDeleted()),
        "Cannot delete or restore an vocabulary while updating");

    // update the vocabulary
    vocabularyMapper.update(vocabulary);
  }

  @Override
  public PagingResponse<Vocabulary> list(VocabularySearchParams params, Pageable page) {
    page = page != null ? page : new PagingRequest();
    params = params != null ? params : VocabularySearchParams.empty();

    return new PagingResponse<>(
        page,
        vocabularyMapper.count(
            params.getQuery(), params.getName(), params.getNamespace(), params.getDeprecated()),
        vocabularyMapper.list(
            params.getQuery(),
            params.getName(),
            params.getNamespace(),
            params.getDeprecated(),
            page));
  }

  @Override
  public void deprecate(
      int key, @NotBlank String deprecatedBy, int replacementKey, boolean deprecateConcepts) {
    deprecateVocabulary(key, deprecatedBy, replacementKey, deprecateConcepts);
  }

  @Override
  public void deprecate(int key, @NotBlank String deprecatedBy, boolean deprecateConcepts) {
    deprecateVocabulary(key, deprecatedBy, null, deprecateConcepts);
  }

  private void deprecateVocabulary(
      int key, String deprecatedBy, Integer replacementKey, boolean deprecateConcepts) {
    List<Integer> concepts = findConceptsKeys(key, false);
    if (!concepts.isEmpty()) {
      if (!deprecateConcepts) {
        throw new IllegalArgumentException(
            "A vocabulary that has concepts cannot be deprecated unless its concepts are deprecated too");
      }

      // deprecate concepts
      conceptMapper.deprecateInBulk(concepts, deprecatedBy, null);
    }

    vocabularyMapper.deprecate(key, deprecatedBy, replacementKey);
  }

  @Override
  public void restoreDeprecated(int key, boolean restoreDeprecatedConcepts) {
    vocabularyMapper.restoreDeprecated(key);

    if (restoreDeprecatedConcepts) {
      conceptMapper.restoreDeprecatedInBulk(findConceptsKeys(key, true));
    }
  }

  private List<Integer> findConceptsKeys(int vocabularyKey, boolean deprecated) {
    return conceptMapper.list(null, vocabularyKey, null, null, null, deprecated, null).stream()
        .map(Concept::getKey)
        .collect(Collectors.toList());
  }
}