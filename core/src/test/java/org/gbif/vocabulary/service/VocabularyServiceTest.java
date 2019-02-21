package org.gbif.vocabulary.service;

import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;

import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the {@link VocabularyService}. */
@Execution(ExecutionMode.SAME_THREAD)
public class VocabularyServiceTest extends BaseServiceTest<Vocabulary> {

  private final VocabularyService vocabularyService;
  private final VocabularyMapper vocabularyMapper;
  private final ConceptMapper conceptMapper;

  @Autowired
  VocabularyServiceTest(
      VocabularyService vocabularyService,
      VocabularyMapper vocabularyMapper,
      ConceptMapper conceptMapper) {
    super(vocabularyService, vocabularyMapper);
    this.vocabularyService = vocabularyService;
    this.vocabularyMapper = vocabularyMapper;
    this.conceptMapper = conceptMapper;
  }

  @Test
  public void invalidVocabularyTest() {
    Vocabulary vocabulary = new Vocabulary();

    // required fields are null
    assertThrows(ConstraintViolationException.class, () -> vocabularyService.create(vocabulary));

    // set name
    vocabulary.setName("name");
    assertThrows(ConstraintViolationException.class, () -> vocabularyService.create(vocabulary));

    // set required auditable fields
    vocabulary.setCreatedBy("test");
    vocabulary.setModifiedBy("test");
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
}
