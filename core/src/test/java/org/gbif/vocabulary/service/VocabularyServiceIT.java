package org.gbif.vocabulary.service;

import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.PostgresDBExtension;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.VocabularySearchParams;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.gbif.vocabulary.TestUtils.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the {@link VocabularyService}.
 *
 * <p>These tests are intended to run in parallel. This should be taken into account when adding new
 * tests since we're not cleaning the DB after each test and htis can interferred with other
 * tests.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = {VocabularyServiceIT.ContexInitializer.class})
@ActiveProfiles("test")
public class VocabularyServiceIT {

  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  private final VocabularyService vocabularyService;
  private final ConceptService conceptService;

  @Autowired
  VocabularyServiceIT(VocabularyService vocabularyService, ConceptService conceptService) {
    this.vocabularyService = vocabularyService;
    this.conceptService = conceptService;
  }

  @Test
  public void createTest() {
    Vocabulary vocabulary = createBasicVocabulary();
    assertDoesNotThrow(() -> vocabularyService.create(vocabulary));
  }

  @Test
  public void createSimilarVocabularyTest() {
    Vocabulary vocabulary = createBasicVocabulary();
    vocabulary.setLabel(Collections.singletonMap(Language.ENGLISH, "sim"));
    vocabularyService.create(vocabulary);

    Vocabulary similarName = createBasicVocabulary();
    similarName.setName("sim");
    assertThrows(IllegalArgumentException.class, () -> vocabularyService.create(similarName));

    Vocabulary similarLabel = createBasicVocabulary();
    similarLabel.getLabel().put(Language.ITALIAN, "sim");
    assertThrows(IllegalArgumentException.class, () -> vocabularyService.create(similarLabel));
  }

  @Test
  public void updateTest() {
    Vocabulary vocabulary = createBasicVocabulary();
    int key = vocabularyService.create(vocabulary);
    vocabulary = vocabularyService.get(key);

    // update concept
    vocabulary.setLabel(Collections.singletonMap(Language.ENGLISH, "label"));
    vocabulary.setEditorialNotes(Arrays.asList("note1", "note2"));
    vocabularyService.update(vocabulary);

    Vocabulary updatedVocabulary = vocabularyService.get(key);
    assertEquals("label", updatedVocabulary.getLabel().get(Language.ENGLISH));
    assertTrue(updatedVocabulary.getEditorialNotes().containsAll(Arrays.asList("note1", "note2")));
  }

  @Test
  public void updateSimilarVocabularyTest() {
    Vocabulary vocabulary1 = createBasicVocabulary();
    vocabulary1.setLabel(Collections.singletonMap(Language.ENGLISH, "simupdated"));
    vocabularyService.create(vocabulary1);

    Vocabulary vocabulary2 = createBasicVocabulary();
    int key2 = vocabularyService.create(vocabulary2);

    // update concept
    Vocabulary updated = vocabularyService.get(key2);
    updated.setLabel(Collections.singletonMap(Language.ENGLISH, "simupdated"));
    assertThrows(IllegalArgumentException.class, () -> vocabularyService.update(updated));

    Vocabulary updated2 = vocabularyService.get(key2);
    updated2.setLabel(Collections.singletonMap(Language.ENGLISH, vocabulary1.getName()));
    assertThrows(IllegalArgumentException.class, () -> vocabularyService.update(updated2));
  }

  @Test
  public void deprecatingWhenUpdatingTest() {
    Vocabulary vocabulary = createBasicVocabulary();
    int key = vocabularyService.create(vocabulary);

    Vocabulary createdVocabulary = vocabularyService.get(key);
    createdVocabulary.setReplacedByKey(2);
    assertThrows(IllegalArgumentException.class, () -> vocabularyService.update(createdVocabulary));
  }

  @Test
  public void deletingWhenUpdatingTest() {
    Vocabulary vocabulary = createBasicVocabulary();
    int key = vocabularyService.create(vocabulary);

    Vocabulary createdVocabulary = vocabularyService.get(key);
    createdVocabulary.setDeleted(LocalDateTime.now());
    assertThrows(IllegalArgumentException.class, () -> vocabularyService.update(createdVocabulary));
  }

  @Test
  public void listConceptsTest() {
    Vocabulary v1 = createBasicVocabulary();
    v1.setNamespace("n1");
    vocabularyService.create(v1);
    Vocabulary v2 = createBasicVocabulary();
    v2.setNamespace("n1");
    vocabularyService.create(v2);
    assertEquals(
        1,
        vocabularyService
            .list(VocabularySearchParams.builder().name(v1.getName()).build(), null)
            .getCount()
            .longValue());
    assertEquals(
        2,
        vocabularyService
            .list(VocabularySearchParams.builder().namespace("n1").build(), null)
            .getCount()
            .longValue());
  }

  @Test
  public void deprecateTest() {
    int v1Key = vocabularyService.create(createBasicVocabulary());

    vocabularyService.deprecateWithoutReplacement(v1Key, DEPRECATED_BY, false);
    assertDeprecated(vocabularyService.get(v1Key), DEPRECATED_BY);

    vocabularyService.restoreDeprecated(v1Key, false);
    assertNotDeprecated(vocabularyService.get(v1Key));

    // add concepts to the vocabulary
    int c1Key = conceptService.create(createBasicConcept(v1Key));
    int c2Key = conceptService.create(createBasicConcept(v1Key));

    // create a replacement
    int v2Key = vocabularyService.create(createBasicVocabulary());

    // deprecating ignoring concepts
    assertThrows(
        IllegalArgumentException.class,
        () -> vocabularyService.deprecate(v1Key, DEPRECATED_BY, v2Key, false));

    // deprecating concepts too
    vocabularyService.deprecate(v1Key, DEPRECATED_BY, v2Key, true);
    assertDeprecatedWithReplacement(vocabularyService.get(v1Key), DEPRECATED_BY, v2Key);
    assertDeprecated(conceptService.get(c1Key), DEPRECATED_BY);
    assertDeprecated(conceptService.get(c2Key), DEPRECATED_BY);

    // restore with concepts
    vocabularyService.restoreDeprecated(v1Key, true);
    assertNotDeprecated(vocabularyService.get(v1Key));
    assertNotDeprecated(conceptService.get(c1Key));
    assertNotDeprecated(conceptService.get(c2Key));
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
