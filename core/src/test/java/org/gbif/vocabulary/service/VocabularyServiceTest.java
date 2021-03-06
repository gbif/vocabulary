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

import org.gbif.vocabulary.model.UserRoles;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.persistence.mappers.BaseMapper;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import javax.validation.ConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the {@link VocabularyService}. */
public class VocabularyServiceTest extends VocabularyEntityServiceBaseTest<Vocabulary> {

  @Autowired private VocabularyService vocabularyService;

  @MockBean(name = "vocabularyMapper")
  private VocabularyMapper vocabularyMapper;

  @MockBean(name = "conceptMapper")
  private ConceptMapper conceptMapper;

  @WithMockUser(authorities = UserRoles.VOCABULARY_ADMIN)
  @Test
  public void createTest() {
    Vocabulary vocabulary = createNewEntity("name");

    // mock
    mockCreateEntity(vocabulary);

    getService().create(vocabulary);

    assertEquals(TEST_KEY, vocabulary.getKey().intValue());
  }

  @WithMockUser(authorities = UserRoles.VOCABULARY_ADMIN)
  @Test
  public void invalidVocabularyTest() {
    Vocabulary vocabulary = new Vocabulary();

    // required fields are null
    assertThrows(ConstraintViolationException.class, () -> vocabularyService.create(vocabulary));

    // set name
    vocabulary.setName("name");
    mockCreateEntity(vocabulary);
    assertDoesNotThrow(() -> vocabularyService.create(vocabulary));
  }

  @Override
  Vocabulary createNewEntity(String name) {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(name);
    vocabulary.setCreatedBy("test");
    vocabulary.setModifiedBy("test");
    return vocabulary;
  }

  @WithMockUser
  @Test
  public void unauthorizedDeprecateTest() {
    assertThrows(
        AccessDeniedException.class, () -> vocabularyService.deprecate(0, "test", 0L, false));
    assertThrows(
        AccessDeniedException.class,
        () -> vocabularyService.deprecateWithoutReplacement(0, "test", false));
  }

  @WithMockUser
  @Test
  public void unauthorizedRestoreDeprecateTest() {
    assertThrows(AccessDeniedException.class, () -> vocabularyService.restoreDeprecated(0, false));
  }

  @Override
  BaseMapper<Vocabulary> getMapper() {
    return vocabularyMapper;
  }

  @Override
  BaseService<Vocabulary> getService() {
    return vocabularyService;
  }
}
