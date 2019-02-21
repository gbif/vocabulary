package org.gbif.vocabulary.service.impl;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;
import org.gbif.vocabulary.service.VocabularyService;

import javax.validation.constraints.NotBlank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

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
  public void deprecate(
    int key, @NotBlank String deprecatedBy, int replacementKey, boolean deprecateConcepts
  ) {
    // TODO
  }

  @Override
  public void deprecate(int key, @NotBlank String deprecatedBy, boolean deprecateConcepts) {
    // TODO
  }

  @Override
  public void restoreDeprecated(int key, boolean restoreDeprecatedConcepts) {
    // TODO
  }

  private boolean hasConcepts(int vocabularyKey) {
    return conceptMapper.count(null, vocabularyKey, null, null, null, false) > 0;
  }
}
