package org.gbif.vocabulary.persistence.mappers;

import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.persistence.PostgresDBExtension;

import java.net.URI;
import java.util.ArrayList;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
   * This is not in the base class because when running tests in parallel it uses the same DB for
   * all the children.
   */
  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

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

    assertList("vocab1", null, null, null, 1);
    assertList("voc", null, null, null, 3);
    assertList("ocab", null, null, null, 0);
    assertList("namesp gb", null, null, null, 1);
    assertList(null, "vocab1", null, null, 1);
    assertList(null, "vocab", null, null, 0);
    assertList(null, null, "namespace gbif", null, 1);
    assertList(null, null, "namespace", null, 0);
    assertList(null, "vocab2", "namespace2", null, 1);
    assertList("voc", "vocab1", "namespace1", null, 1);
    assertList("oca", "vocab2", "namespace2", null, 0);
    assertList("v gbif", "vocab gbif", null, null, 1);
  }

  @Test
  public void deleteTest() {
    Vocabulary vocabulary1 = createNewEntity("v1");
    vocabulary1.setNamespace("n1");
    vocabularyMapper.create(vocabulary1);

    Vocabulary vocabulary2 = createNewEntity("v2");
    vocabulary2.setNamespace("n1");
    vocabularyMapper.create(vocabulary2);

    Vocabulary vocabulary3 = createNewEntity("v3");
    vocabulary3.setNamespace("n3");
    vocabularyMapper.create(vocabulary3);

    // delete
    assertList(null, null, null, true, 0);

    vocabularyMapper.delete(vocabulary1.getKey());
    assertList(null, null, null, true, 1);
    assertList("v", "v1", null, true, 1);

    // test deleted param
    assertList("n1", null, null, true, 1);
    assertList("n1", null, null, false, 1);
    assertList("n1", null, null, null, 2);

    assertList("z", null, null, true, 0);
    assertList(null, null, "n3", true, 0);

    vocabularyMapper.delete(vocabulary2.getKey());
    assertList(null, null, null, true, 2);
    assertList("v", null, null, true, 2);
    assertList("n1", null, null, true, 2);
    assertList("z", null, null, true, 0);
    assertList(null, null, "n3", true, 0);

    Vocabulary vocabularyDeleted = vocabularyMapper.get(vocabulary2.getKey());
    assertNotNull(vocabularyDeleted.getDeleted());

    // restore deleted
    vocabularyDeleted.setDeleted(null);
    vocabularyMapper.update(vocabularyDeleted);
    assertList(null, null, null, true, 1);
    assertList("v", null, null, true, 1);
    assertList("n1", null, null, true, 1);
    assertList("z", null, null, true, 0);
    assertList(null, null, "n3", true, 0);
  }

  @Test
  public void findSimilaritiesTest() {
    Vocabulary vocabulary1 = createNewEntity("similar");
    vocabulary1.setLabel(Collections.singletonMap(Language.SPANISH, "igual"));
    vocabularyMapper.create(vocabulary1);

    List<KeyNameResult> similarities =
        vocabularyMapper.findSimilarities(createNewEntity("igual"));
    assertEquals(1, similarities.size());
    assertEquals(vocabulary1.getKey().intValue(), similarities.get(0).getKey());
    assertEquals(vocabulary1.getName(), similarities.get(0).getName());

    Vocabulary vocabulary2 = createNewEntity("similar2");
    vocabulary2.setLabel(Collections.singletonMap(Language.SPANISH, "igual"));
    vocabularyMapper.create(vocabulary2);
    assertEquals(2, vocabularyMapper.findSimilarities(createNewEntity("igual")).size());
  }

  private void assertList(
      String query, String name, String namespace, Boolean deleted, int expectedResult) {
    assertEquals(
        expectedResult,
        vocabularyMapper.list(query, name, namespace, deleted, DEFAULT_PAGE).size());
    assertEquals(expectedResult, vocabularyMapper.count(query, name, namespace, deleted));
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
