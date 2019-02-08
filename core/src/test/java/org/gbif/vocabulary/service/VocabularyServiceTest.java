package org.gbif.vocabulary.service;

import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.persistence.mapper.VocabularyMapper;

import org.springframework.beans.factory.annotation.Autowired;

/** Tests the {@link VocabularyService}. */
public class VocabularyServiceTest extends BaseServiceTest<Vocabulary> {

  private final VocabularyService vocabularyService;
  private final VocabularyMapper vocabularyMapper;

  @Autowired
  VocabularyServiceTest(VocabularyService vocabularyService, VocabularyMapper vocabularyMapper) {
    super(vocabularyService, vocabularyMapper);
    this.vocabularyService = vocabularyService;
    this.vocabularyMapper = vocabularyMapper;
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
