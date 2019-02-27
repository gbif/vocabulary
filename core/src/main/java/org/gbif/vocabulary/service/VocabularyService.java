package org.gbif.vocabulary.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;

import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
   * Returns suggestions for the given query. It only checks for matches in the name field of the
   * vocabulary.
   *
   * @param query suggestion
   * @return a list of up to 20 suggested vocabularies
   */
  List<KeyNameResult> suggest(@NotNull String query);

  /**
   * Deprecates a vocabulary with a replacement.
   *
   * @param key key of the vocabulary to be deprecated
   * @param deprecatedBy name of the actor who deprecates the vocabulary
   * @param replacementKey key of the replacement
   * @param deprecateConcepts if true the concepts of the vocabulary will be deprecated too
   */
  void deprecate(
      int key, @NotBlank String deprecatedBy, int replacementKey, boolean deprecateConcepts);

  /**
   * Deprecates a concept without replacement.
   *
   * @param key key of the concept to be deprecated
   * @param deprecatedBy name of the actor who deprecates the concept
   * @param deprecateConcepts if true the concepts of the concept will be deprecated too
   */
  void deprecate(int key, @NotBlank String deprecatedBy, boolean deprecateConcepts);

  /**
   * Restores a deprecated concept.
   *
   * @param key key of the concept to undeprecate.
   * @param restoreDeprecatedConcepts if true it restores the deprecated concepts of the vocabulary
   */
  void restoreDeprecated(int key, boolean restoreDeprecatedConcepts);
}
