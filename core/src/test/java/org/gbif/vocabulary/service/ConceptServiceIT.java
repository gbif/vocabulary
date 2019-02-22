package org.gbif.vocabulary.service;

import org.gbif.vocabulary.PostgresDBExtension;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = {ConceptServiceIT.ContexInitializer.class})
public class ConceptServiceIT {

  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  private final ConceptService conceptService;

  private static int defaultVocabularyKey;

  @Autowired
  ConceptServiceIT(ConceptService conceptService) {
    this.conceptService = conceptService;
  }

  /**
   * Creates a default vocabulary to use it in the concepts, since the vocabularyKey of a concept
   * cannot be null.
   */
    @BeforeAll
    public static void populateData(@Autowired VocabularyMapper vocabularyMapper) {
      Vocabulary vocabulary = new Vocabulary();
      vocabulary.setName("default");
      vocabulary.setCreatedBy("test");
      vocabulary.setModifiedBy("test");
      vocabularyMapper.create(vocabulary);
      defaultVocabularyKey = vocabulary.getKey();
    }

  @Test
  public void createTest() {
    Concept concept = new Concept();
    concept.setName("c1");
    concept.setVocabularyKey(defaultVocabularyKey);
    concept.setCreatedBy("test");
    concept.setModifiedBy("test");

    int key = conceptService.create(concept);
    Assertions.assertNotNull(key);
  }

  static class ContexInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
              "spring.datasource.url=" + database.getPostgresContainer().getJdbcUrl(),
              "spring.datasource.username=" + database.getPostgresContainer().getUsername(),
              "spring.datasource.password=" + database.getPostgresContainer().getPassword())
          .applyTo(configurableApplicationContext.getEnvironment());
    }
  }
}
