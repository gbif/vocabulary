/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.persistence.parameters.NormalizedValuesParam;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.gbif.vocabulary.TestUtils.DEFAULT_PAGE;
import static org.gbif.vocabulary.TestUtils.DEPRECATED_BY;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeLabel;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeName;
import static org.gbif.vocabulary.persistence.parameters.NormalizedValuesParam.NAME_NODE;
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

  private final VocabularyMapper vocabularyMapper;

  @Autowired
  VocabularyMapperTest(VocabularyMapper vocabularyMapper) {
    super(vocabularyMapper);
    this.vocabularyMapper = vocabularyMapper;
  }

  @Test
  public void listVocabulariesTest() {
    Vocabulary vocabulary1 = createNewEntity();
    vocabulary1.setName("vocab1");
    vocabulary1.setNamespace("namespace1");
    vocabularyMapper.create(vocabulary1);

    Vocabulary vocabulary2 = createNewEntity();
    vocabulary2.setName("vocab2");
    vocabulary2.setNamespace("namespace2");
    vocabularyMapper.create(vocabulary2);

    Vocabulary vocabularyGbif = createNewEntity();
    vocabularyGbif.setName("vocab gbif");
    vocabularyGbif.setNamespace("namespace gbif");
    vocabularyMapper.create(vocabularyGbif);

    assertList(VocabularySearchParams.builder().query("vocab1").key(Long.MAX_VALUE).build(), 0);
    assertList(
        VocabularySearchParams.builder().query("vocab1").key(vocabulary1.getKey()).build(), 1);
    assertList(VocabularySearchParams.builder().key(vocabulary2.getKey()).build(), 1);
    assertList(VocabularySearchParams.builder().query("voc").build(), 3);
    assertList(VocabularySearchParams.builder().query("ocab").build(), 0);
    assertList(VocabularySearchParams.builder().query("namesp gb").build(), 1);
    assertList(VocabularySearchParams.builder().name("vocab1").build(), 1);
    assertList(VocabularySearchParams.builder().name("vocab").build(), 0);
    assertList(VocabularySearchParams.builder().namespace("namespace gbif").build(), 1);
    assertList(VocabularySearchParams.builder().namespace("namespace").build(), 0);
    assertList(VocabularySearchParams.builder().name("vocab2").namespace("namespace2").build(), 1);
    assertList(
        VocabularySearchParams.builder()
            .query("voc")
            .name("vocab1")
            .namespace("namespace1")
            .build(),
        1);
    assertList(
        VocabularySearchParams.builder()
            .query("oca")
            .name("vocab2")
            .namespace("namespace2")
            .build(),
        0);
    assertList(VocabularySearchParams.builder().query("v gbif").name("vocab gbif").build(), 1);
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
    v1.setName("suggest111");
    vocabularyMapper.create(v1);
    assertNotNull(v1.getKey());

    Vocabulary v2 = createNewEntity();
    v2.setName("suggest222");
    vocabularyMapper.create(v2);
    assertNotNull(v2.getKey());

    // check result values
    List<KeyNameResult> result = vocabularyMapper.suggest("suggest1");
    assertEquals("suggest111", result.get(0).getName());
    assertEquals(v1.getKey().intValue(), result.get(0).getKey());

    // assert expected number of results
    assertEquals(2, vocabularyMapper.suggest("su").size());
    assertEquals(2, vocabularyMapper.suggest("gge").size());
    assertEquals(1, vocabularyMapper.suggest("22").size());
    assertEquals(0, vocabularyMapper.suggest("zz").size());
    assertEquals(0, vocabularyMapper.suggest(null).size());
  }

  @Test
  public void findSimilaritiesTest() {
    Vocabulary vocabulary1 = createNewEntity();
    vocabulary1.setLabel(Collections.singletonMap(LanguageRegion.SPANISH, "igual"));
    vocabularyMapper.create(vocabulary1);

    // check Spanish labels
    NormalizedValuesParam spanishValues =
        NormalizedValuesParam.from(
            LanguageRegion.SPANISH.getLocale(), Arrays.asList("igual", "foo"));

    List<KeyNameResult> similarities =
        vocabularyMapper.findSimilarities(Collections.singletonList(spanishValues), null);
    assertEquals(1, similarities.size());
    assertSimilarity(similarities, vocabulary1);

    spanishValues.setValues(Arrays.asList("foo", "bar"));
    assertEquals(
        0,
        vocabularyMapper.findSimilarities(Collections.singletonList(spanishValues), null).size());

    // check name
    NormalizedValuesParam namesValues =
        NormalizedValuesParam.from(
            NAME_NODE, Collections.singletonList(normalizeName(vocabulary1.getName())));

    similarities = vocabularyMapper.findSimilarities(Collections.singletonList(namesValues), null);
    assertEquals(1, similarities.size());
    assertSimilarity(similarities, vocabulary1);

    // create another vocabulary
    Vocabulary vocabulary2 = createNewEntity();
    vocabulary2.setName("another-vocab");
    vocabulary2.setLabel(Collections.singletonMap(LanguageRegion.ENGLISH, "another label "));
    vocabularyMapper.create(vocabulary2);

    // check with multiple labels
    spanishValues.setValues(Collections.singletonList("igual"));
    namesValues.setValues(Collections.singletonList(normalizeName("Another Vocab")));
    similarities =
        vocabularyMapper.findSimilarities(Arrays.asList(spanishValues, namesValues), null);
    assertEquals(2, similarities.size());

    namesValues.setValues(Collections.singletonList(normalizeName("new Vocab")));
    similarities =
        vocabularyMapper.findSimilarities(Arrays.asList(spanishValues, namesValues), null);
    assertEquals(1, similarities.size());
    assertSimilarity(similarities, vocabulary1);

    similarities =
        vocabularyMapper.findSimilarities(
            Arrays.asList(spanishValues, namesValues), vocabulary1.getKey());
    assertEquals(0, similarities.size());

    spanishValues.setNode(LanguageRegion.ENGLISH.getLocale());
    similarities =
        vocabularyMapper.findSimilarities(Arrays.asList(spanishValues, namesValues), null);
    assertEquals(0, similarities.size());

    NormalizedValuesParam englishValues =
        NormalizedValuesParam.from(
            LanguageRegion.ENGLISH.getLocale(),
            Collections.singletonList(normalizeLabel("another label")));
    assertEquals(
        1,
        vocabularyMapper.findSimilarities(Collections.singletonList(englishValues), null).size());

    // remove labels and there shouldn't be similarities in labels
    vocabulary2 = vocabularyMapper.get(vocabulary2.getKey());
    vocabulary2.setLabel(null);
    vocabularyMapper.update(vocabulary2);

    assertEquals(
        0,
        vocabularyMapper.findSimilarities(Collections.singletonList(englishValues), null).size());
  }

  private void assertSimilarity(List<KeyNameResult> similarities, Vocabulary vocabulary) {
    assertEquals(1, similarities.size());
    assertEquals(vocabulary.getKey().intValue(), similarities.get(0).getKey());
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

  @Override
  Vocabulary createNewEntity() {
    Vocabulary entity = new Vocabulary();
    entity.setName(UUID.randomUUID().toString());
    entity.setLabel(new HashMap<>(Collections.singletonMap(LanguageRegion.ENGLISH, "Label")));
    entity.setDefinition(
        new HashMap<>(Collections.singletonMap(LanguageRegion.ENGLISH, "Definition")));
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
