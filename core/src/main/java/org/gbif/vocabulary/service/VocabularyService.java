package org.gbif.vocabulary.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.VocabularySearchParams;

import javax.annotation.Nullable;

/** Services for a {@link Vocabulary}. */
public interface VocabularyService extends BaseService<Vocabulary> {

  /**
   * Retrieves pages of {@link Vocabulary} that match with the {@link VocabularySearchParams}
   * received.
   *
   * @param params to do the search.
   * @param page paging parameters
   * @return a list of {@link Vocabulary} ordered by their creation date, newest coming first
   */
  PagingResponse<Vocabulary> list(@Nullable VocabularySearchParams params, @Nullable Pageable page);

  /**
   * Deletes a {@link Vocabulary}.
   *
   * <p>A vocabulary that has concepts associated cannot be deleted. If specified, this method will
   * delete all the concepts too.
   *
   * @param key key of the vocabulary to delete.
   */
  // TODO: let delete concepts too??
  void delete(int key);
}
