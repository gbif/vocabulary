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
package org.gbif.vocabulary.persistence.mappers;

import org.gbif.vocabulary.TestUtils;
import org.gbif.vocabulary.model.*;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.persistence.dto.SuggestDto;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TestTransaction;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.gbif.vocabulary.TestUtils.DEFAULT_PAGE;
import static org.gbif.vocabulary.TestUtils.DEPRECATED_BY;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the {@link VocabularyMapper} class
 *
 * <p>It uses a embedded PostgreSQL provided by {@link PostgreSQLContainer} which is started before
 * the tests run and it's reused by all the tests.
 */
@ContextConfiguration(initializers = {VocabularyMapperTest.ContextInitializer.class})
public class VocabularyMapperTest extends BaseMapperTest<Vocabulary> {

  private static final int DEFAULT_SUGGEST_LIMIT = 20;
  private final VocabularyMapper vocabularyMapper;
  private final ConceptMapper conceptMapper;
  private final VocabularyReleaseMapper vocabularyReleaseMapper;

  @Autowired
  VocabularyMapperTest(
      VocabularyMapper vocabularyMapper,
      VocabularyReleaseMapper vocabularyReleaseMapper,
      ConceptMapper conceptMapper) {
    super(vocabularyMapper);
    this.vocabularyMapper = vocabularyMapper;
    this.vocabularyReleaseMapper = vocabularyReleaseMapper;
    this.conceptMapper = conceptMapper;
  }

  @Test
  public void listVocabulariesTest() throws InterruptedException {
    Vocabulary vocabulary1 = createNewEntity();
    vocabulary1.setName("Vocab1");
    vocabulary1.setNamespace("namespace1");
    vocabularyMapper.create(vocabulary1);

    Concept c1 = new Concept();
    c1.setName("C1");
    c1.setVocabularyKey(vocabulary1.getKey());
    c1.setCreatedBy("test");
    c1.setModifiedBy("test");
    conceptMapper.create(c1);

    Vocabulary vocabulary2 = createNewEntity();
    vocabulary2.setName("Vocab2");
    vocabulary2.setNamespace("namespace2");
    vocabularyMapper.create(vocabulary2);

    Vocabulary vocabularyGbif = createNewEntity();
    vocabularyGbif.setName("VocabGbif");
    vocabularyGbif.setNamespace("namespace gbif");
    vocabularyMapper.create(vocabularyGbif);

    assertList(VocabularySearchParams.builder().query("vocab1").key(Long.MAX_VALUE).build(), 0);
    assertList(
        VocabularySearchParams.builder().query("vocab1").key(vocabulary1.getKey()).build(), 1);
    assertList(
        VocabularySearchParams.builder().query("(vocab1)").key(vocabulary1.getKey()).build(), 1);
    assertList(VocabularySearchParams.builder().key(vocabulary2.getKey()).build(), 1);
    assertList(VocabularySearchParams.builder().query("voc").build(), 3);
    assertList(VocabularySearchParams.builder().query("ocab").build(), 0);
    assertList(VocabularySearchParams.builder().query("namesp gb").build(), 1);
    assertList(VocabularySearchParams.builder().name("Vocab1").build(), 1);
    assertList(VocabularySearchParams.builder().name("Vocab").build(), 0);
    assertList(VocabularySearchParams.builder().namespace("namespace gbif").build(), 1);
    assertList(VocabularySearchParams.builder().namespace("namespace").build(), 0);
    assertList(VocabularySearchParams.builder().name("Vocab2").namespace("namespace2").build(), 1);
    assertList(
        VocabularySearchParams.builder()
            .query("voc")
            .name("Vocab1")
            .namespace("namespace1")
            .build(),
        1);
    assertList(
        VocabularySearchParams.builder()
            .query("oca")
            .name("Vocab2")
            .namespace("namespace2")
            .build(),
        0);
    assertList(VocabularySearchParams.builder().query("v gbif").name("VocabGbif").build(), 1);

    TestTransaction.flagForCommit();
    TestTransaction.end();
    TestTransaction.start();

    assertList(VocabularySearchParams.builder().hasUnreleasedChanges(false).build(), 0);
    assertList(VocabularySearchParams.builder().hasUnreleasedChanges(true).build(), 3);

    VocabularyRelease vr = new VocabularyRelease();
    vr.setVocabularyKey(vocabulary1.getKey());
    vr.setComment("test");
    vr.setVersion("1.0");
    vr.setExportUrl("http://test.com");
    vr.setCreatedBy("test");
    vocabularyReleaseMapper.create(vr);

    TestTransaction.flagForCommit();
    TestTransaction.end();
    TestTransaction.start();

    assertList(VocabularySearchParams.builder().hasUnreleasedChanges(true).build(), 2);
    assertList(VocabularySearchParams.builder().hasUnreleasedChanges(false).build(), 1);

    TestTransaction.flagForCommit();
    TestTransaction.end();
    TestTransaction.start();

    vocabulary1.getEditorialNotes().add("test");
    vocabularyMapper.update(vocabulary1);

    vocabularyMapper.get(vocabulary1.getKey());

    assertList(VocabularySearchParams.builder().hasUnreleasedChanges(true).build(), 3);

    VocabularyRelease vr2 = new VocabularyRelease();
    vr2.setVocabularyKey(vocabulary1.getKey());
    vr2.setComment("test");
    vr2.setVersion("2.0");
    vr2.setExportUrl("http://test.com");
    vr2.setCreatedBy("test");
    vocabularyReleaseMapper.create(vr2);

    TestTransaction.flagForCommit();
    TestTransaction.end();
    TestTransaction.start();

    assertList(VocabularySearchParams.builder().hasUnreleasedChanges(false).build(), 1);

    conceptMapper.addLabel(
        c1.getKey(),
        Label.builder().language(LanguageRegion.ACHOLI).value("aa").createdBy("test").build());
    assertList(VocabularySearchParams.builder().hasUnreleasedChanges(false).build(), 0);
  }

