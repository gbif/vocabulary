package org.gbif.vocabulary.service;

import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.persistence.mappers.BaseMapper;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;

import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the {@link VocabularyService}. */
public class VocabularyServiceTest extends VocabularyEntityServiceBaseTest<Vocabulary> {

  @Autowired private VocabularyService vocabularyService;
  @MockBean private VocabularyMapper vocabularyMapper;
  @MockBean private ConceptMapper conceptMapper;

  @Test
  public void createTest() {
    Vocabulary vocabulary = createNewEntity("name");

    // mock
    mockCreateEntity(vocabulary);

    getService().create(vocabulary);

    Assertions.assertEquals(TEST_KEY, vocabulary.getKey().intValue());
  }

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

  @Override
  BaseMapper<Vocabulary> getMapper() {
    return vocabularyMapper;
  }

  @Override
  BaseService<Vocabulary> getService() {
    return vocabularyService;
  }
}
