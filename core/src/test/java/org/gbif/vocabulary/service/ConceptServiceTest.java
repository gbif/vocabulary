package org.gbif.vocabulary.service;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;

import java.time.LocalDateTime;
import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/** Tests the {@link ConceptService}. */
@Execution(ExecutionMode.SAME_THREAD)
public class ConceptServiceTest extends BaseServiceTest<Concept> {

  private final ConceptService conceptService;
  private final ConceptMapper conceptMapper;

  @Autowired
  ConceptServiceTest(ConceptService conceptService, ConceptMapper conceptMapper) {
    super(conceptService, conceptMapper);
    this.conceptService = conceptService;
    this.conceptMapper = conceptMapper;
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

  @Test
  public void restoringDeletedWhenUpdatingTest() {
    Concept conceptDB = createNewEntity("c1");
    conceptDB.setKey(TEST_KEY);
    conceptDB.setDeleted(LocalDateTime.now());
    Concept updatedConcept = new Concept();
    BeanUtils.copyProperties(conceptDB, updatedConcept);
    updatedConcept.setDeleted(null);

    // mock
    when(conceptMapper.get(TEST_KEY)).thenReturn(conceptDB);

    assertThrows(IllegalArgumentException.class, () -> conceptService.update(updatedConcept));
  }

  @Test
  public void deprecatingWhenUpdatingTest() {
    Concept conceptDB = createNewEntity("c1");
    conceptDB.setKey(TEST_KEY);
    Concept updatedConcept = new Concept();
    BeanUtils.copyProperties(conceptDB, updatedConcept);
    updatedConcept.setReplacedByKey(2);

    // mock
    when(conceptMapper.get(TEST_KEY)).thenReturn(conceptDB);

    assertThrows(IllegalArgumentException.class, () -> conceptService.update(updatedConcept));
  }

  @Test
  public void restoringDeprecatedWhenUpdatingTest() {
    Concept conceptDB = createNewEntity("c1");
    conceptDB.setKey(TEST_KEY);
    conceptDB.setDeprecated(LocalDateTime.now());
    conceptDB.setDeprecatedBy("test");
    Concept updatedConcept = new Concept();
    BeanUtils.copyProperties(conceptDB, updatedConcept);
    updatedConcept.setDeprecated(null);

    // mock
    when(conceptMapper.get(TEST_KEY)).thenReturn(conceptDB);

    assertThrows(IllegalArgumentException.class, () -> conceptService.update(updatedConcept));
  }

  @Test
  public void deprecateWithoutReplacementButWithChildrenTest() {
    when(conceptMapper.count(null, null, TEST_KEY, null, null, false)).thenReturn(1L);
    assertThrows(IllegalArgumentException.class, () -> conceptService.deprecate(TEST_KEY, "test"));
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
