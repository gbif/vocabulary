/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.vocabulary.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.PostgresDBExtension;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.UserRoles;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;

import static org.gbif.vocabulary.TestUtils.DEPRECATED_BY;
import static org.gbif.vocabulary.TestUtils.assertDeprecated;
import static org.gbif.vocabulary.TestUtils.assertDeprecatedWithReplacement;
import static org.gbif.vocabulary.TestUtils.assertNotDeprecated;
import static org.gbif.vocabulary.TestUtils.createBasicConcept;
import static org.gbif.vocabulary.TestUtils.createBasicVocabulary;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integration tests for the {@link ConceptService}. */
@WithMockUser(authorities = UserRoles.VOCABULARY_ADMIN)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = {ConceptServiceIT.ContexInitializer.class})
@ActiveProfiles("test")
public class ConceptServiceIT {

  // TODO: labels tests

  private static final String CLEAN_DB_SCRIPT = "/clean-concepts.sql";

  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  @Autowired private DataSource dataSource;

  private final ConceptService conceptService;
  private final VocabularyService vocabularyService;
  private final TagService tagService;

  private static final long[] vocabularyKeys = new long[2];

  @Autowired
  ConceptServiceIT(
      ConceptService conceptService, VocabularyService vocabularyService, TagService tagService) {
    this.conceptService = conceptService;
    this.vocabularyService = vocabularyService;
    this.tagService = tagService;
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

  @BeforeEach
  public void cleanDB() throws SQLException {
    Connection connection = dataSource.getConnection();
    ScriptUtils.executeSqlScript(connection, new ClassPathResource(CLEAN_DB_SCRIPT));
    connection.close();
  }

  @Test
  public void createTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    assertDoesNotThrow(() -> conceptService.create(concept));
  }

  @Test
  public void createWithIncorrectParentTest() {
    long parentKey = conceptService.create(createBasicConcept(vocabularyKeys[0]));

    Concept child = createBasicConcept(vocabularyKeys[1]);
    child.setParentKey(parentKey);
    // concept and parent belong to different vocabularies
    assertThrows(IllegalArgumentException.class, () -> conceptService.create(child));
  }

  @Test
  public void createWithDeprecatedParentTest() {
    long parentKey = conceptService.create(createBasicConcept(vocabularyKeys[0]));
    conceptService.deprecateWithoutReplacement(parentKey, DEPRECATED_BY, false);

    Concept child = createBasicConcept(vocabularyKeys[0]);
    child.setParentKey(parentKey);
    // parent cannot be deprecated
    assertThrows(IllegalArgumentException.class, () -> conceptService.create(child));
  }

  @Test
  public void createWithDeprecatedVocabularyTest() {
    Vocabulary vocabulary = createBasicVocabulary();
    long vKey = vocabularyService.create(vocabulary);
    vocabularyService.deprecateWithoutReplacement(vKey, DEPRECATED_BY, false);

    // vocabulary cannot be deprecated
    assertThrows(
        IllegalArgumentException.class, () -> conceptService.create(createBasicConcept(vKey)));
  }

  @Test
  public void createSimilarConceptTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    conceptService.create(concept);

    conceptService.addLabel(
        Label.builder()
            .entityKey(concept.getKey())
            .language(LanguageRegion.ENGLISH)
            .value("sim1")
            .build());
    conceptService.addHiddenLabel(
        HiddenLabel.builder().entityKey(concept.getKey()).value("simm1").build());

    Concept similar = createBasicConcept(vocabularyKeys[0]);
    similar.setName(concept.getName());
    assertThrows(IllegalArgumentException.class, () -> conceptService.create(similar));

    similar.setName("Sim1");
    assertThrows(IllegalArgumentException.class, () -> conceptService.create(similar));

    similar.setVocabularyKey(vocabularyKeys[1]);
    assertDoesNotThrow(() -> conceptService.create(similar));

