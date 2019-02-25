package org.gbif.vocabulary.service;

import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.PostgresDBExtension;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = {ConceptServiceIT.ContexInitializer.class})
public class ConceptServiceIT {

  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  private final ConceptService conceptService;
  private final VocabularyService vocabularyService;

  private static int[] vocabularyKeys = new int[2];

  @Autowired
  ConceptServiceIT(ConceptService conceptService, VocabularyService vocabularyService) {
    this.conceptService = conceptService;
    this.vocabularyService = vocabularyService;
  }

  /**
   * Creates a default vocabulary to use it in the concepts, since the vocabularyKey of a concept
   * cannot be null.
   */
  @BeforeAll
  public static void populateData(@Autowired VocabularyMapper vocabularyMapper) {
    Vocabulary vocabulary1 = new Vocabulary();
    vocabulary1.setName("v1");
    vocabulary1.setCreatedBy("test");
    vocabulary1.setModifiedBy("test");
    vocabularyMapper.create(vocabulary1);
    vocabularyKeys[0] = vocabulary1.getKey();

    Vocabulary vocabulary2 = new Vocabulary();
    vocabulary2.setName("v2");
    vocabulary2.setCreatedBy("test");
    vocabulary2.setModifiedBy("test");
    vocabularyMapper.create(vocabulary2);
    vocabularyKeys[1] = vocabulary2.getKey();
  }

  @Test
  public void createTest() {
    Concept concept = createNewConcept("name", vocabularyKeys[0]);
    assertDoesNotThrow(() -> conceptService.create(concept));
  }

  @Test
  public void createWithIncorrectParentTest() {
    int parentKey = conceptService.create(createNewConcept("parent", vocabularyKeys[0]));

    Concept child = createNewConcept("child", vocabularyKeys[1]);
    child.setParentKey(parentKey);
    // concept and parent belong to different vocabularies
    assertThrows(IllegalArgumentException.class, () -> conceptService.create(child));
  }

  @Test
  public void createSimilarConceptTest() {
    Concept concept = createNewConcept("first", vocabularyKeys[0]);
    concept.setLabel(Collections.singletonMap(Language.ENGLISH, "label"));
    conceptService.create(concept);

    Concept similar = createNewConcept("label", vocabularyKeys[0]);
    assertThrows(IllegalArgumentException.class, () -> conceptService.create(similar));
  }

  @Test
  public void updateTest() {
    Concept concept = createNewConcept("concept", vocabularyKeys[0]);
    int key = conceptService.create(concept);
    concept = conceptService.get(key);

    // update concept
    concept.setLabel(Collections.singletonMap(Language.ENGLISH, "label"));
    concept.setMisspeltLabels(
        Collections.singletonMap(Language.ENGLISH, Arrays.asList("labl", "lbel")));
    concept.setParentKey(vocabularyKeys[1]);
    conceptService.update(concept);

    Concept updatedConcept = conceptService.get(key);
    assertEquals("label", updatedConcept.getLabel().get(Language.ENGLISH));
    assertEquals(2, updatedConcept.getMisspeltLabels().get(Language.ENGLISH).size());
    assertTrue(
        updatedConcept
            .getMisspeltLabels()
            .get(Language.ENGLISH)
            .containsAll(Arrays.asList("labl", "lbel")));
    assertEquals(vocabularyKeys[1], updatedConcept.getParentKey().intValue());
  }

  @Test
  public void invalidParentUpdateTest() {
    Concept concept = createNewConcept("concept v1", vocabularyKeys[0]);
    int key = conceptService.create(concept);

    // parent with different vocabulary
    int parentKey = conceptService.create(createNewConcept("concept v2", vocabularyKeys[1]));
    Concept createdConcept = conceptService.get(key);
    createdConcept.setParentKey(parentKey);
    assertThrows(IllegalArgumentException.class, () -> conceptService.update(createdConcept));
  }

  @Test
  public void deprecatingWhenUpdatingTest() {
    Concept concept = createNewConcept("deprecation candidate", vocabularyKeys[0]);
    int key = conceptService.create(concept);

    Concept createdConcept = conceptService.get(key);
    createdConcept.setReplacedByKey(2);
    assertThrows(IllegalArgumentException.class, () -> conceptService.update(createdConcept));
  }

