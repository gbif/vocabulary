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

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.persistence.mappers.BaseMapper;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;

import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the {@link ConceptService}. */
public class ConceptServiceTest extends VocabularyEntityServiceBaseTest<Concept> {

  @Autowired private ConceptService conceptService;
  @MockBean private ConceptMapper conceptMapper;
  @MockBean private VocabularyMapper vocabularyMapper;

  @Test
  public void invalidConceptTest() {
    Concept concept = new Concept();

    // required fields are null
    assertThrows(ConstraintViolationException.class, () -> conceptService.create(concept));

    // set name
    concept.setName("name");
    assertThrows(ConstraintViolationException.class, () -> conceptService.create(concept));

    // set required auditable fields
    concept.setVocabularyKey(TEST_KEY);
    mockCreateEntity(concept);
    assertDoesNotThrow(() -> conceptService.create(concept));
  }

  @Override
  Concept createNewEntity(String name) {
    Concept concept = new Concept();
    concept.setVocabularyKey(TEST_KEY);
    concept.setName(name);
    concept.setCreatedBy("test");
    concept.setModifiedBy("test");
    return concept;
  }

  @Override
  BaseMapper<Concept> getMapper() {
    return conceptMapper;
  }

  @Override
  BaseService<Concept> getService() {
    return conceptService;
  }
}
