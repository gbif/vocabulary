package org.gbif.vocabulary.service.persistence.mapper;

import org.gbif.vocabulary.model.Vocabulary;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.Assert.*;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// @Rollback(false)
@ContextConfiguration(initializers = {VocabularyMapperTest.Initializer.class})
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class VocabularyMapperTest {

  @Autowired private VocabularyMapper vocabularyMapper;

  public static PostgreSQLContainer postgresContainer = new PostgreSQLContainer("postgres:11.1");

  @BeforeAll
  public static void setup() throws IOException, SQLException {
    postgresContainer.start();
  }

  @AfterAll
  public static void tearDown() {
    postgresContainer.close();
  }

  @Test
  public void test() {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setKey(1);
    vocabulary.setName("real db");
    vocabulary.setCreatedBy("test");
    vocabulary.setModifiedBy("test");
    vocabularyMapper.create(vocabulary);

    List<Vocabulary> vocabularies = vocabularyMapper.list();
    assertEquals(1, vocabularies.size());
  }

  static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
              "spring.datasource.url=" + postgresContainer.getJdbcUrl(),
              "spring.datasource.username=" + postgresContainer.getUsername(),
              "spring.datasource.password=" + postgresContainer.getPassword())
          .applyTo(configurableApplicationContext.getEnvironment());
    }
  }
}