  @Test
  public void deletingWhenUpdatingTest() {
    Concept concept = createNewConcept("deletion candidate", vocabularyKeys[0]);
    int key = conceptService.create(concept);

    Concept createdConcept = conceptService.get(key);
    createdConcept.setDeleted(LocalDateTime.now());
    assertThrows(IllegalArgumentException.class, () -> conceptService.update(createdConcept));
  }

  @Test
  public void listConceptsTest() {
    conceptService.create(createNewConcept("concept1", vocabularyKeys[0]));
    conceptService.create(createNewConcept("concept2", vocabularyKeys[1]));
    assertEquals(
        1,
        conceptService
            .list(ConceptSearchParams.builder().name("concept1").build(), null)
            .getCount()
            .longValue());
    assertEquals(
        0,
        conceptService
            .list(
                ConceptSearchParams.builder().name("concept1").parentKey(vocabularyKeys[1]).build(),
                null)
            .getCount()
            .longValue());
  }

  @Test
  public void deprecateWithReplacementTest() {
    int key1 = conceptService.create(createNewConcept("cr1", vocabularyKeys[0]));
    int key2 = conceptService.create(createNewConcept("cr2", vocabularyKeys[1]));

    // replacement and deprecated must belong to the same vocabulary
    assertThrows(
        IllegalArgumentException.class, () -> conceptService.deprecate(key1, "test", key2, false));

    Concept concept2 = conceptService.get(key2);
    concept2.setVocabularyKey(vocabularyKeys[0]);
    conceptService.update(concept2);

    assertDoesNotThrow(() -> conceptService.deprecate(key1, "test", key2, false));

    Concept deprecated = conceptService.get(key1);
    assertNotNull(deprecated.getDeprecated());
    assertEquals(key2, deprecated.getReplacedByKey().intValue());

    conceptService.restoreDeprecated(key1, false);
    Concept restored = conceptService.get(key1);
    assertNull(restored.getDeprecated());
    assertNull(restored.getReplacedByKey());

    // add children to the concept
    Concept child1 = createNewConcept("cr3", vocabularyKeys[0]);
    child1.setParentKey(key1);
    int key3 = conceptService.create(child1);
    Concept child2 = createNewConcept("cr4", vocabularyKeys[0]);
    child2.setParentKey(key1);
    int key4 = conceptService.create(child2);

    // deprecating children too
    conceptService.deprecate(key1, "test", key2, true);
    Concept child1Deprecated = conceptService.get(key3);
    assertNotNull(child1Deprecated.getDeprecated());
    assertNull(child1Deprecated.getReplacedByKey());
    Concept child2Deprecated = conceptService.get(key4);
    assertNotNull(child2Deprecated.getDeprecated());
    assertNull(child2Deprecated.getReplacedByKey());

    // restore concept and children
    conceptService.restoreDeprecated(key1, true);
    deprecated = conceptService.get(key1);
    assertNull(deprecated.getDeprecated());
    assertNull(deprecated.getReplacedByKey());
    child1Deprecated = conceptService.get(key3);
    assertNull(child1Deprecated.getDeprecated());
    assertNull(child1Deprecated.getReplacedByKey());
    child2Deprecated = conceptService.get(key4);
    assertNull(child2Deprecated.getDeprecated());
    assertNull(child2Deprecated.getReplacedByKey());

    // children not deprecated but reassigned to the replacement
    conceptService.deprecate(key1, "test", key2, false);
    deprecated = conceptService.get(key1);
    assertNotNull(deprecated.getDeprecated());
    assertEquals(key2, deprecated.getReplacedByKey().intValue());
    child1Deprecated = conceptService.get(key3);
    assertNull(child1Deprecated.getDeprecated());
    assertNull(child1Deprecated.getReplacedByKey());
    assertEquals(key2, child1Deprecated.getParentKey().intValue());
    child2Deprecated = conceptService.get(key4);
    assertNull(child2Deprecated.getDeprecated());
    assertNull(child2Deprecated.getReplacedByKey());
    assertEquals(key2, child2Deprecated.getParentKey().intValue());
  }

