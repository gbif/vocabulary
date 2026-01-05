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
package org.gbif.vocabulary.service.impl;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.UserRoles;
import org.gbif.vocabulary.persistence.mappers.TagMapper;
import org.gbif.vocabulary.service.TagService;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@Service
@Validated
public class DefaultTagService implements TagService {

  public static final String DEFAULT_COLOR = "#FFFFFF";

  private final TagMapper tagMapper;

  @Autowired
  public DefaultTagService(TagMapper tagMapper) {
    this.tagMapper = tagMapper;
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Transactional
  @Override
  public int create(@NotNull @Valid Tag tag) {
    checkArgument(tag.getKey() == null, "Can't create a tag which already has a key");

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    tag.setCreatedBy(authentication.getName());
    tag.setModifiedBy(authentication.getName());

    if (Strings.isNullOrEmpty(tag.getColor())) {
      tag.setColor(DEFAULT_COLOR);
    }

    tagMapper.create(tag);

    return tag.getKey();
  }

  @Override
  public Tag get(int key) {
    return tagMapper.get(key);
  }

  @Override
  public Tag getByName(String name) {
    return tagMapper.getByName(name);
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Transactional
  @Override
  public void update(@NotNull @Valid Tag tag) {
    requireNonNull(tag.getKey());

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    tag.setModifiedBy(authentication.getName());

    if (Strings.isNullOrEmpty(tag.getColor())) {
      tag.setColor(DEFAULT_COLOR);
    }

    tagMapper.update(tag);
  }

  @Override
  public PagingResponse<Tag> list(@Nullable Pageable page) {
    page = page != null ? page : new PagingRequest();

    return new PagingResponse<>(page, tagMapper.count(), tagMapper.list(page));
  }

  @Secured({UserRoles.VOCABULARY_ADMIN})
  @Override
  public void delete(@NotNull int key) {
    tagMapper.delete(key);
  }
}
