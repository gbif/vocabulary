package org.gbif.vocabulary.service;

import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;

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

  @Test
  public void restoringDeletedWhenUpdatingTest() {
    Vocabulary vocabularyDB = createNewEntity("v1");
    vocabularyDB.setKey(TEST_KEY);
    vocabularyDB.setDeleted(LocalDateTime.now());
    Vocabulary updatedVocabulary = new Vocabulary();
    BeanUtils.copyProperties(vocabularyDB, updatedVocabulary);
    updatedVocabulary.setDeleted(null);

    // mock
    when(vocabularyMapper.get(TEST_KEY)).thenReturn(vocabularyDB);

    assertDoesNotThrow(() -> vocabularyService.update(updatedVocabulary));
  }

  @Test
  public void deleteWithConceptsTest() {
    when(conceptMapper.count(null, TEST_KEY, null, null, null, false)).thenReturn(1L);
    assertThrows(IllegalArgumentException.class, () -> vocabularyService.delete(TEST_KEY));
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
