package org.gbif.vocabulary.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;

import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** Services for a {@link Concept}. */
public interface ConceptService extends BaseService<Concept> {

  /**
   * Retrieves a concept by its name and its vocabulary name.
   *
   * @param name name of the concept
   * @param vocabularyName name of the vocabulary
   * @return concept
   */
  Concept getByNameAndVocabulary(@NotBlank String name, @NotBlank String vocabularyName);

  /**
   * Retrieves pages of {@link Concept} that match with the {@link ConceptSearchParams} received.
   *
   * @param params to do the search
   * @param page paging parameters
   * @return a list of {@link Concept} ordered by their creation date, newest coming first
   */
  PagingResponse<Concept> list(@Nullable ConceptSearchParams params, @Nullable Pageable page);

  /**
   * Returns suggestions for the given query. It only checks for matches in the name field of the
   * concept within the specified vocabulary.
   *
   * @param query suggestion
   * @param vocabularyKey key of the vocabulary
   * @return a list of up to 20 suggested concepts
   */
  List<KeyNameResult> suggest(@NotNull String query, int vocabularyKey);

  /**
   * Deprecates a concept with a replacement.
   *
   * @param key key of the concept to be deprecated
   * @param deprecatedBy name of the actor who deprecates the concept
   * @param replacementKey key of the replacement
   * @param deprecateChildren if true the children of the concept will be deprecated too
   */
  void deprecate(
      int key,
      @NotBlank String deprecatedBy,
      @Nullable Integer replacementKey,
      boolean deprecateChildren);

  /**
   * Deprecates a concept without replacement.
   *
   * @param key key of the concept to be deprecated
   * @param deprecatedBy name of the actor who deprecates the concept
   * @param deprecateChildren if true the children of the concept will be deprecated too
   */
  void deprecateWithoutReplacement(
      int key, @NotBlank String deprecatedBy, boolean deprecateChildren);

  /**
   * Restores a deprecated concept.
   *
   * @param key key of the concept to undeprecate.
   * @param restoreDeprecatedChildren if true it restores the deprecated children of the concept
   */
  void restoreDeprecated(int key, boolean restoreDeprecatedChildren);

}
