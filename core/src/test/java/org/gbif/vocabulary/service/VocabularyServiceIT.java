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

import org.gbif.vocabulary.PostgresDBExtension;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.UserRoles;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.VocabularySearchParams;

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

/** Integration tests for the {@link VocabularyService}. */
@WithMockUser(authorities = UserRoles.VOCABULARY_ADMIN)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = {VocabularyServiceIT.ContexInitializer.class})
@ActiveProfiles("test")
public class VocabularyServiceIT {

  private static final String CLEAN_DB_SCRIPT = "/clean-db.sql";

  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  @Autowired private DataSource dataSource;

  private final VocabularyService vocabularyService;
  private final ConceptService conceptService;

  @BeforeEach
  public void cleanDB() throws SQLException {
    Connection connection = dataSource.getConnection();
    ScriptUtils.executeSqlScript(connection, new ClassPathResource(CLEAN_DB_SCRIPT));
    connection.close();
  }

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
    vocabularyService.create(vocabulary);

    Vocabulary similarName = createBasicVocabulary();
    similarName.setName(vocabulary.getName());
    assertThrows(IllegalArgumentException.class, () -> vocabularyService.create(similarName));
  }

  @Test
  public void updateTest() {
    Vocabulary vocabulary = createBasicVocabulary();
    long key = vocabularyService.create(vocabulary);
    vocabulary = vocabularyService.get(key);

    // update concept
    vocabulary.setEditorialNotes(Arrays.asList("note1", "note2"));
    vocabularyService.update(vocabulary);

    Vocabulary updatedVocabulary = vocabularyService.get(key);
    assertTrue(updatedVocabulary.getEditorialNotes().containsAll(Arrays.asList("note1", "note2")));
  }

  @Test
  public void updateSimilarVocabularyTest() {
    Vocabulary vocabulary1 = createBasicVocabulary();
    vocabulary1.setName("SimVocab");
    vocabularyService.create(vocabulary1);

    Vocabulary vocabulary2 = createBasicVocabulary();
    long key2 = vocabularyService.create(vocabulary2);

    // update concept
    Vocabulary updated = vocabularyService.get(key2);
    updated.setName(vocabulary1.getName());
    assertThrows(IllegalArgumentException.class, () -> vocabularyService.update(updated));
  }

  @Test
  public void deprecatingWhenUpdatingTest() {
    Vocabulary vocabulary = createBasicVocabulary();
    long key = vocabularyService.create(vocabulary);

    Vocabulary createdVocabulary = vocabularyService.get(key);
    createdVocabulary.setReplacedByKey(2L);
    assertThrows(IllegalArgumentException.class, () -> vocabularyService.update(createdVocabulary));
  }

  @Test
  public void deletingWhenUpdatingTest() {
    Vocabulary vocabulary = createBasicVocabulary();
    long key = vocabularyService.create(vocabulary);

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
    long v1Key = vocabularyService.create(createBasicVocabulary());

    vocabularyService.deprecateWithoutReplacement(v1Key, DEPRECATED_BY, false);
    assertDeprecated(vocabularyService.get(v1Key), DEPRECATED_BY);

    vocabularyService.restoreDeprecated(v1Key, false);
    assertNotDeprecated(vocabularyService.get(v1Key));

    // add concepts to the vocabulary
    long c1Key = conceptService.create(createBasicConcept(v1Key));
    long c2Key = conceptService.create(createBasicConcept(v1Key));

    // create a replacement
    long v2Key = vocabularyService.create(createBasicVocabulary());

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

  @Test
  public void deleteVocabularyTest() {
    long v1Key = vocabularyService.create(createBasicVocabulary());

    // add concepts to the vocabulary
    conceptService.create(createBasicConcept(v1Key));
    conceptService.create(createBasicConcept(v1Key));

    assertNotNull(vocabularyService.get(v1Key));
    vocabularyService.deleteVocabulary(v1Key);
    assertNull(vocabularyService.get(v1Key));
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
