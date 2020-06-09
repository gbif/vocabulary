package org.gbif.vocabulary.persistence.mappers;

import org.gbif.vocabulary.PostgresDBExtension;
import org.gbif.vocabulary.TestUtils;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyRelease;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@link VocabularyReleaseMapper}.
 *
 * <p>It rolls back all the transactions, so there is no need to clean the DB before/after each
 * test.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = {VocabularyReleaseMapperTest.ContexInitializer.class})
public class VocabularyReleaseMapperTest {

  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  private static final String DEFAULT_VOCABULARY = "default";

  private final VocabularyReleaseMapper vocabularyReleaseMapper;

  private static long vocabularyKey;

  @Autowired
  VocabularyReleaseMapperTest(VocabularyReleaseMapper vocabularyReleaseMapper) {
    this.vocabularyReleaseMapper = vocabularyReleaseMapper;
  }

  /**
   * Creates a default vocabulary to use in the releases since it is a required field.
   *
   * @param vocabularyMapper to insert the vocabulary in the DB.
   */
  @BeforeAll
  public static void populateData(@Autowired VocabularyMapper vocabularyMapper) {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(DEFAULT_VOCABULARY);
    vocabulary.setCreatedBy("test");
    vocabulary.setModifiedBy("test");
    vocabularyMapper.create(vocabulary);
    vocabularyKey = vocabulary.getKey();
  }

  @Test
  public void createAndGetTest() {
    VocabularyRelease vocabularyRelease = new VocabularyRelease();
    vocabularyRelease.setVocabularyKey(vocabularyKey);
    vocabularyRelease.setVersion("1.0");
    vocabularyRelease.setCreatedBy("test");
    vocabularyRelease.setExportUrl("dummy url");

    // create release
    vocabularyReleaseMapper.create(vocabularyRelease);
    assertNotNull(vocabularyRelease.getKey());

    // get release
    VocabularyRelease stored = vocabularyReleaseMapper.get(vocabularyRelease.getKey());
    assertTrue(vocabularyRelease.lenientEquals(stored));
  }

  @Test
  public void listTest() {
    VocabularyRelease vr1 = new VocabularyRelease();
    vr1.setVocabularyKey(vocabularyKey);
    vr1.setVersion("1.0");
    vr1.setCreatedBy("test");
    vr1.setExportUrl("dummy url");
    vocabularyReleaseMapper.create(vr1);

    VocabularyRelease vr2 = new VocabularyRelease();
    vr2.setVocabularyKey(vocabularyKey);
    vr2.setVersion("2.0");
    vr2.setCreatedBy("test");
    vr2.setExportUrl("dummy url");
    vocabularyReleaseMapper.create(vr2);

    VocabularyRelease vr3 = new VocabularyRelease();
    vr3.setVocabularyKey(vocabularyKey);
    vr3.setVersion("3.0");
    vr3.setCreatedBy("test");
    vr3.setExportUrl("dummy url");
    vocabularyReleaseMapper.create(vr3);

    assertEquals(3, vocabularyReleaseMapper.list(null, null, TestUtils.DEFAULT_PAGE).size());
    assertEquals(3, vocabularyReleaseMapper.count(null, null));
    assertEquals(
        3, vocabularyReleaseMapper.list(vocabularyKey, null, TestUtils.DEFAULT_PAGE).size());
    assertEquals(3, vocabularyReleaseMapper.count(vocabularyKey, null));
    assertEquals(
        1, vocabularyReleaseMapper.list(vocabularyKey, "1.0", TestUtils.DEFAULT_PAGE).size());
    assertEquals(1, vocabularyReleaseMapper.list(null, "2.0", TestUtils.DEFAULT_PAGE).size());
    assertEquals(1, vocabularyReleaseMapper.count(vocabularyKey, "1.0"));
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
