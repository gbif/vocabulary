package org.gbif.vocabulary.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Tag;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/** Services for a {@link Tag}. */
public interface TagService {

  /**
   * Persists the tag received.
   *
   * @param tag to create
   * @return key of the newly created tag
   */
  int create(@NotNull @Valid Tag tag);

  /**
   * Retrieves an tag by its key.
   *
   * @param key key of the tag to retrieve
   * @return {@link Tag}
   */
  Tag get(int key);

  /**
   * Retrieves an tag by its name.
   *
   * @param name name of the tag to retrieve
   * @return {@link Tag}
   */
  Tag getByName(String name);

  /**
   * Updates a tag.
   *
   * @param tag to be updated.
   */
  void update(@NotNull @Valid Tag tag);

  /**
   * Retrieves pages of {@link Tag}.
   *
   * @param page paging parameters
   * @return a list of {@link Tag} ordered by name.
   */
  PagingResponse<Tag> list(@Nullable Pageable page);

  /**
   * Deletes a tag.
   *
   * @param key of the tag to delete
   */
  void delete(@NotNull int key);
}
