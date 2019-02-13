package org.gbif.vocabulary.service;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;

import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the {@link ConceptService}. */
@Execution(ExecutionMode.SAME_THREAD)
public class ConceptServiceTest extends BaseServiceTest<Concept> {

  private final ConceptService conceptService;

  @Autowired
  ConceptServiceTest(ConceptService conceptService, ConceptMapper conceptMapper) {
    super(conceptService, conceptMapper);
    this.conceptService = conceptService;
  }

  @Test
  public void invalidVocabularyTest() {
    Concept concept = new Concept();

    // required fields are null
    assertThrows(ConstraintViolationException.class, () -> conceptService.create(concept));

    // set name
    concept.setName("name");
    assertThrows(ConstraintViolationException.class, () -> conceptService.create(concept));

    // set required auditable fields
    concept.setCreatedBy("test");
    concept.setModifiedBy("test");
    assertThrows(ConstraintViolationException.class, () -> conceptService.create(concept));

    // set vocabulary key
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
}
