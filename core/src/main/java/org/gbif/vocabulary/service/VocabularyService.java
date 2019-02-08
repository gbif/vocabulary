package org.gbif.vocabulary.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.VocabularySearchParams;

/** Services for a {@link Vocabulary}. */
public interface VocabularyService extends BaseService<Vocabulary> {

  /**
   * Retrieves pages of {@link Vocabulary} that match with the {@link VocabularySearchParams} received.
   *
   * @param params to do the search.
   * @return a list of {@link Vocabulary} ordered by their creation date, newest coming first
   */
  PagingResponse<Vocabulary> list(VocabularySearchParams params);

  /**
   * Retrieves pages of {@link Vocabulary} that are deleted.
   *
   * @param page paging parameters
   * @return a list of deleted {@link Vocabulary}
   */
  PagingResponse<Vocabulary> listDeleted(Pageable page);
}