  @Test
  public void deprecateWithOutReplacementTest() {
    int key1 = conceptService.create(createNewConcept("cnr1", vocabularyKeys[0]));
    assertDoesNotThrow(() -> conceptService.deprecate(key1, "test", false));

    Concept deprecated = conceptService.get(key1);
    assertNotNull(deprecated.getDeprecated());
    assertNull(deprecated.getReplacedByKey());

    conceptService.restoreDeprecated(key1, false);
    Concept restored = conceptService.get(key1);
    assertNull(restored.getDeprecated());
    assertNull(restored.getReplacedByKey());

    // add children to the concept
    Concept child1 = createNewConcept("cnr3", vocabularyKeys[0]);
    child1.setParentKey(key1);
    int key3 = conceptService.create(child1);
    Concept child2 = createNewConcept("cnr4", vocabularyKeys[0]);
    child2.setParentKey(key1);
    int key4 = conceptService.create(child2);

    // deprecating without children is not allowed
    assertThrows(
        IllegalArgumentException.class, () -> conceptService.deprecate(key1, "test", false));

    // deprecating children too
    conceptService.deprecate(key1, "test", true);
    Concept child1Deprecated = conceptService.get(key3);
    assertNotNull(child1Deprecated.getDeprecated());
    assertNull(child1Deprecated.getReplacedByKey());
    Concept child2Deprecated = conceptService.get(key4);
    assertNotNull(child2Deprecated.getDeprecated());
    assertNull(child2Deprecated.getReplacedByKey());

    // restore concept and children
    conceptService.restoreDeprecated(key1, true);
    restored = conceptService.get(key1);
    assertNull(restored.getDeprecated());
    assertNull(restored.getReplacedByKey());
    Concept child1Restored = conceptService.get(key3);
    assertNull(child1Restored.getDeprecated());
    assertNull(child1Restored.getReplacedByKey());
    Concept child2Restored = conceptService.get(key4);
    assertNull(child2Restored.getDeprecated());
    assertNull(child2Restored.getReplacedByKey());
  }

  @Test
  public void restoreWithDeprecatedVocabularyTest() {
    // create vocabulary
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName("name");
    vocabulary.setCreatedBy("test");
    vocabulary.setModifiedBy("test");
    int vocabularyKey = vocabularyService.create(vocabulary);

    // create concept for that vocabulary
    int key1 = conceptService.create(createNewConcept("deprecable", vocabularyKey));
    // deprecate vocabulary and concept
    vocabularyService.deprecate(key1, "test", true);

    // TODO: wait for vocabulary tests
    // restore concept -> vocabulary cannot be deprecated
    assertThrows(
        IllegalArgumentException.class, () -> conceptService.restoreDeprecated(key1, false));

    // restore vocabulary
    vocabularyService.restoreDeprecated(vocabularyKey, false);

    // now we can restore the concept
    assertDoesNotThrow(() -> conceptService.restoreDeprecated(key1, false));
  }

  @Test
  public void restoreWithDeprecatedParentTest() {
    int root = conceptService.create(createNewConcept("root", vocabularyKeys[0]));
    int child1 = conceptService.create(createNewConcept("replacement1", vocabularyKeys[0]));
    int child2 = conceptService.create(createNewConcept("replacement2", vocabularyKeys[0]));

    Concept mainConcept = createNewConcept("main", vocabularyKeys[0]);
    mainConcept.setParentKey(child2);
    int mainConceptKey = conceptService.create(mainConcept);

    // deprecate parents and main concept
    conceptService.deprecate(child2, "test", child1, false);
    conceptService.deprecate(child1, "test", root, false);
    conceptService.deprecate(mainConceptKey, "test", root, false);

    // restore main concept
    conceptService.restoreDeprecated(mainConceptKey, false);

    // parent has to be updated to the root
    mainConcept = conceptService.get(mainConceptKey);
    assertEquals(root, mainConcept.getParentKey().intValue());
  }

  private Concept createNewConcept(String name, int vocabularyKey) {
    Concept concept = new Concept();
    concept.setName(name);
    concept.setVocabularyKey(vocabularyKey);
    concept.setCreatedBy("test");
    concept.setModifiedBy("test");
    return concept;
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
