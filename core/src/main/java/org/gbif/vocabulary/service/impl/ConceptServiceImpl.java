package org.gbif.vocabulary.service.impl;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.service.ConceptService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

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
            params.getName()),
        conceptMapper.list(
            params.getQuery(),
            params.getVocabularyKey(),
            params.getParentKey(),
            params.getReplacedByKey(),
            params.getName(),
            page));
  }

  @Override
  public PagingResponse<Concept> listDeleted(Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(page, conceptMapper.countDeleted(), conceptMapper.deleted(page));
  }
}
