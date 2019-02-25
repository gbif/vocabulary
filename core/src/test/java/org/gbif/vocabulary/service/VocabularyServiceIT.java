package org.gbif.vocabulary.service;

import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.PostgresDBExtension;
import org.gbif.vocabulary.model.Concept;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = {VocabularyServiceIT.ContexInitializer.class})
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
    Vocabulary vocabulary = createNewVocabulary("name");
    assertDoesNotThrow(() -> vocabularyService.create(vocabulary));
  }

  @Test
  public void createSimilarConceptTest() {
    Vocabulary vocabulary = createNewVocabulary("first");
    vocabulary.setLabel(Collections.singletonMap(Language.ENGLISH, "label"));
    vocabularyService.create(vocabulary);

    Vocabulary similar = createNewVocabulary("label");
    assertThrows(IllegalArgumentException.class, () -> vocabularyService.create(similar));
  }

  @Test
  public void updateTest() {
    Vocabulary vocabulary = createNewVocabulary("vocab");
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
  public void deprecatingWhenUpdatingTest() {
    Vocabulary vocabulary = createNewVocabulary("deprecation candidate");
    int key = vocabularyService.create(vocabulary);

    Vocabulary createdVocabulary = vocabularyService.get(key);
    createdVocabulary.setReplacedByKey(2);
    assertThrows(IllegalArgumentException.class, () -> vocabularyService.update(createdVocabulary));
  }

  @Test
  public void deletingWhenUpdatingTest() {
    Vocabulary vocabulary = createNewVocabulary("deletion candidate");
    int key = vocabularyService.create(vocabulary);

    Vocabulary createdVocabulary = vocabularyService.get(key);
    createdVocabulary.setDeleted(LocalDateTime.now());
    assertThrows(IllegalArgumentException.class, () -> vocabularyService.update(createdVocabulary));
  }

  @Test
  public void listConceptsTest() {
    Vocabulary v1 = createNewVocabulary("vocab1");
    v1.setName("n1");
    vocabularyService.create(v1);
    Vocabulary v2 = createNewVocabulary("vocab2");
    v2.setName("n1");
    vocabularyService.create(v2);
    assertEquals(
        1,
        vocabularyService
            .list(VocabularySearchParams.builder().name("vocab1").build(), null)
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
    int v1Key = vocabularyService.create(createNewVocabulary("vd1"));

    vocabularyService.deprecate(v1Key, "test", false);
    Vocabulary deprecated = vocabularyService.get(v1Key);
    assertNotNull(deprecated.getDeprecated());
    assertNull(deprecated.getReplacedByKey());

    vocabularyService.restoreDeprecated(v1Key, false);
    Vocabulary restored = vocabularyService.get(v1Key);
    assertNull(restored.getDeprecated());
    assertNull(restored.getReplacedByKey());

    // add concepts to the vocabulary
    Concept c1 = new Concept();
    c1.setName("c1");
    c1.setVocabularyKey(v1Key);
    c1.setCreatedBy("test");
    c1.setModifiedBy("test");
    int c1Key = conceptService.create(c1);

    Concept c2 = new Concept();
    c2.setName("c2");
    c2.setVocabularyKey(v1Key);
    c2.setCreatedBy("test");
    c2.setModifiedBy("test");
    int c2Key = conceptService.create(c2);

    // create a replacement
    int v2Key = vocabularyService.create(createNewVocabulary("vd2"));

    // deprecating ignoring concepts
    assertThrows(
        IllegalArgumentException.class,
        () -> vocabularyService.deprecate(v1Key, "test", v2Key, false));

    // deprecating concepts too
    vocabularyService.deprecate(v1Key, "test", v2Key, true);
    deprecated = vocabularyService.get(v1Key);
    assertNotNull(deprecated.getDeprecated());
    assertEquals(v2Key, deprecated.getReplacedByKey().intValue());
    Concept c1Deprecated = conceptService.get(c1Key);
    assertNotNull(c1Deprecated.getDeprecated());
    assertNull(c1Deprecated.getReplacedByKey());
    Concept c2Deprecated = conceptService.get(c2Key);
    assertNotNull(c2Deprecated.getDeprecated());
    assertNull(c2Deprecated.getReplacedByKey());

    // restore with concepts
    vocabularyService.restoreDeprecated(v1Key, true);
    restored = vocabularyService.get(v1Key);
    assertNull(restored.getDeprecated());
    assertNull(restored.getReplacedByKey());
    Concept c1Restored = conceptService.get(c1Key);
    assertNull(c1Restored.getDeprecated());
    assertNull(c1Restored.getReplacedByKey());
    Concept c2Restored = conceptService.get(c2Key);
    assertNull(c2Restored.getDeprecated());
    assertNull(c2Restored.getReplacedByKey());
  }

  private Vocabulary createNewVocabulary(String name) {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(name);
    vocabulary.setCreatedBy("test");
    vocabulary.setModifiedBy("test");

    return vocabulary;
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
