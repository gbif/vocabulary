/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.PostgresDBExtension;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.UserRoles;
import org.gbif.vocabulary.service.impl.DefaultTagService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.validation.ConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integration tests for the {@link ConceptService}. */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = {TagServiceIT.ContextInitializer.class})
@ActiveProfiles("test")
public class TagServiceIT {

  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  private final TagService tagService;

  @Autowired
  TagServiceIT(TagService tagService) {
    this.tagService = tagService;
  }

  @WithMockUser(authorities = UserRoles.VOCABULARY_ADMIN)
  @Test
  public void crudTest() {
    Tag tag = new Tag();
    tag.setName("birds");
    tag.setDescription("Test tag");

    int key = tagService.create(tag);

    Tag createdTag = tagService.get(key);
    assertEquals(DefaultTagService.DEFAULT_COLOR, createdTag.getColor());
    assertNotNull(createdTag.getCreated());
    assertNotNull(createdTag.getModified());

    Tag tagByName = tagService.getByName(tag.getName());
    assertTrue(tagByName.lenientEquals(createdTag));

    createdTag.setName("birds 2");
    tagService.update(createdTag);
    createdTag = tagService.get(key);
    assertEquals("birds 2", createdTag.getName());

    PagingResponse<Tag> tags = tagService.list(new PagingRequest(0, 5));
    assertEquals(1, tags.getResults().size());
    assertEquals(1, tags.getCount());

    tagService.delete(createdTag.getKey());
    tags = tagService.list(new PagingRequest(0, 5));
    assertEquals(0, tags.getResults().size());
  }

  @WithMockUser(authorities = UserRoles.VOCABULARY_ADMIN)
  @Test
  public void invalidColorTest() {
    Tag tag = new Tag();
    tag.setName("birds");
    tag.setDescription("Test tag");
    tag.setColor("dsgdsg");

    assertThrows(ConstraintViolationException.class, () -> tagService.create(tag));

    tag.setColor("#FF88GGG");
    assertThrows(ConstraintViolationException.class, () -> tagService.create(tag));

    tag.setColor("FF88GGG");
    assertThrows(ConstraintViolationException.class, () -> tagService.create(tag));
  }

  @WithMockUser(authorities = UserRoles.VOCABULARY_ADMIN)
  @Test
  public void duplicateTagTest() {
    Tag tag = new Tag();
    tag.setName("birds");
    tag.setDescription("Test tag");
    tagService.create(tag);

    Tag tag2 = new Tag();
    tag2.setName("birds");
    assertThrows(DuplicateKeyException.class, () -> tagService.create(tag2));
  }

  @WithMockUser
  @Test
  public void unauthCreateTagTest() {
    Tag tag = new Tag();
    tag.setName("unauth");
    assertThrows(AccessDeniedException.class, () -> tagService.create(tag));
  }

  @WithMockUser
  @Test
  public void unauthUpdateTagTest() {
    Tag tag = new Tag();
    tag.setName("unauth");
    assertThrows(AccessDeniedException.class, () -> tagService.update(tag));
  }

  @WithMockUser(authorities = UserRoles.VOCABULARY_EDITOR)
  @Test
  public void unauthDeleteTagTest() {
    assertThrows(AccessDeniedException.class, () -> tagService.delete(1));
  }

  static class ContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
              "spring.datasource.url=" + database.getPostgresContainer().getJdbcUrl(),
              "spring.datasource.username=" + database.getPostgresContainer().getUsername(),
              "spring.datasource.password=" + database.getPostgresContainer().getPassword())
          .applyTo(configurableApplicationContext.getEnvironment());
    }
  }
}
