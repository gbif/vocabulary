package org.gbif.vocabulary.persistence.mappers;

import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.PostgresDBExtension;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.KeyNameResult;

import java.net.URI;
import java.util.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.gbif.vocabulary.TestUtils.DEFAULT_PAGE;
import static org.gbif.vocabulary.TestUtils.DEPRECATED_BY;
import static org.gbif.vocabulary.TestUtils.assertNotDeprecated;
import static org.gbif.vocabulary.model.normalizers.EntityNormalizer.normalizeLabel;
import static org.gbif.vocabulary.model.normalizers.EntityNormalizer.normalizeLabels;
import static org.gbif.vocabulary.model.normalizers.EntityNormalizer.normalizeName;

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

  private static final String DEFAULT_VOCABULARY = "default";

  private static int[] vocabularyKeys = new int[2];

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
  public static void populateData(
      @Autowired VocabularyMapper vocabularyMapper, @Autowired ConceptMapper conceptMapper) {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(DEFAULT_VOCABULARY);
    vocabulary.setCreatedBy("test");
    vocabulary.setModifiedBy("test");
    vocabularyMapper.create(vocabulary);
    vocabularyKeys[0] = vocabulary.getKey();

    Vocabulary vocabulary2 = new Vocabulary();
    vocabulary2.setName("default2");
    vocabulary2.setCreatedBy("test");
    vocabulary2.setModifiedBy("test");
    vocabularyMapper.create(vocabulary2);
    vocabularyKeys[1] = vocabulary2.getKey();
  }

  @Test
  public void listConceptsTest() {
    Concept concept1 = createNewEntity();
    concept1.setName("concept1");
    concept1.setAlternativeLabels(
        Collections.singletonMap(
            Language.ENGLISH, Collections.singletonList("alternative example")));
    conceptMapper.create(concept1);

    Concept concept2 = createNewEntity();
    concept2.setName("concept2");
    concept2.setParentKey(concept1.getKey());
    concept2.setMisappliedLabels(
        Collections.singletonMap(Language.ENGLISH, Collections.singletonList("misspelt example")));
    conceptMapper.create(concept2);

    Concept concept3 = createNewEntity();
    concept3.setName("concept3");
    concept3.setParentKey(concept1.getKey());
    concept3.setEditorialNotes(Collections.singletonList("editorial notes"));
    conceptMapper.create(concept3);

    assertList("concept1", null, null, null, null, null, 1);
    assertList("conc", null, null, null, null, null, 3);
    assertList("example", null, null, null, null, null, 2);
    assertList("altern ex", null, null, null, null, null, 1);
    assertList("oncept", null, null, null, null, null, 0);
    assertList(null, vocabularyKeys[0], null, null, null, null, 3);
    assertList(null, null, concept1.getKey(), null, null, null, 2);
    assertList(null, null, concept2.getKey(), null, null, null, 0);
    assertList(null, null, null, null, "concept1", null, 1);
    assertList(null, null, null, null, "concepto", null, 0);
    assertList("exa", vocabularyKeys[0], null, null, null, null, 2);
    assertList(null, null, concept1.getKey(), null, "concept3", null, 1);
  }

  @Test
  public void suggestTest() {
    // create entities for the test
    Concept c1 = createNewEntity();
    c1.setName("suggest111");
    conceptMapper.create(c1);
    assertNotNull(c1.getKey());

    Concept c2 = createNewEntity();
    c2.setName("suggest222");
    conceptMapper.create(c2);
    assertNotNull(c2.getKey());

    // check result values
    List<KeyNameResult> result = conceptMapper.suggest("suggest1", c1.getVocabularyKey());
    assertEquals("suggest111", result.get(0).getName());
    assertEquals(c1.getKey().intValue(), result.get(0).getKey());

    // assert expected number of results
    assertEquals(2, conceptMapper.suggest("su", c1.getVocabularyKey()).size());
    assertEquals(2, conceptMapper.suggest("gge", c1.getVocabularyKey()).size());
    assertEquals(1, conceptMapper.suggest("22", c1.getVocabularyKey()).size());
    assertEquals(0, conceptMapper.suggest("zz", c1.getVocabularyKey()).size());
    assertEquals(0, conceptMapper.suggest(null, c1.getVocabularyKey()).size());

    Concept c3 = createNewEntity();
    c3.setVocabularyKey(vocabularyKeys[1]);
    c3.setName("suggest333");
    conceptMapper.create(c3);
    assertNotNull(c3.getKey());

    assertEquals(2, conceptMapper.suggest("su", c1.getVocabularyKey()).size());
    assertEquals(1, conceptMapper.suggest("su", c3.getVocabularyKey()).size());
    assertEquals(1, conceptMapper.suggest("33", c3.getVocabularyKey()).size());
    assertEquals(0, conceptMapper.suggest("33", c1.getVocabularyKey()).size());
  }

  @Test
  public void findSimilaritiesTest() {
    Concept concept1 = createNewEntity();
    concept1.setLabel(new HashMap<>(Collections.singletonMap(Language.SPANISH, "primero ")));
    concept1.setMisappliedLabels(
        Collections.singletonMap(Language.SPANISH, Arrays.asList("primeiro", "otro primeiro")));
    conceptMapper.create(concept1);

    List<KeyNameResult> similarities =
        conceptMapper.findSimilarities(
            Collections.singletonList(normalizeLabel("primero")),
            concept1.getVocabularyKey(),
            null);
    assertSimilarity(similarities, concept1);

    similarities =
        conceptMapper.findSimilarities(
            Collections.singletonList(normalizeLabel("primeiro")),
            concept1.getVocabularyKey(),
            null);
    assertSimilarity(similarities, concept1);
    similarities =
        conceptMapper.findSimilarities(
            Arrays.asList("foo", normalizeName(concept1.getName())),
            concept1.getVocabularyKey(),
            null);
    assertSimilarity(similarities, concept1);

    similarities =
        conceptMapper.findSimilarities(
            Collections.singletonList("foo"), concept1.getVocabularyKey(), null);
    assertEquals(0, similarities.size());

    // for another vocabulary there should be no match
    similarities = conceptMapper.findSimilarities(Collections.singletonList("foo"), 200, null);
    assertEquals(0, similarities.size());

    // for the same concept there should be no match
    similarities =
        conceptMapper.findSimilarities(
            Collections.singletonList(normalizeLabel("primero")),
            concept1.getVocabularyKey(),
            concept1.getKey());
    assertEquals(0, similarities.size());
  }

  @Test
  public void findSimilaritiesNormalizationTest() {
    Concept concept1 = createNewEntity();
    concept1.setName("my-concept");
    concept1.setLabel(new HashMap<>(Collections.singletonMap(Language.ENGLISH, "normalization ")));
    concept1.setMisappliedLabels(
        Collections.singletonMap(Language.ENGLISH, Arrays.asList("norm", "another norm")));
    conceptMapper.create(concept1);

    List<KeyNameResult> similarities =
        conceptMapper.findSimilarities(
            Collections.singletonList(normalizeLabel(" normaLiZA tion  ")),
            concept1.getVocabularyKey(),
            null);
    assertSimilarity(similarities, concept1);

    similarities =
        conceptMapper.findSimilarities(
            normalizeLabels(Arrays.asList(" aNotHer  NORM  ", "segundo")),
            concept1.getVocabularyKey(),
            null);
    assertSimilarity(similarities, concept1);

    similarities =
        conceptMapper.findSimilarities(
            Collections.singletonList(normalizeName("My Concept")),
            concept1.getVocabularyKey(),
            null);
    assertSimilarity(similarities, concept1);
  }

  @Test
  public void deprecationInBulkTest() {
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);
    assertNotDeprecated(concept1);

    Concept concept2 = createNewEntity();
    conceptMapper.create(concept2);
    assertNotDeprecated(concept2);

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
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);

    Concept concept2 = createNewEntity();
    concept2.setParentKey(concept1.getKey());
    conceptMapper.create(concept2);

    Concept concept3 = createNewEntity();
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
    // just created, doesn't have replacement
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);
    assertNull(conceptMapper.findReplacement(concept1.getKey()));

    // deprecated by concept 1
    Concept concept2 = createNewEntity();
    conceptMapper.create(concept2);
    conceptMapper.deprecate(concept2.getKey(), DEPRECATED_BY, concept1.getKey());
    assertEquals(concept1.getKey(), conceptMapper.findReplacement(concept2.getKey()));

    // deprecated by concept 2 and concept 2 is deprecated by concept 1
    Concept concept3 = createNewEntity();
    conceptMapper.create(concept3);
    conceptMapper.deprecate(concept3.getKey(), DEPRECATED_BY, concept2.getKey());
    assertEquals(concept1.getKey(), conceptMapper.findReplacement(concept3.getKey()));

    // concept 2 deprecated without replacement, hence concept 3 hasn't a non-deprecated replacement
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
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);
    assertEquals(vocabularyKeys[0], conceptMapper.getVocabularyKey(concept1.getKey()).intValue());
  }

  @Test
  public void getByNameAndVocabularyTest() {
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);

    Concept conceptDB =
        conceptMapper.getByNameAndVocabulary(concept1.getName(), DEFAULT_VOCABULARY);
    assertEquals(concept1.getKey(), conceptDB.getKey());
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

  private void assertSimilarity(List<KeyNameResult> similarities, Concept concept) {
    assertEquals(1, similarities.size());
    assertEquals(concept.getKey().intValue(), similarities.get(0).getKey());
    assertEquals(concept.getName(), similarities.get(0).getName());
  }

  @Override
  Concept createNewEntity() {
    Concept entity = new Concept();
    entity.setVocabularyKey(vocabularyKeys[0]);
    entity.setName(UUID.randomUUID().toString());
    entity.setLabel(new HashMap<>(Collections.singletonMap(Language.ENGLISH, "Label")));
    entity.setMisappliedLabels(
        Collections.singletonMap(Language.SPANISH, Arrays.asList("lab,l", "lbel")));
    entity.setDefinition(new HashMap<>(Collections.singletonMap(Language.ENGLISH, "Definition")));
    entity.setExternalDefinitions(
        new ArrayList<>(Collections.singletonList(URI.create("http://test.com"))));
    entity.setEditorialNotes(new ArrayList<>(Collections.singletonList("Note test")));
    entity.setCreatedBy("test");
    entity.setModifiedBy("test");
    return entity;
  }

  /**
   * Initializes the Spring Context. Needed to create the datasource on the fly using the postgres
   * container.
   *
   * <p>NOTE: this initializer cannot be in the base class because it gets executed only once when
   * we run several tests at the same time.
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
