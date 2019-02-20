package org.gbif.vocabulary.service.impl;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;
import org.gbif.vocabulary.service.VocabularyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static java.util.Objects.requireNonNull;

import static com.google.common.base.Preconditions.checkArgument;

/** Default implementation for {@link VocabularyService}. */
@Service
@Validated
public class VocabularyServiceImpl extends AbstractBaseService<Vocabulary>
    implements VocabularyService {

  private final VocabularyMapper vocabularyMapper;
  private final ConceptMapper conceptMapper;

  @Autowired
  public VocabularyServiceImpl(VocabularyMapper vocabularyMapper, ConceptMapper conceptMapper) {
    super(vocabularyMapper);
    this.vocabularyMapper = vocabularyMapper;
    this.conceptMapper = conceptMapper;
  }

  @Transactional
  @Override
  public void update(Vocabulary vocabulary) {
    requireNonNull(vocabulary.getKey());

    Vocabulary oldVocabulary = vocabularyMapper.get(vocabulary.getKey());
    requireNonNull(oldVocabulary, "Couldn't find entity with key: " + oldVocabulary.getKey());

    if (oldVocabulary.getDeleted() != null) {
      checkArgument(
          vocabulary.getDeleted() == null,
          "Unable to update a previously deleted vocabulary unless you clear the deletion timestamp");
    } else {
      checkArgument(vocabulary.getDeleted() == null, "Can't delete a vocabulary when updating");
    }

    vocabularyMapper.update(vocabulary);
  }

  @Override
  public PagingResponse<Vocabulary> list(VocabularySearchParams params, Pageable page) {
    page = page != null ? page : new PagingResponse<>();

    return new PagingResponse<>(
        page,
        vocabularyMapper.count(
            params.getQuery(), params.getName(), params.getNamespace(), params.getDeleted()),
        vocabularyMapper.list(
            params.getQuery(), params.getName(), params.getNamespace(), params.getDeleted(), page));
  }

  @Override
  public void delete(int key) {
    if (hasConcepts(key)) {
      throw new IllegalArgumentException("Cannot delete a vocabulary that has concepts");
    }

    vocabularyMapper.delete(key);
  }

  private boolean hasConcepts(int vocabularyKey) {
    return conceptMapper.count(null, vocabularyKey, null, null, null, false) > 0;
  }
}