    Concept similar2 = createBasicConcept(vocabularyKeys[0]);
    conceptService.create(similar2);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            conceptService.addHiddenLabel(
                HiddenLabel.builder().entityKey(similar2.getKey()).value("simm1").build()));

    Concept similar3 = createBasicConcept(vocabularyKeys[0]);
    conceptService.create(similar3);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            conceptService.addHiddenLabel(
                HiddenLabel.builder().entityKey(similar3.getKey()).value("simm2").build()));

    Concept similar4 = createBasicConcept(vocabularyKeys[0]);
    conceptService.create(similar4);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            conceptService.addAlternativeLabel(
                Label.builder()
                    .entityKey(similar4.getKey())
                    .language(LanguageRegion.SPANISH)
                    .value("simm2")
                    .build()));
  }

  @Test
  public void updateTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    long key = conceptService.create(concept);
    concept = conceptService.get(key);

    // update concept
    concept.getEditorialNotes().add("note1");

    Concept parent = createBasicConcept(vocabularyKeys[0]);
    long parentKey = conceptService.create(parent);
    concept.setParentKey(parentKey);
    conceptService.update(concept);

    Concept updatedConcept = conceptService.get(key);
    assertTrue(updatedConcept.getEditorialNotes().contains("note1"));
    assertEquals(parentKey, updatedConcept.getParentKey().intValue());
  }

  @Test
  public void updateSimilarLabelsTest() {
    Concept concept1 = createBasicConcept(vocabularyKeys[0]);
    concept1.setName("SimConcept");
    conceptService.create(concept1);
    conceptService.addAlternativeLabel(
        Label.builder()
            .entityKey(concept1.getKey())
            .language(LanguageRegion.ENGLISH)
            .value("simupdated")
            .build());
    conceptService.addHiddenLabel(
        HiddenLabel.builder().entityKey(concept1.getKey()).value("hidden1").build());

    Concept concept2 = createBasicConcept(vocabularyKeys[0]);
    long key2 = conceptService.create(concept2);

    // update concept
    assertThrows(
        IllegalArgumentException.class,
        () ->
            conceptService.addLabel(
                Label.builder()
                    .entityKey(concept2.getKey())
                    .language(LanguageRegion.ENGLISH)
                    .value("simupdated")
                    .build()));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            conceptService.addLabel(
                Label.builder()
                    .entityKey(concept2.getKey())
                    .language(LanguageRegion.ENGLISH)
                    .value(concept1.getName())
                    .build()));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            conceptService.addAlternativeLabel(
                Label.builder()
                    .entityKey(concept2.getKey())
                    .language(LanguageRegion.SPANISH)
                    .value("simupdated")
                    .build()));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            conceptService.addHiddenLabel(
                HiddenLabel.builder().entityKey(concept2.getKey()).value("simupdated").build()));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            conceptService.addHiddenLabel(
                HiddenLabel.builder().entityKey(concept2.getKey()).value("hidden1").build()));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            conceptService.addAlternativeLabel(
                Label.builder()
                    .entityKey(concept2.getKey())
                    .language(LanguageRegion.ENGLISH)
                    .value("hidden1")
                    .build()));
  }

  @Test
  public void updateParentFromOtherVocabularyTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    long key = conceptService.create(concept);

    // parent with different vocabulary
    long parentKey = conceptService.create(createBasicConcept(vocabularyKeys[1]));
    Concept createdConcept = conceptService.get(key);
    createdConcept.setParentKey(parentKey);
    assertThrows(IllegalArgumentException.class, () -> conceptService.update(createdConcept));
  }

  @Test
  public void updateVocabularyDeprecatedTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    long key = conceptService.create(concept);

    Vocabulary vocabulary = createBasicVocabulary();
    long vDeprecatedKey = vocabularyService.create(vocabulary);
    vocabularyService.deprecateWithoutReplacement(vDeprecatedKey, DEPRECATED_BY, false);

    Concept createdConcept = conceptService.get(key);
    createdConcept.setVocabularyKey(vDeprecatedKey);
    // vocabulary cannot be deprecated
    assertThrows(IllegalArgumentException.class, () -> conceptService.update(createdConcept));
  }

  @Test
  public void updateParentDeprecatedTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    long key = conceptService.create(concept);

    // parent with different vocabulary
    Concept deprecated = createBasicConcept(vocabularyKeys[0]);
    long deprecatedKey = conceptService.create(deprecated);
    conceptService.deprecateWithoutReplacement(deprecatedKey, DEPRECATED_BY, false);

    Concept createdConcept = conceptService.get(key);
    createdConcept.setParentKey(deprecatedKey);
    // parent cannot be deprecated
    assertThrows(IllegalArgumentException.class, () -> conceptService.update(createdConcept));
  }

  @Test
  public void removeParentInUpdateTest() {
    long key1 = conceptService.create(createBasicConcept(vocabularyKeys[0]));
    Concept conceptWithParent = createBasicConcept(vocabularyKeys[0]);
    conceptWithParent.setParentKey(key1);
    long keyWithParent = conceptService.create(conceptWithParent);

    Concept createdConceptWithParent = conceptService.get(keyWithParent);
    assertNotNull(createdConceptWithParent.getParentKey());
    createdConceptWithParent.setParentKey(null);
    conceptService.update(createdConceptWithParent);

    Concept updatedConcept = conceptService.get(keyWithParent);
    assertNull(updatedConcept.getParentKey());
  }

  @Test
  public void deprecatingWhenUpdatingTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    long key = conceptService.create(concept);

    Concept createdConcept = conceptService.get(key);
    createdConcept.setReplacedByKey(2L);
    assertThrows(IllegalArgumentException.class, () -> conceptService.update(createdConcept));
  }

  @Test
  public void deletingWhenUpdatingTest() {
    Concept concept = createBasicConcept(vocabularyKeys[0]);
    long key = conceptService.create(concept);

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
    long c1 = conceptService.create(createBasicConcept(vocabularyKeys[0]));
    long c2 = conceptService.create(createBasicConcept(vocabularyKeys[1]));

    // replacement and deprecated must belong to the same vocabulary
    assertThrows(
        IllegalArgumentException.class,
        () -> conceptService.deprecate(c1, DEPRECATED_BY, c2, false));
  }

  @Test
  public void deprecateWithReplacementTest() {
    long key1 = conceptService.create(createBasicConcept(vocabularyKeys[0]));
    long key2 = conceptService.create(createBasicConcept(vocabularyKeys[0]));

    assertDoesNotThrow(() -> conceptService.deprecate(key1, DEPRECATED_BY, key2, false));
    assertDeprecatedWithReplacement(conceptService.get(key1), DEPRECATED_BY, key2);

    conceptService.restoreDeprecated(key1, false);
    assertNotDeprecated(conceptService.get(key1));

    // add children to the concept
    Concept child1 = createBasicConcept(vocabularyKeys[0]);
    child1.setParentKey(key1);
    long key3 = conceptService.create(child1);
    Concept child2 = createBasicConcept(vocabularyKeys[0]);
    child2.setParentKey(key1);
    long key4 = conceptService.create(child2);

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
    assertNotDeprecated(deprecatedChild3);
    assertEquals(key2, deprecatedChild3.getParentKey().intValue());
    Concept deprecatedChild4 = conceptService.get(key4);
    assertNotDeprecated(deprecatedChild4);
    assertEquals(key2, deprecatedChild4.getParentKey().intValue());
  }

  @Test
  public void deprecateWithoutReplacementTest() {
    long key1 = conceptService.create(createBasicConcept(vocabularyKeys[0]));
    assertDoesNotThrow(
        () -> conceptService.deprecateWithoutReplacement(key1, DEPRECATED_BY, false));
    assertDeprecated(conceptService.get(key1), DEPRECATED_BY);

    conceptService.restoreDeprecated(key1, false);
    assertNotDeprecated(conceptService.get(key1));

    // add children to the concept
    Concept child1 = createBasicConcept(vocabularyKeys[0]);
    child1.setParentKey(key1);
    long key2 = conceptService.create(child1);
    Concept child2 = createBasicConcept(vocabularyKeys[0]);
    child2.setParentKey(key1);
    long key3 = conceptService.create(child2);

    // deprecating without children is not allowed
    assertThrows(
        IllegalArgumentException.class,
        () -> conceptService.deprecateWithoutReplacement(key1, DEPRECATED_BY, false));

    // deprecating children too
    conceptService.deprecateWithoutReplacement(key1, DEPRECATED_BY, true);
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
    long vocabularyKey = vocabularyService.create(vocabulary);

    // create concept for that vocabulary
    long key1 = conceptService.create(createBasicConcept(vocabularyKey));
    // deprecate vocabulary and concept
    vocabularyService.deprecateWithoutReplacement(vocabularyKey, DEPRECATED_BY, true);

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
    long root = conceptService.create(createBasicConcept(vocabularyKeys[0]));
    long child1 = conceptService.create(createBasicConcept(vocabularyKeys[0]));
    long child2 = conceptService.create(createBasicConcept(vocabularyKeys[0]));

    Concept mainConcept = createBasicConcept(vocabularyKeys[0]);
    mainConcept.setParentKey(child2);
    long mainConceptKey = conceptService.create(mainConcept);

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

  @Test
  public void findParentsTest() {
    Concept concept1 = createBasicConcept(vocabularyKeys[0]);
    conceptService.create(concept1);
    assertTrue(conceptService.findParents(concept1.getKey()).isEmpty());

    Concept concept2 = createBasicConcept(vocabularyKeys[0]);
    concept2.setParentKey(concept1.getKey());
    conceptService.create(concept2);
    assertEquals(1, conceptService.findParents(concept2.getKey()).size());
    assertEquals(concept1.getName(), conceptService.findParents(concept2.getKey()).get(0));

    Concept concept3 = createBasicConcept(vocabularyKeys[0]);
    concept3.setParentKey(concept2.getKey());
    conceptService.create(concept3);
    List<String> parents = conceptService.findParents(concept3.getKey());
    assertEquals(2, parents.size());
    assertTrue(parents.contains(concept1.getName()));
    assertTrue(parents.contains(concept2.getName()));
  }

  @Test
  public void tagsTest() {
    Concept concept1 = createBasicConcept(vocabularyKeys[0]);
    conceptService.create(concept1);

    Tag tag = new Tag();
    tag.setName("Tag");
    tag.setCreatedBy("Test");
    tag.setModifiedBy("Test");
    tagService.create(tag);

    Tag tag2 = new Tag();
    tag2.setName("Tag2");
    tag2.setCreatedBy("Test");
    tag2.setModifiedBy("Test");
    tagService.create(tag2);

    conceptService.addTag(concept1.getKey(), tag.getKey());
    conceptService.addTag(concept1.getKey(), tag2.getKey());
    PagingResponse<Concept> concepts =
        conceptService.list(
            ConceptSearchParams.builder().tags(Collections.singletonList(tag.getName())).build(),
            new PagingRequest(0, 5));
    assertEquals(1, concepts.getResults().size());
    concepts =
        conceptService.list(
            ConceptSearchParams.builder().tags(Collections.singletonList(tag2.getName())).build(),
            new PagingRequest(0, 5));
    assertEquals(1, concepts.getResults().size());
    concepts =
        conceptService.list(
            ConceptSearchParams.builder()
                .tags(Arrays.asList(tag.getName(), tag2.getName()))
                .build(),
            new PagingRequest(0, 5));
    assertEquals(1, concepts.getResults().size());

    conceptService.removeTag(concept1.getKey(), tag2.getKey());
    concepts =
        conceptService.list(
            ConceptSearchParams.builder().tags(Collections.singletonList(tag.getName())).build(),
            new PagingRequest(0, 5));
    assertEquals(1, concepts.getResults().size());
    concepts =
        conceptService.list(
            ConceptSearchParams.builder().tags(Collections.singletonList(tag2.getName())).build(),
            new PagingRequest(0, 5));
    assertEquals(0, concepts.getResults().size());
    concepts =
        conceptService.list(
            ConceptSearchParams.builder()
                .tags(Arrays.asList(tag.getName(), tag2.getName()))
                .build(),
            new PagingRequest(0, 5));
    assertEquals(0, concepts.getResults().size());

    tagService.delete(tag.getKey());
    tagService.delete(tag2.getKey());
    concepts =
        conceptService.list(
            ConceptSearchParams.builder()
                .tags(Arrays.asList(tag.getName(), tag2.getName()))
                .build(),
            new PagingRequest(0, 5));
    assertEquals(0, concepts.getResults().size());
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
