package org.gbif.vocabulary.service.impl;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.persistence.mapper.VocabularyMapper;
import org.gbif.vocabulary.service.VocabularyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/** Default implementation for {@link VocabularyService}. */
@Service
@Validated
public class VocabularyServiceImpl extends AbstractBaseService<Vocabulary>
    implements VocabularyService {

  private final VocabularyMapper vocabularyMapper;

  @Autowired
  public VocabularyServiceImpl(VocabularyMapper vocabularyMapper) {
    super(vocabularyMapper);
    this.vocabularyMapper = vocabularyMapper;
  }

  @Override
  public PagingResponse<Vocabulary> list(VocabularySearchParams params, Pageable page) {
    page = page != null ? page : new PagingResponse<>();

    return new PagingResponse<>(
        page,
        vocabularyMapper.count(params.getQuery(), params.getName(), params.getNamespace()),
        vocabularyMapper.list(params.getQuery(), params.getName(), params.getNamespace(), page));
  }

  @Override
  public PagingResponse<Vocabulary> listDeleted(Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(
        page, vocabularyMapper.countDeleted(), vocabularyMapper.deleted(page));
  }
}
