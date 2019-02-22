package org.gbif.vocabulary.service;

import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.persistence.mappers.BaseMapper;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;

import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the {@link VocabularyService}. */
@Execution(ExecutionMode.SAME_THREAD)
public class VocabularyServiceTest extends BaseServiceTest<Vocabulary> {

  @Autowired private VocabularyService vocabularyService;
  @MockBean private VocabularyMapper vocabularyMapper;
  @MockBean private ConceptMapper conceptMapper;

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

  @Override
  BaseMapper<Vocabulary> getMapper() {
    return vocabularyMapper;
  }

  @Override
  BaseService<Vocabulary> getService() {
    return vocabularyService;
  }
}
