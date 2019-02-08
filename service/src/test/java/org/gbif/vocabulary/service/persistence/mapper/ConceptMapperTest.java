package org.gbif.vocabulary.service.persistence.mapper;

import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.service.PostgresDBExtension;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

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
    concept1.setVocabularyKey(defaultVocabularyKey);
    concept1.setAlternativeLabels(
        Collections.singletonMap(
            Language.ENGLISH, Collections.singletonList("alternative example")));
    conceptMapper.create(concept1);

    Concept concept2 = createNewEntity("concept2");
    concept2.setVocabularyKey(defaultVocabularyKey);
    concept2.setParentKey(concept1.getKey());
    concept2.setMisspeltLabels(
        Collections.singletonMap(Language.ENGLISH, Collections.singletonList("misspelt example")));
    conceptMapper.create(concept2);

    Concept concept3 = createNewEntity("concept3");
    concept3.setVocabularyKey(defaultVocabularyKey);
    concept3.setParentKey(concept1.getKey());
    concept3.setReplacedByKey(concept2.getKey());
    concept3.setEditorialNotes(Collections.singletonList("editorial notes"));
    conceptMapper.create(concept3);

    // create search params
    List<SearchParameter> params =
        Arrays.asList(
            new SearchParameter("concept1", null, null, null, null, 1),
            new SearchParameter("conc", null, null, null, null, 3),
            new SearchParameter("example", null, null, null, null, 2),
            new SearchParameter("altern ex", null, null, null, null, 1),
            new SearchParameter("oncept", null, null, null, null, 0),
            new SearchParameter(null, defaultVocabularyKey, null, null, null, 3),
            new SearchParameter(null, null, concept1.getKey(), null, null, 2),
            new SearchParameter(null, null, concept2.getKey(), null, null, 0),
            new SearchParameter(null, null, null, concept2.getKey(), null, 1),
            new SearchParameter(null, null, null, concept3.getKey(), null, 0),
            new SearchParameter(null, null, null, null, "concept1", 1),
            new SearchParameter(null, null, null, null, "concepto", 0),
            new SearchParameter("exa", defaultVocabularyKey, null, null, null, 2),
            new SearchParameter(null, null, concept1.getKey(), null, "concept3", 1));

    // make the calls and assert results
    params.forEach(this::assertSearch);
  }

  /**
   * Makes a concept search and count and asserts the results by checking that both methods return
   * the same number of results.
   *
   * @param p {@link SearchParameter} with the parameters needed to do the search and the expected
   *     number of results.
   */
  private void assertSearch(SearchParameter p) {
    assertThat(
        p.expectedResult,
        allOf(
            is(
                conceptMapper
                    .list(
                        p.query,
                        p.vocabularyKey,
                        p.parentKey,
                        p.replacedByKey,
                        p.name,
                        DEFAULT_PAGE)
                    .size()),
            is(
                (int)
                    conceptMapper.count(
                        p.query, p.vocabularyKey, p.parentKey, p.replacedByKey, p.name))));
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

  /** Holder for the list concepts parameters. */
  private class SearchParameter {
    String query;
    Integer vocabularyKey;
    Integer parentKey;
    Integer replacedByKey;
    String name;
    int expectedResult;

    SearchParameter(
        String query,
        Integer vocabularyKey,
        Integer parentKey,
        Integer replacedByKey,
        String name,
        int expectedResult) {
      this.query = query;
      this.vocabularyKey = vocabularyKey;
      this.parentKey = parentKey;
      this.replacedByKey = replacedByKey;
      this.name = name;
      this.expectedResult = expectedResult;
    }
  }
}
