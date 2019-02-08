package org.gbif.vocabulary.service.persistence.mapper;

import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.service.PostgresDBExtension;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
 * Tests the {@link VocabularyMapper} class
 *
 * <p>It uses a embedded PostgreSQL provided by {@link PostgreSQLContainer} which is started before
 * the tests run and it's reused by all the tests.
 *
 * <p>All the methods are intended to be run in parallel.
 */
@ContextConfiguration(initializers = {VocabularyMapperTest.ContexInitializer.class})
public class VocabularyMapperTest extends BaseMapperTest<Vocabulary> {

  /**
   * This is not in the base class because when running tests in parallel it uses the same DB for all the children.
   */
  @RegisterExtension
  static PostgresDBExtension database = new PostgresDBExtension();

  private final VocabularyMapper vocabularyMapper;

  @Autowired
  VocabularyMapperTest(VocabularyMapper vocabularyMapper) {
    super(vocabularyMapper);
    this.vocabularyMapper = vocabularyMapper;
  }

  @Override
  Vocabulary createNewEntity(String name) {
    Vocabulary entity = new Vocabulary();
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
  public void listVocabulariesTest() {
    Vocabulary vocabulary1 = createNewEntity("vocab1");
    vocabulary1.setNamespace("namespace1");
    vocabularyMapper.create(vocabulary1);

    Vocabulary vocabulary2 = createNewEntity("vocab2");
    vocabulary2.setNamespace("namespace2");
    vocabularyMapper.create(vocabulary2);

    Vocabulary vocabularyGbif = createNewEntity("vocab gbif");
    vocabularyGbif.setNamespace("namespace gbif");
    vocabularyMapper.create(vocabularyGbif);

    // create search params
    List<SearchParameter> params =
        Arrays.asList(
            new SearchParameter("vocab1", null, null, 1),
            new SearchParameter("voc", null, null, 3),
            new SearchParameter("ocab", null, null, 0),
            new SearchParameter("namesp gb", null, null, 1),
            new SearchParameter(null, "vocab1", null, 1),
            new SearchParameter(null, "vocab", null, 0),
            new SearchParameter(null, null, "namespace gbif", 1),
            new SearchParameter(null, null, "namespace", 0),
            new SearchParameter(null, "vocab2", "namespace2", 1),
            new SearchParameter("voc", "vocab1", "namespace1", 1),
            new SearchParameter("oca", "vocab2", "namespace2", 0),
            new SearchParameter("v gbif", "vocab gbif", null, 1));

    // make the calls and assert results
    params.forEach(this::assertSearch);
  }

  /**
   * Makes a vocabulary search and count and asserts the results by checking that both methods
   * return the same number of results.
   *
   * @param p {@link SearchParameter} with the parameters needed to do the search and the expected
   *     number of results.
   */
  private void assertSearch(SearchParameter p) {
    assertThat(
        p.expectedResult,
        allOf(
            is(vocabularyMapper.list(p.query, p.name, p.namespace, DEFAULT_PAGE).size()),
            is((int) vocabularyMapper.count(p.query, p.name, p.namespace))));
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

  /** Holder for the list vocabulary parameters. */
  private class SearchParameter {
    String query;
    String name;
    String namespace;
    int expectedResult;

    SearchParameter(String query, String name, String namespace, int expectedResult) {
      this.query = query;
      this.name = name;
      this.namespace = namespace;
      this.expectedResult = expectedResult;
    }
  }
}
