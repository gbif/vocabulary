package org.gbif.vocabulary.service;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.persistence.mappers.BaseMapper;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.validation.ConstraintViolationException;

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
