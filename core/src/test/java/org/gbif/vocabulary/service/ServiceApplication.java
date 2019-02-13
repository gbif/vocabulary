package org.gbif.vocabulary.service;

import org.gbif.vocabulary.persistence.mapper.ConceptMapper;
import org.gbif.vocabulary.persistence.mapper.VocabularyMapper;

import javax.sql.DataSource;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootApplication
public class ServiceApplication {

  @TestConfiguration
  class MockConfiguration {

    @MockBean
    private DataSource dataSource;
    @MockBean private PlatformTransactionManager platformTransactionManager;

    @Bean
    VocabularyMapper vocabularyMapper() {
      return Mockito.mock(VocabularyMapper.class);
    }

    @Bean
    ConceptMapper conceptMapper() {
      return Mockito.mock(ConceptMapper.class);
    }

//    @Bean
//    public MethodValidationPostProcessor methodValidationPostProcessor() {
//      return new MethodValidationPostProcessor();
//    }
  }
}
