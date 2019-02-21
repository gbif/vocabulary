package org.gbif.vocabulary.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.search.ConceptSearchParams;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;

/** Services for a {@link Concept}. */
public interface ConceptService extends BaseService<Concept> {

  /**
   * Retrieves pages of {@link Concept} that match with the {@link ConceptSearchParams} received.
   *
   * @param params to do the search.
   * @param page paging parameters
   * @return a list of {@link Concept} ordered by their creation date, newest coming first
   */
  PagingResponse<Concept> list(@Nullable ConceptSearchParams params, @Nullable Pageable page);

  /**
   * Deprecates a concept with a replacement.
   *
   * @param key key of the concept to be deprecated
   * @param deprecatedBy name of the actor who deprecates the concept
   * @param replacementKey key of the replacement
   * @param deprecateChildren if true the children of the concept will be deprecated too
   */
  void deprecate(
      int key, @NotBlank String deprecatedBy, int replacementKey, boolean deprecateChildren);

  /**
   * Deprecates a concept without replacement.
   *
   * @param key key of the concept to be deprecated
   * @param deprecatedBy name of the actor who deprecates the concept
   * @param deprecateChildren if true the children of the concept will be deprecated too
   */
  void deprecate(int key, @NotBlank String deprecatedBy, boolean deprecateChildren);

  /**
   * Restores a deprecated concept.
   *
   * @param key key of the concept to undeprecate.
   * @param restoreDeprecatedChildren if true it restores the deprecated children of the concept
   */
  void restoreDeprecated(int key, boolean restoreDeprecatedChildren);
}
