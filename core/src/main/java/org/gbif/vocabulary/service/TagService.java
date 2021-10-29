/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
