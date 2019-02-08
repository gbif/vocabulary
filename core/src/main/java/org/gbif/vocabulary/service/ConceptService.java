package org.gbif.vocabulary.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.search.ConceptSearchParams;

/** Services for a {@link Concept}. */
public interface ConceptService extends BaseService<Concept> {

  /**
   * Retrieves pages of {@link Concept} that match with the {@link ConceptSearchParams} received.
   *
   * @param params to do the search.
   * @return a list of {@link Concept} ordered by their creation date, newest coming first
   */
  PagingResponse<Concept> list(ConceptSearchParams params);

  /**
   * Retrieves pages of {@link Concept} that are deleted.
   *
   * @param page paging parameters
   * @return a list of deleted {@link Concept}
   */
  PagingResponse<Concept> listDeleted(Pageable page);
}