  @Test
  public void listDeprecatedTest() {
    Vocabulary vocabulary1 = createNewEntity();
    vocabulary1.setNamespace("n1");
    vocabularyMapper.create(vocabulary1);

    Vocabulary vocabulary2 = createNewEntity();
    vocabulary2.setNamespace("n1");
    vocabularyMapper.create(vocabulary2);

    assertList(VocabularySearchParams.builder().deprecated(true).build(), 0);
    assertList(VocabularySearchParams.builder().namespace("n1").deprecated(true).build(), 0);
    assertList(VocabularySearchParams.builder().namespace("n1").build(), 2);
    assertList(VocabularySearchParams.builder().namespace("n1").deprecated(false).build(), 2);

    vocabularyMapper.deprecate(vocabulary1.getKey(), DEPRECATED_BY, null);
    assertList(VocabularySearchParams.builder().deprecated(true).build(), 1);
    assertList(VocabularySearchParams.builder().namespace("n1").deprecated(true).build(), 1);
  }

  @Test
  public void suggestTest() {
    // create entities for the test
    Vocabulary v1 = createNewEntity();
    v1.setName("Suggest111");

    vocabularyMapper.create(v1);
    assertNotNull(v1.getKey());

    vocabularyMapper.addLabel(
        v1.getKey(),
        Label.builder()
            .language(LanguageRegion.SPANISH)
            .value("labelspanish")
            .createdBy("test")
            .build());

    vocabularyMapper.addLabel(
        v1.getKey(),
        Label.builder()
            .language(LanguageRegion.ENGLISH)
            .value("labelenglish")
            .createdBy("test")
            .build());

    Vocabulary v2 = createNewEntity();
    v2.setName("Suggest222");
    vocabularyMapper.create(v2);
    assertNotNull(v2.getKey());

    vocabularyMapper.addLabel(
        v2.getKey(),
        Label.builder().language(LanguageRegion.ENGLISH).value("Label").createdBy("test").build());

    vocabularyMapper.addLabel(
        v2.getKey(),
        Label.builder()
            .language(LanguageRegion.PORTUGUESE)
            .value("tílDE")
            .createdBy("test")
            .build());

    // check result values
    List<SuggestDto> result =
        vocabularyMapper.suggest("suggest1", null, null, DEFAULT_SUGGEST_LIMIT);
    assertEquals("Suggest111", result.get(0).getName());
    assertEquals(v1.getKey(), result.get(0).getKey());

    // assert expected number of results
    assertEquals(2, vocabularyMapper.suggest("su", null, null, DEFAULT_SUGGEST_LIMIT).size());
    assertEquals(2, vocabularyMapper.suggest("gge", null, null, DEFAULT_SUGGEST_LIMIT).size());
    assertEquals(1, vocabularyMapper.suggest("22", null, null, DEFAULT_SUGGEST_LIMIT).size());
    assertEquals(0, vocabularyMapper.suggest("zz", null, null, DEFAULT_SUGGEST_LIMIT).size());
    assertEquals(0, vocabularyMapper.suggest(null, null, null, DEFAULT_SUGGEST_LIMIT).size());
    assertEquals(2, vocabularyMapper.suggest("label", null, null, DEFAULT_SUGGEST_LIMIT).size());
    assertEquals(1, vocabularyMapper.suggest("labeleng", null, null, DEFAULT_SUGGEST_LIMIT).size());
    assertEquals(
        0,
        vocabularyMapper
            .suggest("labeleng", LanguageRegion.SPANISH, null, DEFAULT_SUGGEST_LIMIT)
            .size());
    assertEquals(
        1,
        vocabularyMapper
            .suggest("label", LanguageRegion.SPANISH, null, DEFAULT_SUGGEST_LIMIT)
            .size());
    assertEquals(
        1,
        vocabularyMapper
            .suggest("labeleng", LanguageRegion.ENGLISH, null, DEFAULT_SUGGEST_LIMIT)
            .size());

    List<SuggestDto> dtos =
        vocabularyMapper.suggest(
            "label", LanguageRegion.SPANISH, LanguageRegion.ENGLISH, DEFAULT_SUGGEST_LIMIT);
    assertEquals(2, dtos.size());
    dtos.forEach(
        dto -> {
          if (dto.getName().equals(v2.getName())) {
            assertEquals(LanguageRegion.ENGLISH, dto.getLabelLang());
          } else {
            assertEquals(LanguageRegion.SPANISH, dto.getLabelLang());
          }
        });

    // test order
    assertEquals(
        v2.getKey(),
        vocabularyMapper.suggest("Label", LanguageRegion.ENGLISH, null, 1).get(0).getKey());

    assertEquals(
        v2.getKey(),
        vocabularyMapper.suggest("TILDE", LanguageRegion.PORTUGUESE, null, 1).get(0).getKey());

    assertEquals(
        v2.getKey(),
        vocabularyMapper.suggest("Tílde", LanguageRegion.PORTUGUESE, null, 1).get(0).getKey());
  }

