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

import static org.gbif.vocabulary.TestUtils.DEPRECATED_BY;
import static org.gbif.vocabulary.TestUtils.assertDeprecated;
import static org.gbif.vocabulary.TestUtils.assertDeprecatedWithReplacement;
import static org.gbif.vocabulary.TestUtils.assertNotDeprecated;
import static org.gbif.vocabulary.TestUtils.createBasicConcept;
import static org.gbif.vocabulary.TestUtils.createBasicVocabulary;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    Vocabulary vocabulary1 = createBasicVocabulary();
    vocabularyMapper.create(vocabulary1);
    vocabularyKeys[0] = vocabulary1.getKey();

    Vocabulary vocabulary2 = createBasicVocabulary();
    vocabularyMapper.create(vocabulary2);
    vocabularyKeys[1] = vocabulary2.getKey();
  }

  @Test
  public void createTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    assertDoesNotThrow(() -> conceptService.create(concept));
  }

  @Test
  public void createWithIncorrectParentTest() {
    int parentKey = conceptService.create(createBasicConcept(vocabularyKeys[0]));

    Concept child = createBasicConcept(vocabularyKeys[1]);
    child.setParentKey(parentKey);
    // concept and parent belong to different vocabularies
    assertThrows(IllegalArgumentException.class, () -> conceptService.create(child));
  }

  @Test
  public void createWithDeprecatedParentTest() {
    int parentKey = conceptService.create(createBasicConcept(vocabularyKeys[0]));
    conceptService.deprecate(parentKey, DEPRECATED_BY, false);

    Concept child = createBasicConcept(vocabularyKeys[0]);
    child.setParentKey(parentKey);
    // parent cannot be deprecated
    assertThrows(IllegalArgumentException.class, () -> conceptService.create(child));
  }

  @Test
  public void createWithDeprecatedVocabularyTest() {
    Vocabulary vocabulary = createBasicVocabulary();
    int vKey = vocabularyService.create(vocabulary);
    vocabularyService.deprecate(vKey, DEPRECATED_BY, false);

    // vocabulary cannot be deprecated
    assertThrows(
        IllegalArgumentException.class, () -> conceptService.create(createBasicConcept(vKey)));
  }

  @Test
  public void createSimilarConceptTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    concept.setLabel(Collections.singletonMap(Language.ENGLISH, "label"));
    conceptService.create(concept);

    Concept similar = createBasicConcept(vocabularyKeys[0]);
    similar.setName(concept.getName());
    assertThrows(IllegalArgumentException.class, () -> conceptService.create(similar));

    similar.setName("label");
    assertThrows(IllegalArgumentException.class, () -> conceptService.create(similar));

    similar.setVocabularyKey(vocabularyKeys[1]);
    assertDoesNotThrow(() -> conceptService.create(similar));
  }

  @Test
  public void updateTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
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
  public void updateParentFromOtherVocabularyTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    int key = conceptService.create(concept);

    // parent with different vocabulary
    int parentKey = conceptService.create(createBasicConcept(vocabularyKeys[1]));
    Concept createdConcept = conceptService.get(key);
    createdConcept.setParentKey(parentKey);
    assertThrows(IllegalArgumentException.class, () -> conceptService.update(createdConcept));
  }

  @Test
  public void updateVocabularyDeprecatedTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    int key = conceptService.create(concept);

    Vocabulary vocabulary = createBasicVocabulary();
    int vDeprecatedKey = vocabularyService.create(vocabulary);
    vocabularyService.deprecate(vDeprecatedKey, DEPRECATED_BY, false);

    Concept createdConcept = conceptService.get(key);
    createdConcept.setVocabularyKey(vDeprecatedKey);
    // vocabulary cannot be deprecated
    assertThrows(IllegalArgumentException.class, () -> conceptService.update(createdConcept));
  }

  @Test
  public void updateParentDeprecatedTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    int key = conceptService.create(concept);

    // parent with different vocabulary
    Concept deprecated = createBasicConcept(vocabularyKeys[0]);
    int deprecatedKey = conceptService.create(deprecated);
    conceptService.deprecate(deprecatedKey, DEPRECATED_BY, false);

    Concept createdConcept = conceptService.get(key);
    createdConcept.setParentKey(deprecatedKey);
    // parent cannot be deprecated
    assertThrows(IllegalArgumentException.class, () -> conceptService.update(createdConcept));
  }

  @Test
  public void deprecatingWhenUpdatingTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    int key = conceptService.create(concept);

    Concept createdConcept = conceptService.get(key);
    createdConcept.setReplacedByKey(2);
    assertThrows(IllegalArgumentException.class, () -> conceptService.update(createdConcept));
  }

  @Test
  public void deletingWhenUpdatingTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    int key = conceptService.create(concept);

    Concept createdConcept = conceptService.get(key);
    createdConcept.setDeleted(LocalDateTime.now());
    assertThrows(IllegalArgumentException.class, () -> conceptService.update(createdConcept));
  }

  @Test
  public void listConceptsTest() {
    Concept c1 = createBasicConcept(vocabularyKeys[0]);
    conceptService.create(c1);
    Concept c2 = createBasicConcept(vocabularyKeys[1]);
    conceptService.create(c2);
    assertEquals(
        1,
        conceptService
            .list(ConceptSearchParams.builder().name(c1.getName()).build(), null)
            .getCount()
            .longValue());
    assertEquals(
        0,
        conceptService
            .list(
                ConceptSearchParams.builder()
                    .name(c1.getName())
                    .parentKey(vocabularyKeys[1])
                    .build(),
                null)
            .getCount()
            .longValue());
  }

  @Test
  public void deprecateWithReplacementFromOtherVocabularyTest() {
    int c1 = conceptService.create(createBasicConcept(vocabularyKeys[0]));
    int c2 = conceptService.create(createBasicConcept(vocabularyKeys[1]));

    // replacement and deprecated must belong to the same vocabulary
    assertThrows(
        IllegalArgumentException.class,
        () -> conceptService.deprecate(c1, DEPRECATED_BY, c2, false));

    Concept concept2 = conceptService.get(c2);
    concept2.setVocabularyKey(vocabularyKeys[0]);
    conceptService.update(concept2);

    assertDoesNotThrow(() -> conceptService.deprecate(c1, DEPRECATED_BY, c2, false));
  }

  @Test
  public void deprecateWithReplacementTest() {
    int key1 = conceptService.create(createBasicConcept(vocabularyKeys[0]));
    int key2 = conceptService.create(createBasicConcept(vocabularyKeys[0]));

    assertDoesNotThrow(() -> conceptService.deprecate(key1, DEPRECATED_BY, key2, false));
    assertDeprecatedWithReplacement(conceptService.get(key1), DEPRECATED_BY, key2);

    conceptService.restoreDeprecated(key1, false);
    assertNotDeprecated(conceptService.get(key1));

    // add children to the concept
    Concept child1 = createBasicConcept(vocabularyKeys[0]);
    child1.setParentKey(key1);
    int key3 = conceptService.create(child1);
    Concept child2 = createBasicConcept(vocabularyKeys[0]);
    child2.setParentKey(key1);
    int key4 = conceptService.create(child2);

    // deprecating children too
    conceptService.deprecate(key1, DEPRECATED_BY, key2, true);
    assertDeprecated(conceptService.get(key3), DEPRECATED_BY);
    assertDeprecated(conceptService.get(key4), DEPRECATED_BY);

    // restore concept and children
    conceptService.restoreDeprecated(key1, true);
    assertNotDeprecated(conceptService.get(key1));
    assertNotDeprecated(conceptService.get(key3));
    assertNotDeprecated(conceptService.get(key4));

    // children not deprecated but reassigned to the replacement
    conceptService.deprecate(key1, DEPRECATED_BY, key2, false);
    assertDeprecatedWithReplacement(conceptService.get(key1), DEPRECATED_BY, key2);
    Concept deprecatedChild3 = conceptService.get(key3);
    assertDeprecated(conceptService.get(key3), DEPRECATED_BY);
    assertEquals(key2, deprecatedChild3.getParentKey().intValue());
    Concept deprecatedChild4 = conceptService.get(key4);
    assertDeprecated(conceptService.get(key4), DEPRECATED_BY);
    assertEquals(key2, deprecatedChild4.getParentKey().intValue());
  }

  @Test
  public void deprecateWithoutReplacementTest() {
    int key1 = conceptService.create(createBasicConcept(vocabularyKeys[0]));
    assertDoesNotThrow(() -> conceptService.deprecate(key1, DEPRECATED_BY, false));
    assertDeprecated(conceptService.get(key1), DEPRECATED_BY);

    conceptService.restoreDeprecated(key1, false);
    assertNotDeprecated(conceptService.get(key1));

    // add children to the concept
    Concept child1 = createBasicConcept(vocabularyKeys[0]);
    child1.setParentKey(key1);
    int key2 = conceptService.create(child1);
    Concept child2 = createBasicConcept(vocabularyKeys[0]);
    child2.setParentKey(key1);
    int key3 = conceptService.create(child2);

    // deprecating without children is not allowed
    assertThrows(
        IllegalArgumentException.class, () -> conceptService.deprecate(key1, DEPRECATED_BY, false));

    // deprecating children too
    conceptService.deprecate(key1, DEPRECATED_BY, true);
    assertDeprecated(conceptService.get(key2), DEPRECATED_BY);
    assertDeprecated(conceptService.get(key3), DEPRECATED_BY);

    // restore concept and children
    conceptService.restoreDeprecated(key1, true);
    assertNotDeprecated(conceptService.get(key1));
    assertNotDeprecated(conceptService.get(key2));
    assertNotDeprecated(conceptService.get(key3));
  }

  @Test
  public void restoreWithDeprecatedVocabularyTest() {
    // create vocabulary
    Vocabulary vocabulary = createBasicVocabulary();
    int vocabularyKey = vocabularyService.create(vocabulary);

    // create concept for that vocabulary
    int key1 = conceptService.create(createBasicConcept(vocabularyKey));
    // deprecate vocabulary and concept
    vocabularyService.deprecate(vocabularyKey, DEPRECATED_BY, true);

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
    int root = conceptService.create(createBasicConcept(vocabularyKeys[0]));
    int child1 = conceptService.create(createBasicConcept(vocabularyKeys[0]));
    int child2 = conceptService.create(createBasicConcept(vocabularyKeys[0]));

    Concept mainConcept = createBasicConcept(vocabularyKeys[0]);
    mainConcept.setParentKey(child2);
    int mainConceptKey = conceptService.create(mainConcept);

    // deprecate parents and main concept
    conceptService.deprecate(child2, DEPRECATED_BY, child1, false);
    conceptService.deprecate(child1, DEPRECATED_BY, root, false);
    conceptService.deprecate(mainConceptKey, DEPRECATED_BY, root, false);

    // restore main concept
    conceptService.restoreDeprecated(mainConceptKey, false);

    // parent has to be updated to the root
    mainConcept = conceptService.get(mainConceptKey);
    assertEquals(root, mainConcept.getParentKey().intValue());
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
