package org.gbif.vocabulary.persistence.mappers;

import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.PostgresDBExtension;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests the {@link ConceptMapper} class
 *
 * <p>It uses a embedded PostgreSQL provided by {@link PostgreSQLContainer} which is started before
 * the tests run and it's reused by all the tests.
 *
 * <p>All the methods are intended to be run in parallel.
 */
@ContextConfiguration(initializers = {ConceptMapperTest.ContexInitializer.class})
public class ConceptMapperTest extends BaseMapperTest<Concept> {

  /**
   * This is not in the base class because when running tests in parallel it uses the same DB for
   * all the children.
   */
  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  private static int defaultVocabularyKey;

  private final ConceptMapper conceptMapper;

  @Autowired
  ConceptMapperTest(ConceptMapper conceptMapper) {
    super(conceptMapper);
    this.conceptMapper = conceptMapper;
  }

  /**
   * Creates a default vocabulary to use it in the concepts, since the vocabularyKey of a concept
   * cannot be null.
   *
   * @param vocabularyMapper to insert the vocabulary in the DB.
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

  @Override
  Concept createNewEntity(String name) {
    Concept entity = new Concept();
    entity.setVocabularyKey(defaultVocabularyKey);
    entity.setName(name);
    entity.setLabel(new HashMap<>(Collections.singletonMap(Language.ENGLISH, "Label")));
    entity.setMisspeltLabels(
        Collections.singletonMap(Language.SPANISH, Arrays.asList("labl", "lbel")));
    entity.setDefinition(new HashMap<>(Collections.singletonMap(Language.ENGLISH, "Definition")));
    entity.setExternalDefinitions(
        new ArrayList<>(Collections.singletonList(URI.create("http://test.com"))));
    entity.setEditorialNotes(new ArrayList<>(Collections.singletonList("Note test")));
    entity.setCreatedBy("test");
    entity.setModifiedBy("test");

    return entity;
  }

  @Test
  public void listConceptsTest() {
    Concept concept1 = createNewEntity("concept1");
    concept1.setAlternativeLabels(
        Collections.singletonMap(
            Language.ENGLISH, Collections.singletonList("alternative example")));
    conceptMapper.create(concept1);

    Concept concept2 = createNewEntity("concept2");
    concept2.setParentKey(concept1.getKey());
    concept2.setMisspeltLabels(
        Collections.singletonMap(Language.ENGLISH, Collections.singletonList("misspelt example")));
    conceptMapper.create(concept2);

    Concept concept3 = createNewEntity("concept3");
    concept3.setParentKey(concept1.getKey());
    concept3.setEditorialNotes(Collections.singletonList("editorial notes"));
    conceptMapper.create(concept3);

    assertList("concept1", null, null, null, null, null, 1);
    assertList("conc", null, null, null, null, null, 3);
    assertList("example", null, null, null, null, null, 2);
    assertList("altern ex", null, null, null, null, null, 1);
    assertList("oncept", null, null, null, null, null, 0);
    assertList(null, defaultVocabularyKey, null, null, null, null, 3);
    assertList(null, null, concept1.getKey(), null, null, null, 2);
    assertList(null, null, concept2.getKey(), null, null, null, 0);
    assertList(null, null, null, null, "concept1", null, 1);
    assertList(null, null, null, null, "concepto", null, 0);
    assertList("exa", defaultVocabularyKey, null, null, null, null, 2);
    assertList(null, null, concept1.getKey(), null, "concept3", null, 1);
  }

  @Test
  public void findSimilaritiesTest() {
    Concept concept1 = createNewEntity("first");
    concept1.setLabel(new HashMap<>(Collections.singletonMap(Language.SPANISH, "primero")));
    concept1.setMisspeltLabels(
        Collections.singletonMap(Language.SPANISH, Collections.singletonList("primeiro")));
    conceptMapper.create(concept1);

    Concept concept2 = createNewEntity("primero");
    List<KeyNameResult> similarities = conceptMapper.findSimilarities(concept2);
    assertEquals(1, similarities.size());
    assertEquals(concept1.getKey().intValue(), similarities.get(0).getKey());
    assertEquals(concept1.getName(), similarities.get(0).getName());

    Concept concept3 = createNewEntity("primeiro");
    similarities = conceptMapper.findSimilarities(concept3);
    assertEquals(1, similarities.size());
    assertEquals(concept1.getKey().intValue(), similarities.get(0).getKey());
    assertEquals(concept1.getName(), similarities.get(0).getName());
  }

  @Test
  public void deprecationInBulkTest() {
    Concept concept1 = createNewEntity("deprecated");
    conceptMapper.create(concept1);
    assertNull(concept1.getDeprecated());

    Concept concept2 = createNewEntity("deprecated2");
    conceptMapper.create(concept2);
    assertNull(concept2.getDeprecated());

    // deprecate in bulk
    conceptMapper.deprecateInBulk(
        Arrays.asList(concept1.getKey(), concept2.getKey()), DEPRECATED_BY, null);
    assertEquals(2, conceptMapper.list(null, null, null, null, null, true, null).size());

    // undeprecate in bulk
    conceptMapper.restoreDeprecatedInBulk(Arrays.asList(concept1.getKey(), concept2.getKey()));
    assertEquals(0, conceptMapper.list(null, null, null, null, null, true, null).size());
  }

  @Test
  public void updateParentTest() {
    Concept concept1 = createNewEntity("updatable");
    conceptMapper.create(concept1);

    Concept concept2 = createNewEntity("updatable2");
    concept2.setParentKey(concept1.getKey());
    conceptMapper.create(concept2);

    Concept concept3 = createNewEntity("updatable3");
    conceptMapper.create(concept3);

    // assert parent
    Concept conceptSaved = conceptMapper.get(concept2.getKey());
    assertEquals(concept1.getKey(), conceptSaved.getParentKey());

    // update parent
    conceptMapper.updateParent(Collections.singletonList(concept2.getKey()), concept3.getKey());
    conceptSaved = conceptMapper.get(concept2.getKey());
    assertEquals(concept3.getKey(), conceptSaved.getParentKey());
  }

  @Test
  public void findReplacementTest() {
    Concept concept1 = createNewEntity("r1");
    conceptMapper.create(concept1);
    // not deprecated
    assertNull(conceptMapper.findReplacement(concept1.getKey()));

    // deprecated by concept 1
    Concept concept2 = createNewEntity("r2");
    conceptMapper.create(concept2);
    conceptMapper.deprecate(concept2.getKey(), DEPRECATED_BY, concept1.getKey());
    assertEquals(concept1.getKey(), conceptMapper.findReplacement(concept2.getKey()));

    // deprecated by concept 2 and concept 2 is deprecated by concept 1
    Concept concept3 = createNewEntity("r3");
    conceptMapper.create(concept3);
    conceptMapper.deprecate(concept3.getKey(), DEPRECATED_BY, concept2.getKey());
    assertEquals(concept1.getKey(), conceptMapper.findReplacement(concept3.getKey()));

    // concept 3 replaced by concept 2 and concept 2 is deprecated without replacement, hence
    // concept 3 hasn't a non-deprecated replacement
    conceptMapper.deprecate(concept2.getKey(), DEPRECATED_BY, null);
    assertNull(conceptMapper.findReplacement(concept3.getKey()));

    // concept 3 deprecated by concept 1
    conceptMapper.deprecate(concept3.getKey(), DEPRECATED_BY, concept1.getKey());
    assertEquals(concept1.getKey(), conceptMapper.findReplacement(concept3.getKey()));

    // concept 3 deprecated without replacement
    conceptMapper.deprecate(concept3.getKey(), DEPRECATED_BY, null);
    assertNull(conceptMapper.findReplacement(concept3.getKey()));

    // not deprecated, so no need to find replacement
    conceptMapper.restoreDeprecated(concept3.getKey());
    assertNull(conceptMapper.findReplacement(concept3.getKey()));
  }

  @Test
  public void getVocabularyKeyTest() {
    Concept concept1 = createNewEntity("r1");
    assertEquals(
        defaultVocabularyKey, conceptMapper.getVocabularyKey(concept1.getKey()).intValue());
  }

  private void assertList(
      String query,
      Integer vocabularyKey,
      Integer parentKey,
      Integer replacedByKey,
      String name,
      Boolean deprecated,
      int expectedResult) {
    assertEquals(
        expectedResult,
        conceptMapper
            .list(query, vocabularyKey, parentKey, replacedByKey, name, deprecated, DEFAULT_PAGE)
            .size());
    assertEquals(
        expectedResult,
        conceptMapper.count(query, vocabularyKey, parentKey, replacedByKey, name, deprecated));
  }

  /**
   * Initializes the Spring Context. Needed to create the datasource on the fly using the postgres
   * container.
   *
   * <p>NOTE: this initializer cannot be in the base class because it gets executed only once.
   */
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