  @Test
  public void findSimilaritiesTest() {
    Vocabulary vocabulary1 = createNewEntity();
    vocabularyMapper.create(vocabulary1);

    // check name
    List<KeyNameResult> similarities =
        vocabularyMapper.findSimilarities(normalizeName(vocabulary1.getName()), null);
    assertEquals(1, similarities.size());
    assertSimilarity(similarities, vocabulary1);

    // create another vocabulary
    Vocabulary vocabulary2 = createNewEntity();
    vocabulary2.setName("AnotherVocab");
    vocabularyMapper.create(vocabulary2);

    // check with multiple labels
    similarities = vocabularyMapper.findSimilarities(normalizeName("Another Vocab"), null);
    assertEquals(1, similarities.size());
    assertSimilarity(similarities, vocabulary2);

    similarities = vocabularyMapper.findSimilarities(normalizeName("new Vocab"), null);
    assertEquals(0, similarities.size());
  }

  private void assertSimilarity(List<KeyNameResult> similarities, Vocabulary vocabulary) {
    assertEquals(1, similarities.size());
    assertEquals(vocabulary.getKey(), similarities.get(0).getKey());
    assertEquals(vocabulary.getName(), similarities.get(0).getName());
  }

  private void assertList(VocabularySearchParams params, int expectedResult) {
    assertEquals(expectedResult, vocabularyMapper.list(params, DEFAULT_PAGE).size());
    assertEquals(expectedResult, vocabularyMapper.count(params));
  }

