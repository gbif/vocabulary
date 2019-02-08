package org.gbif.vocabulary.service;

import org.gbif.vocabulary.persistence.mapper.VocabularyMapper;
import org.gbif.vocabulary.service.impl.VocabularyServiceImpl;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring configuration to test the service layer using mocks in the persistence layer. */
@Configuration
class MockConfiguration {

  @Bean
  VocabularyMapper vocabularyMapper() {
    return Mockito.mock(VocabularyMapper.class);
  }

  @Bean
  VocabularyService vocabularyService() {
    return new VocabularyServiceImpl(vocabularyMapper());
  }
}