  @Test
  public void getByNameTest() {
    Vocabulary vocabulary1 = createNewEntity();
    vocabularyMapper.create(vocabulary1);

    Vocabulary vocabularyDB = vocabularyMapper.getByName(vocabulary1.getName());
    assertEquals(vocabulary1.getKey(), vocabularyDB.getKey());
  }

  @Test
  public void deleteVocabularyTest() {
    Vocabulary vocabulary1 = createNewEntity();
    vocabularyMapper.create(vocabulary1);

    List<Vocabulary> vocabs =
        vocabularyMapper.list(VocabularySearchParams.builder().build(), DEFAULT_PAGE);
    assertEquals(1, vocabs.size());

    vocabularyMapper.delete(vocabulary1.getKey());
    vocabs = vocabularyMapper.list(VocabularySearchParams.builder().build(), DEFAULT_PAGE);
    assertEquals(0, vocabs.size());
  }

  @Test
  public void definitionTest() {
    Vocabulary vocabulary = createNewEntity();
    vocabularyMapper.create(vocabulary);

    Definition definition =
        Definition.builder()
            .language(LanguageRegion.ENGLISH)
            .createdBy("test")
            .modifiedBy("test")
            .value("test")
            .build();
    vocabularyMapper.addDefinition(vocabulary.getKey(), definition);

    List<Definition> definitions = vocabularyMapper.listDefinitions(vocabulary.getKey(), null);
    assertEquals(1, definitions.size());

    assertEquals(
        1,
        vocabularyMapper
            .listDefinitions(vocabulary.getKey(), Collections.singletonList(LanguageRegion.ENGLISH))
            .size());
    assertEquals(
        0,
        vocabularyMapper
            .listDefinitions(vocabulary.getKey(), Collections.singletonList(LanguageRegion.SPANISH))
            .size());

    definition = vocabularyMapper.getDefinition(vocabulary.getKey(), definition.getKey());
    assertEquals("test", definition.getValue());
    assertEquals(LanguageRegion.ENGLISH, definition.getLanguage());
    assertEquals("test", definition.getCreatedBy());
    assertEquals("test", definition.getModifiedBy());
    assertNotNull(definition.getCreated());
    assertNotNull(definition.getModified());

    definition.setValue("test2");
    vocabularyMapper.updateDefinition(vocabulary.getKey(), definition);
    definition = vocabularyMapper.getDefinition(vocabulary.getKey(), definition.getKey());
    assertEquals("test2", definition.getValue());

    vocabularyMapper.deleteDefinition(vocabulary.getKey(), definition.getKey());

    definitions = vocabularyMapper.listDefinitions(vocabulary.getKey(), null);
    assertEquals(0, definitions.size());
  }

  @Test
  public void labelsTest() {
    Vocabulary vocabulary = createNewEntity();
    vocabularyMapper.create(vocabulary);

    Label label =
        Label.builder().language(LanguageRegion.ENGLISH).createdBy("test").value("test").build();
    vocabularyMapper.addLabel(vocabulary.getKey(), label);

    List<Label> labels = vocabularyMapper.listLabels(vocabulary.getKey(), null);
    assertEquals(1, labels.size());
    label = labels.get(0);
    assertEquals("test", label.getValue());
    assertEquals(LanguageRegion.ENGLISH, label.getLanguage());
    assertEquals("test", label.getCreatedBy());
    assertNotNull(label.getCreated());

    assertEquals(
        1,
        vocabularyMapper
            .listLabels(vocabulary.getKey(), Collections.singletonList(LanguageRegion.ENGLISH))
            .size());
    assertEquals(
        0,
        vocabularyMapper
            .listLabels(vocabulary.getKey(), Collections.singletonList(LanguageRegion.SPANISH))
            .size());

    vocabularyMapper.deleteLabel(vocabulary.getKey(), label.getKey());

    labels = vocabularyMapper.listLabels(vocabulary.getKey(), null);
    assertEquals(0, labels.size());
  }

  @Override
  Vocabulary createNewEntity() {
    Vocabulary entity = new Vocabulary();
    entity.setName(TestUtils.getRandomName());
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
   * <p>NOTE: this initializer cannot be in the base class because it gets executed only once and
   * provokes errors.
   */
  static class ContextInitializer
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
