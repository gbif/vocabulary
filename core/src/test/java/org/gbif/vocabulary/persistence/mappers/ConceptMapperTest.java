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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.TestUtils;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Definition;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.ChildrenResult;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.persistence.dto.SuggestDto;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.gbif.vocabulary.TestUtils.DEFAULT_PAGE;
import static org.gbif.vocabulary.TestUtils.DEPRECATED_BY;
import static org.gbif.vocabulary.TestUtils.PAGE_FN;
import static org.gbif.vocabulary.TestUtils.assertNotDeprecated;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeLabel;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeName;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@link ConceptMapper} class
 *
 * <p>It uses a embedded PostgreSQL provided by {@link PostgreSQLContainer} which is started before
 * the tests run and it's reused by all the tests.
 */
@ContextConfiguration(initializers = {ConceptMapperTest.ContextInitializer.class})
public class ConceptMapperTest extends BaseMapperTest<Concept> {

  private static final String DEFAULT_VOCABULARY = "Default";

  private static final Vocabulary[] vocabularies = new Vocabulary[2];

  private final ConceptMapper conceptMapper;
  private final TagMapper tagMapper;

  @Autowired
  ConceptMapperTest(ConceptMapper conceptMapper, TagMapper tagMapper) {
    super(conceptMapper);
    this.conceptMapper = conceptMapper;
    this.tagMapper = tagMapper;
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
    vocabularies[0] = vocabulary;

    Vocabulary vocabulary2 = new Vocabulary();
    vocabulary2.setName("Default2");
    vocabulary2.setCreatedBy("test");
    vocabulary2.setModifiedBy("test");
    vocabularyMapper.create(vocabulary2);
    vocabularies[1] = vocabulary2;
  }

  @Test
  public void listConceptsTest() {
    Concept concept1 = createNewEntity();
    concept1.setName("Concept1");
    conceptMapper.create(concept1);

    conceptMapper.addAlternativeLabel(
        concept1.getKey(),
        Label.builder()
            .language(LanguageRegion.ENGLISH)
            .value("alternative example")
            .createdBy("test")
            .build());

    Concept concept2 = createNewEntity();
    concept2.setName("Concept2");
    concept2.setParentKey(concept1.getKey());
    conceptMapper.create(concept2);

    HiddenLabel hiddenLabel =
        HiddenLabel.builder().value("misspelt example").createdBy("test").build();
    conceptMapper.addHiddenLabel(concept2.getKey(), hiddenLabel);

    Concept concept3 = createNewEntity();
    concept3.setName("Concept3");
    concept3.setParentKey(concept1.getKey());
    concept3.setEditorialNotes(Collections.singletonList("editorial notes"));
    conceptMapper.create(concept3);
    concept3.setReplacedByKey(concept2.getKey());
    conceptMapper.update(concept3);

    // we create a release view to test it at the same time
    conceptMapper.createLatestReleaseView(
        DEFAULT_VOCABULARY.toLowerCase(), vocabularies[0].getKey());

    assertList(ConceptSearchParams.builder().query("concept1").key(concept1.getKey()).build(), 1);
    assertList(ConceptSearchParams.builder().query("misspelt").build(), 1);
    assertList(ConceptSearchParams.builder().query("(concept1)").key(concept1.getKey()).build(), 1);
    assertList(ConceptSearchParams.builder().query("concept1").key(Long.MAX_VALUE).build(), 0);
    assertList(ConceptSearchParams.builder().key(concept1.getKey()).build(), 1);
    assertList(ConceptSearchParams.builder().query("conc").build(), 3);
    assertList(ConceptSearchParams.builder().query("example").build(), 2);
    assertList(ConceptSearchParams.builder().query("altern ex").build(), 1);
    assertList(ConceptSearchParams.builder().query("oncept").build(), 0);
    assertList(ConceptSearchParams.builder().vocabularyKey(vocabularies[0].getKey()).build(), 3);
    assertList(ConceptSearchParams.builder().parentKey(concept1.getKey()).build(), 2);
    assertList(ConceptSearchParams.builder().parent(concept1.getName()).build(), 2);
    assertList(ConceptSearchParams.builder().parentKey(concept2.getKey()).build(), 0);
    assertList(ConceptSearchParams.builder().parent(concept2.getName()).build(), 0);
    assertList(ConceptSearchParams.builder().name("Concept1").build(), 1);
    assertList(ConceptSearchParams.builder().name("Concepto").build(), 0);
    assertList(
        ConceptSearchParams.builder().query("exa").vocabularyKey(vocabularies[0].getKey()).build(),
        2);
    assertList(
        ConceptSearchParams.builder().parentKey(concept1.getKey()).name("Concept3").build(), 1);
    assertList(ConceptSearchParams.builder().replacedByKey(concept2.getKey()).build(), 1);
    assertList(ConceptSearchParams.builder().hasParent(true).build(), 2);
    assertList(ConceptSearchParams.builder().hasReplacement(true).build(), 1);
    assertList(ConceptSearchParams.builder().hiddenLabel(hiddenLabel.getValue()).build(), 1);

    // remove the hidden label. The release view shouldn't get the change
    conceptMapper.deleteHiddenLabel(concept2.getKey(), hiddenLabel.getKey());

    ConceptSearchParams searchParams = ConceptSearchParams.builder().query("misspelt").build();
    assertEquals(0, conceptMapper.list(searchParams, DEFAULT_PAGE).size());
    assertEquals(0, conceptMapper.count(searchParams));
    assertEquals(
        1,
        conceptMapper
            .listLatestRelease(searchParams, DEFAULT_PAGE, DEFAULT_VOCABULARY.toLowerCase())
            .size());
    assertEquals(
        1, conceptMapper.countLatestRelease(searchParams, DEFAULT_VOCABULARY.toLowerCase()));

    // test that the release view gets updated
    conceptMapper.updateReleaseViews(DEFAULT_VOCABULARY.toLowerCase());
    assertEquals(
        0,
        conceptMapper
            .listLatestRelease(searchParams, DEFAULT_PAGE, DEFAULT_VOCABULARY.toLowerCase())
            .size());
    assertEquals(
        0, conceptMapper.countLatestRelease(searchParams, DEFAULT_VOCABULARY.toLowerCase()));
  }

  @Test
  public void suggestTest() {
    // create entities for the test
    Concept c1 = createNewEntity();
    c1.setName("Suggest111");

    conceptMapper.create(c1);
    assertNotNull(c1.getKey());

    conceptMapper.addLabel(
        c1.getKey(),
        Label.builder()
            .language(LanguageRegion.ENGLISH)
            .value("labelenglish")
            .createdBy("test")
            .build());

    conceptMapper.addLabel(
        c1.getKey(),
        Label.builder()
            .language(LanguageRegion.SPANISH)
            .value("labelspanish")
            .createdBy("test")
            .build());

    conceptMapper.addHiddenLabel(
        c1.getKey(), HiddenLabel.builder().value("lab,l").createdBy("test").build());
    conceptMapper.addHiddenLabel(
        c1.getKey(), HiddenLabel.builder().value("lbel").createdBy("test").build());

    Concept c2 = createNewEntity();
    c2.setName("Suggest222");

    conceptMapper.create(c2);
    assertNotNull(c2.getKey());

    conceptMapper.addLabel(
        c2.getKey(),
        Label.builder().language(LanguageRegion.ENGLISH).value("Label").createdBy("test").build());

    conceptMapper.addHiddenLabel(
        c2.getKey(), HiddenLabel.builder().value("lab,l").createdBy("test").build());
    conceptMapper.addHiddenLabel(
        c2.getKey(), HiddenLabel.builder().value("lbel").createdBy("test").build());

    // check result values
    List<SuggestDto> result = conceptMapper.suggest("suggest1", c1.getVocabularyKey(), null, null);
    assertEquals("Suggest111", result.get(0).getName());
    assertEquals(c1.getKey().intValue(), result.get(0).getKey());

    // create release view to test it at the same time
    conceptMapper.createLatestReleaseView(
        DEFAULT_VOCABULARY.toLowerCase(), vocabularies[0].getKey());

    // assert expected number of results
    assertSuggest(2, "su", c1.getVocabularyKey(), null, null);
    assertSuggest(2, "gge", c1.getVocabularyKey(), null, null);
    assertSuggest(1, "22", c1.getVocabularyKey(), null, null);
    assertSuggest(0, "zz", c1.getVocabularyKey(), null, null);
    assertSuggest(0, null, c1.getVocabularyKey(), null, null);
    assertSuggest(2, "label", c1.getVocabularyKey(), null, null);
    assertSuggest(1, "labeleng", c1.getVocabularyKey(), null, null);
    assertSuggest(0, "labeleng", c1.getVocabularyKey(), LanguageRegion.SPANISH, null);
    assertSuggest(1, "labeleng", c1.getVocabularyKey(), LanguageRegion.ENGLISH, null);
    // should get fallback lang
    assertSuggest(2, "su", c1.getVocabularyKey(), LanguageRegion.ARPITAN, LanguageRegion.ENGLISH);
    assertSuggest(2, "su", c1.getVocabularyKey(), LanguageRegion.ARPITAN, LanguageRegion.SPANISH);
    assertSuggest(2, "su", c1.getVocabularyKey(), LanguageRegion.ARPITAN, LanguageRegion.AFRIKAANS);

    Concept c3 = createNewEntity();
    c3.setVocabularyKey(vocabularies[1].getKey());
    c3.setName("Suggest333");
    conceptMapper.create(c3);
    assertNotNull(c3.getKey());

    assertEquals(2, conceptMapper.suggest("su", c1.getVocabularyKey(), null, null).size());
    assertEquals(1, conceptMapper.suggest("su", c3.getVocabularyKey(), null, null).size());
    assertEquals(1, conceptMapper.suggest("33", c3.getVocabularyKey(), null, null).size());
    assertEquals(0, conceptMapper.suggest("33", c1.getVocabularyKey(), null, null).size());
  }

  private void assertSuggest(
      int expectedSize,
      String query,
      long vocabKey,
      LanguageRegion lang,
      LanguageRegion fallbackLang) {
    List<SuggestDto> result = conceptMapper.suggest(query, vocabKey, lang, fallbackLang);
    assertEquals(expectedSize, result.size());
    List<SuggestDto> resultRelease =
        conceptMapper.suggestLatestRelease(query, vocabKey, lang, fallbackLang, DEFAULT_VOCABULARY);
    assertEquals(expectedSize, resultRelease.size());

    if (lang != null) {
      assertTrue(
          result.stream()
              .allMatch(
                  r ->
                      r.getLabels() == null
                          || r.getLabels().isEmpty()
                          || r.getLabels().stream()
                              .allMatch(
                                  l ->
                                      l.getLanguage() == lang || l.getLanguage() == fallbackLang)));
    }
  }

  @Test
  public void findSimilaritiesTest() {
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);

    conceptMapper.addLabel(
        concept1.getKey(),
        Label.builder()
            .language(LanguageRegion.SPANISH)
            .value("primero ")
            .createdBy("test")
            .build());

    HiddenLabel hiddenLabel1 = HiddenLabel.builder().value("primeiro").createdBy("test").build();
    conceptMapper.addHiddenLabel(concept1.getKey(), hiddenLabel1);

    HiddenLabel hiddenLabel2 =
        HiddenLabel.builder().value("otro primeiro").createdBy("test").build();
    conceptMapper.addHiddenLabel(concept1.getKey(), hiddenLabel2);

    // check hidden labels
    List<KeyNameResult> similarities =
        conceptMapper.findSimilarities(
            normalizeLabel("primeiro"), null, concept1.getVocabularyKey(), null);
    assertSimilarity(similarities, concept1);

    // remove hidden labels and there shouldn't be similarities
    concept1 = conceptMapper.get(concept1.getKey());
    conceptMapper.deleteHiddenLabel(concept1.getKey(), hiddenLabel1.getKey());
    conceptMapper.deleteHiddenLabel(concept1.getKey(), hiddenLabel2.getKey());

    conceptMapper.update(concept1);
    assertEquals(
        0,
        conceptMapper
            .findSimilarities(
                normalizeLabel(hiddenLabel1.getValue()), null, concept1.getVocabularyKey(), null)
            .size());

    similarities =
        conceptMapper.findSimilarities(
            normalizeLabel("Primero"), LanguageRegion.SPANISH, concept1.getVocabularyKey(), null);
    assertSimilarity(similarities, concept1);

    similarities =
        conceptMapper.findSimilarities(
            normalizeName(concept1.getName()), null, concept1.getVocabularyKey(), null);
    assertSimilarity(similarities, concept1);

    similarities =
        conceptMapper.findSimilarities(
            normalizeLabel("foo"), null, concept1.getVocabularyKey(), null);
    assertEquals(0, similarities.size());

    // for another vocabulary there should be no match
    similarities =
        conceptMapper.findSimilarities(
            normalizeLabel("Primero"), LanguageRegion.SPANISH, 200, null);
    assertEquals(0, similarities.size());

    // for the same concept there should be no matches
    similarities =
        conceptMapper.findSimilarities(
            normalizeLabel("primero"),
            LanguageRegion.SPANISH,
            concept1.getVocabularyKey(),
            concept1.getKey());
    assertEquals(0, similarities.size());

    // for other LanguageRegion there should be no matches
    similarities =
        conceptMapper.findSimilarities(
            normalizeLabel("primero"),
            LanguageRegion.ENGLISH,
            concept1.getVocabularyKey(),
            concept1.getKey());
    assertEquals(0, similarities.size());
  }

  @Test
  public void findSimilaritiesNormalizationTest() {
    Concept concept1 = createNewEntity();
    concept1.setName("MyConcept");
    conceptMapper.create(concept1);

    conceptMapper.addLabel(
        concept1.getKey(),
        Label.builder()
            .language(LanguageRegion.ENGLISH)
            .value("normalization")
            .createdBy("test")
            .build());

    conceptMapper.addHiddenLabel(
        concept1.getKey(), HiddenLabel.builder().value("norm").createdBy("test").build());
    conceptMapper.addHiddenLabel(
        concept1.getKey(), HiddenLabel.builder().value("another norm").createdBy("test").build());

    // check English labels
    List<KeyNameResult> similarities =
        conceptMapper.findSimilarities(
            normalizeLabel(" normaLiZA tion  "),
            LanguageRegion.ENGLISH,
            concept1.getVocabularyKey(),
            null);
    assertSimilarity(similarities, concept1);

    similarities =
        conceptMapper.findSimilarities(
            normalizeLabel(" aNotHer  NORM  "), null, concept1.getVocabularyKey(), null);
    assertSimilarity(similarities, concept1);
    similarities =
        conceptMapper.findSimilarities(
            normalizeLabel("segundo"), null, concept1.getVocabularyKey(), null);
    assertEquals(0, similarities.size());

    similarities =
        conceptMapper.findSimilarities(
            normalizeName("My Concept"), null, concept1.getVocabularyKey(), null);
    assertSimilarity(similarities, concept1);
  }

  @Test
  public void findSimilaritiesMultipleParamsTest() {
    Concept concept1 = createNewEntity();
    concept1.setName("C1");
    conceptMapper.create(concept1);

    conceptMapper.addLabel(
        concept1.getKey(),
        Label.builder().language(LanguageRegion.ENGLISH).value("l1").createdBy("test").build());

    conceptMapper.addAlternativeLabel(
        concept1.getKey(),
        Label.builder().language(LanguageRegion.SPANISH).value("l uno").createdBy("test").build());

    conceptMapper.addHiddenLabel(
        concept1.getKey(), HiddenLabel.builder().value("ll1").createdBy("test").build());
    conceptMapper.addHiddenLabel(
        concept1.getKey(), HiddenLabel.builder().value("l1l").createdBy("test").build());

    Concept concept2 = createNewEntity();
    concept2.setName("C2");
    conceptMapper.create(concept2);

    conceptMapper.addLabel(
        concept2.getKey(),
        Label.builder().language(LanguageRegion.ENGLISH).value("l2").createdBy("test").build());

    conceptMapper.addHiddenLabel(
        concept2.getKey(), HiddenLabel.builder().value("ll2").createdBy("test").build());
    conceptMapper.addHiddenLabel(
        concept2.getKey(), HiddenLabel.builder().value("l2l").createdBy("test").build());

    // check Spanish labels
    List<KeyNameResult> similarities =
        conceptMapper.findSimilarities(
            normalizeLabel("l UNo"), LanguageRegion.SPANISH, concept1.getVocabularyKey(), null);
    assertEquals(1, similarities.size());

    similarities =
        conceptMapper.findSimilarities(
            normalizeLabel("l2"), LanguageRegion.ENGLISH, concept1.getVocabularyKey(), null);
    assertEquals(1, similarities.size());

    similarities =
        conceptMapper.findSimilarities(
            normalizeLabel("l2"), LanguageRegion.ITALIAN, concept1.getVocabularyKey(), null);
    assertEquals(0, similarities.size());

    similarities =
        conceptMapper.findSimilarities(
            normalizeLabel("LL1"), null, concept1.getVocabularyKey(), null);
    assertEquals(1, similarities.size());
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
    assertEquals(
        2, conceptMapper.list(ConceptSearchParams.builder().deprecated(true).build(), null).size());

    // undeprecate in bulk
    conceptMapper.restoreDeprecatedInBulk(Arrays.asList(concept1.getKey(), concept2.getKey()));
    assertEquals(
        0, conceptMapper.list(ConceptSearchParams.builder().deprecated(true).build(), null).size());
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
    conceptMapper.restoreDeprecated(concept2.getKey());
    conceptMapper.deprecate(concept2.getKey(), DEPRECATED_BY, null);
    assertNull(conceptMapper.findReplacement(concept3.getKey()));

    // concept 3 deprecated by concept 1
    conceptMapper.restoreDeprecated(concept3.getKey());
    conceptMapper.deprecate(concept3.getKey(), DEPRECATED_BY, concept1.getKey());
    assertEquals(concept1.getKey(), conceptMapper.findReplacement(concept3.getKey()));

    // concept 3 deprecated without replacement
    conceptMapper.restoreDeprecated(concept3.getKey());
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
    assertEquals(
        vocabularies[0].getKey(), conceptMapper.getVocabularyKey(concept1.getKey()).intValue());
  }

  @Test
  public void getByNameAndVocabularyTest() {
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);

    Concept conceptDB =
        conceptMapper.getByNameAndVocabulary(concept1.getName(), DEFAULT_VOCABULARY);
    assertEquals(concept1.getKey(), conceptDB.getKey());
  }

  @Test
  public void findParentsTest() {
    // just created, doesn't have parent
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);
    // create release view to test it at the same time
    conceptMapper.createLatestReleaseView(
        DEFAULT_VOCABULARY.toLowerCase(), vocabularies[0].getKey());

    assertTrue(conceptMapper.findParents(concept1.getKey()).isEmpty());
    assertTrue(
        conceptMapper
            .findParentsLatestRelease(concept1.getKey(), DEFAULT_VOCABULARY.toLowerCase())
            .isEmpty());

    // concept2 will have concept1 as parent
    Concept concept2 = createNewEntity();
    concept2.setParentKey(concept1.getKey());
    conceptMapper.create(concept2);
    conceptMapper.updateReleaseViews(DEFAULT_VOCABULARY.toLowerCase());

    List<String> parents = conceptMapper.findParents(concept2.getKey());
    assertEquals(1, parents.size());
    assertEquals(concept1.getName(), parents.get(0));
    List<String> parentsRelease =
        conceptMapper.findParentsLatestRelease(concept2.getKey(), DEFAULT_VOCABULARY.toLowerCase());
    assertEquals(1, parentsRelease.size());
    assertEquals(concept1.getName(), parentsRelease.get(0));

    // concept3 will have concept2 as parent
    Concept concept3 = createNewEntity();
    concept3.setParentKey(concept2.getKey());
    conceptMapper.create(concept3);
    conceptMapper.updateReleaseViews(DEFAULT_VOCABULARY.toLowerCase());

    parents = conceptMapper.findParents(concept3.getKey());
    assertEquals(2, parents.size());
    assertTrue(parents.contains(concept1.getName()));
    assertTrue(parents.contains(concept2.getName()));
    parentsRelease =
        conceptMapper.findParentsLatestRelease(concept3.getKey(), DEFAULT_VOCABULARY.toLowerCase());
    assertEquals(2, parentsRelease.size());
    assertTrue(parentsRelease.contains(concept1.getName()));
    assertTrue(parentsRelease.contains(concept2.getName()));
  }

  @Test
  public void countChildrenTest() {
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);
    // create release view to test it at the same time
    conceptMapper.createLatestReleaseView(
        DEFAULT_VOCABULARY.toLowerCase(), vocabularies[0].getKey());

    assertTrue(conceptMapper.findParents(concept1.getKey()).isEmpty());
    assertTrue(
        conceptMapper
            .findParentsLatestRelease(concept1.getKey(), DEFAULT_VOCABULARY.toLowerCase())
            .isEmpty());

    Concept concept2 = createNewEntity();
    concept2.setParentKey(concept1.getKey());
    conceptMapper.create(concept2);

    Concept concept3 = createNewEntity();
    concept3.setParentKey(concept1.getKey());
    conceptMapper.create(concept3);

    Concept concept4 = createNewEntity();
    concept4.setParentKey(concept3.getKey());
    conceptMapper.create(concept4);

    conceptMapper.updateReleaseViews(DEFAULT_VOCABULARY.toLowerCase());

    Consumer<List<ChildrenResult>> assertCounts =
        counts -> {
          assertEquals(3, counts.size());
          assertTrue(counts.contains(new ChildrenResult(concept1.getKey(), concept2.getName())));
          assertTrue(counts.contains(new ChildrenResult(concept1.getKey(), concept3.getName())));
          assertTrue(counts.contains(new ChildrenResult(concept3.getKey(), concept4.getName())));
        };

    List<ChildrenResult> counts =
        conceptMapper.countChildren(
            Arrays.asList(
                concept1.getKey(), concept2.getKey(), concept3.getKey(), concept4.getKey()));
    assertCounts.accept(counts);

    List<ChildrenResult> countsLatestRelease =
        conceptMapper.countChildrenLatestRelease(
            Arrays.asList(
                concept1.getKey(), concept2.getKey(), concept3.getKey(), concept4.getKey()),
            DEFAULT_VOCABULARY.toLowerCase());
    assertCounts.accept(countsLatestRelease);
  }

  @Test
  public void tagsTest() {
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);

    Concept concept2 = createNewEntity();
    conceptMapper.create(concept2);

    Tag tag = new Tag();
    tag.setName("tag");
    tag.setColor("#FFFFFF");
    tag.setCreatedBy("test");
    tag.setModifiedBy("test");
    tagMapper.create(tag);

    Tag tag2 = new Tag();
    tag2.setName("tag2");
    tag2.setColor("#FFFFFF");
    tag2.setCreatedBy("test");
    tag2.setModifiedBy("test");
    tagMapper.create(tag2);

    conceptMapper.addTag(concept1.getKey(), tag.getKey());
    conceptMapper.addTag(concept1.getKey(), tag2.getKey());
    conceptMapper.addTag(concept2.getKey(), tag.getKey());

    Concept conceptWithTags = conceptMapper.get(concept1.getKey());
    assertEquals(2, conceptWithTags.getTags().size());

    List<Concept> concepts =
        conceptMapper.list(
            ConceptSearchParams.builder().tags(Collections.singletonList(tag.getName())).build(),
            DEFAULT_PAGE);
    assertEquals(2, concepts.size());

    concepts =
        conceptMapper.list(
            ConceptSearchParams.builder().tags(Collections.singletonList(tag2.getName())).build(),
            DEFAULT_PAGE);
    assertEquals(1, concepts.size());

    concepts =
        conceptMapper.list(
            ConceptSearchParams.builder()
                .tags(Arrays.asList(tag.getName(), tag2.getName()))
                .build(),
            DEFAULT_PAGE);
    assertEquals(1, concepts.size());

    conceptMapper.removeTag(concept1.getKey(), tag2.getKey());
    concepts =
        conceptMapper.list(
            ConceptSearchParams.builder()
                .tags(Arrays.asList(tag.getName(), tag2.getName()))
                .build(),
            DEFAULT_PAGE);
    assertEquals(0, concepts.size());

    conceptMapper.addTag(concept1.getKey(), tag2.getKey());
    tagMapper.delete(tag2.getKey());
    concepts =
        conceptMapper.list(
            ConceptSearchParams.builder().tags(Collections.singletonList(tag2.getName())).build(),
            DEFAULT_PAGE);
    assertEquals(0, concepts.size());
  }

  @Test
  public void deleteAllConceptsTest() {
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);

    Concept concept2 = createNewEntity();
    conceptMapper.create(concept2);

    List<Concept> conceptsFound =
        conceptMapper.list(
            ConceptSearchParams.builder().vocabularyKey(vocabularies[0].getKey()).build(),
            DEFAULT_PAGE);
    assertEquals(2, conceptsFound.size());

    conceptMapper.deleteAllConcepts(vocabularies[0].getKey());
    conceptsFound =
        conceptMapper.list(
            ConceptSearchParams.builder().vocabularyKey(vocabularies[0].getKey()).build(),
            DEFAULT_PAGE);
    assertEquals(0, conceptsFound.size());
  }

  @Test
  public void definitionTest() {
    Concept concept = createNewEntity();
    conceptMapper.create(concept);

    Definition definition =
        Definition.builder()
            .language(LanguageRegion.ENGLISH)
            .value("test")
            .createdBy("test")
            .modifiedBy("test")
            .build();
    conceptMapper.addDefinition(concept.getKey(), definition);

    conceptMapper.createLatestReleaseView(
        DEFAULT_VOCABULARY.toLowerCase(), vocabularies[0].getKey());

    assertListDefinitions(1, concept.getKey(), null);
    assertListDefinitions(1, concept.getKey(), LanguageRegion.ENGLISH);
    assertListDefinitions(0, concept.getKey(), LanguageRegion.SPANISH);

    definition = conceptMapper.getDefinition(concept.getKey(), definition.getKey());
    assertEquals("test", definition.getValue());
    assertEquals(LanguageRegion.ENGLISH, definition.getLanguage());
    assertEquals("test", definition.getCreatedBy());
    assertEquals("test", definition.getModifiedBy());
    assertNotNull(definition.getCreated());
    assertNotNull(definition.getModified());

    definition.setValue("test2");
    conceptMapper.updateDefinition(concept.getKey(), definition);
    definition = conceptMapper.getDefinition(concept.getKey(), definition.getKey());
    assertEquals("test2", definition.getValue());

    conceptMapper.deleteDefinition(concept.getKey(), definition.getKey());
    conceptMapper.updateReleaseViews(DEFAULT_VOCABULARY.toLowerCase());
    assertListDefinitions(0, concept.getKey(), null);
  }

  private void assertListDefinitions(int expectedSize, long conceptKey, LanguageRegion lang) {
    List<LanguageRegion> langs = lang != null ? Collections.singletonList(lang) : null;
    assertEquals(expectedSize, conceptMapper.listDefinitions(conceptKey, langs).size());
    assertEquals(
        expectedSize,
        conceptMapper
            .listDefinitionsLatestRelease(conceptKey, langs, DEFAULT_VOCABULARY.toLowerCase())
            .size());
  }

  @Test
  public void labelsTest() {
    Concept concept = createNewEntity();
    conceptMapper.create(concept);

    Label label =
        Label.builder().language(LanguageRegion.ENGLISH).value("test").createdBy("test").build();
    conceptMapper.addLabel(concept.getKey(), label);

    conceptMapper.createLatestReleaseView(
        DEFAULT_VOCABULARY.toLowerCase(), vocabularies[0].getKey());

    Consumer<List<Label>> assertLabels =
        labs -> {
          assertEquals(1, labs.size());
          Label lab = labs.get(0);
          assertEquals("test", lab.getValue());
          assertEquals(LanguageRegion.ENGLISH, lab.getLanguage());
          assertEquals("test", lab.getCreatedBy());
          assertNotNull(lab.getCreated());
        };

    assertLabels.accept(conceptMapper.listLabels(concept.getKey(), null));
    assertLabels.accept(
        conceptMapper.listLabelsLatestRelease(
            concept.getKey(), null, DEFAULT_VOCABULARY.toLowerCase()));

    assertListLabels(1, concept.getKey(), LanguageRegion.ENGLISH);
    assertListLabels(0, concept.getKey(), LanguageRegion.SPANISH);

    conceptMapper.deleteLabel(concept.getKey(), label.getKey());
    conceptMapper.updateReleaseViews(DEFAULT_VOCABULARY.toLowerCase());
    assertListLabels(0, concept.getKey(), null);
  }

  private void assertListLabels(int expectedSize, long conceptKey, LanguageRegion lang) {
    List<LanguageRegion> langs = lang != null ? Collections.singletonList(lang) : null;
    assertEquals(expectedSize, conceptMapper.listLabels(conceptKey, langs).size());
    assertEquals(
        expectedSize,
        conceptMapper
            .listLabelsLatestRelease(conceptKey, langs, DEFAULT_VOCABULARY.toLowerCase())
            .size());
  }

  @Test
  public void alternativeLabelsTest() {
    Concept concept = createNewEntity();
    conceptMapper.create(concept);

    Label label =
        Label.builder().language(LanguageRegion.ENGLISH).value("test").createdBy("test").build();
    conceptMapper.addAlternativeLabel(concept.getKey(), label);
    conceptMapper.createLatestReleaseView(
        DEFAULT_VOCABULARY.toLowerCase(), vocabularies[0].getKey());

    Consumer<List<Label>> assertLabels =
        labs -> {
          assertEquals(1, labs.size());
          assertEquals(labs.size(), conceptMapper.countAlternativeLabels(concept.getKey(), null));
          Label lab = labs.get(0);
          assertEquals("test", lab.getValue());
          assertEquals(LanguageRegion.ENGLISH, lab.getLanguage());
          assertEquals("test", lab.getCreatedBy());
          assertNotNull(lab.getCreated());
        };

    assertLabels.accept(conceptMapper.listAlternativeLabels(concept.getKey(), null, DEFAULT_PAGE));
    assertLabels.accept(
        conceptMapper.listAlternativeLabelsLatestRelease(
            concept.getKey(), null, DEFAULT_PAGE, DEFAULT_VOCABULARY.toLowerCase()));

    assertListAlternativeLabels(1, concept.getKey(), LanguageRegion.ENGLISH, DEFAULT_PAGE);
    assertListAlternativeLabels(0, concept.getKey(), LanguageRegion.SPANISH, DEFAULT_PAGE);
    assertListAlternativeLabels(0, concept.getKey(), null, PAGE_FN.apply(0, 0L));
    assertListAlternativeLabels(1, concept.getKey(), null, DEFAULT_PAGE);

    conceptMapper.deleteAlternativeLabel(concept.getKey(), label.getKey());
    conceptMapper.updateReleaseViews(DEFAULT_VOCABULARY.toLowerCase());

    assertListAlternativeLabels(0, concept.getKey(), null, DEFAULT_PAGE);
  }

  private void assertListAlternativeLabels(
      int expectedSize, long conceptKey, LanguageRegion lang, Pageable page) {
    List<LanguageRegion> langs = lang != null ? Collections.singletonList(lang) : null;
    assertEquals(expectedSize, conceptMapper.listAlternativeLabels(conceptKey, langs, page).size());
    assertEquals(
        expectedSize,
        conceptMapper
            .listAlternativeLabelsLatestRelease(
                conceptKey, langs, page, DEFAULT_VOCABULARY.toLowerCase())
            .size());
    if (page.getLimit() > 0) {
      assertEquals(expectedSize, conceptMapper.countAlternativeLabels(conceptKey, langs));
      assertEquals(
          expectedSize,
          conceptMapper.countAlternativeLabelsLatestRelease(
              conceptKey, langs, DEFAULT_VOCABULARY.toLowerCase()));
    }
  }

  @Test
  public void hiddenLabelsTest() {
    Concept concept = createNewEntity();
    conceptMapper.create(concept);

    HiddenLabel label = HiddenLabel.builder().value("test").createdBy("test").build();
    conceptMapper.addHiddenLabel(concept.getKey(), label);
    conceptMapper.createLatestReleaseView(
        DEFAULT_VOCABULARY.toLowerCase(), vocabularies[0].getKey());

    Consumer<List<HiddenLabel>> assertLabels =
        labs -> {
          assertEquals(1, labs.size());
          assertEquals(labs.size(), conceptMapper.countHiddenLabels(concept.getKey()));
          HiddenLabel lab = labs.get(0);
          assertEquals("test", lab.getValue());
          assertEquals("test", lab.getCreatedBy());
          assertNotNull(lab.getCreated());
        };

    assertLabels.accept(conceptMapper.listHiddenLabels(concept.getKey(), DEFAULT_PAGE));

    assertListHiddenLabels(0, concept.getKey(), PAGE_FN.apply(0, 0L));
    assertListHiddenLabels(1, concept.getKey(), DEFAULT_PAGE);

    conceptMapper.deleteHiddenLabel(concept.getKey(), label.getKey());
    conceptMapper.updateReleaseViews(DEFAULT_VOCABULARY.toLowerCase());

    assertListHiddenLabels(0, concept.getKey(), DEFAULT_PAGE);
  }

  private void assertListHiddenLabels(int expectedSize, long conceptKey, Pageable page) {
    assertEquals(expectedSize, conceptMapper.listHiddenLabels(conceptKey, page).size());
    assertEquals(
        expectedSize,
        conceptMapper
            .listHiddenLabelsLatestRelease(conceptKey, page, DEFAULT_VOCABULARY.toLowerCase())
            .size());

    if (page.getLimit() > 0) {
      assertEquals(expectedSize, conceptMapper.countHiddenLabels(conceptKey));
      assertEquals(
          expectedSize,
          conceptMapper.countHiddenLabelsLatestRelease(
              conceptKey, DEFAULT_VOCABULARY.toLowerCase()));
    }
  }

  @Test
  public void releaseViewTest() {
    String vocabName = "life_stage_test";
    assertFalse(conceptMapper.existsReleaseView(vocabName));
    conceptMapper.createLatestReleaseView(vocabName, 1);
    assertDoesNotThrow(() -> conceptMapper.updateReleaseViews(vocabName));
    assertTrue(conceptMapper.existsReleaseView(vocabName));
  }

  @Test
  public void releaseViewSubqueriesTest() {
    String vocabName = "test";

    Concept concept1 = createNewEntity();
    concept1.setName("Concept1");
    conceptMapper.create(concept1);

    conceptMapper.addDefinition(
        concept1.getKey(),
        Definition.builder()
            .value("test")
            .language(LanguageRegion.ACHOLI)
            .createdBy("test")
            .modifiedBy("test")
            .build());

    conceptMapper.createLatestReleaseView(vocabName, 1);

    // we add more definitions and labels without updating the release view to make sure the
    // subqueries are done against the release materialized views
    conceptMapper.addDefinition(
        concept1.getKey(),
        Definition.builder()
            .value("test2")
            .language(LanguageRegion.SPANISH)
            .createdBy("test")
            .modifiedBy("test")
            .build());

    conceptMapper.addLabel(
        concept1.getKey(),
        Label.builder().value("l1").language(LanguageRegion.ACHOLI).createdBy("test").build());

    Concept c1Get = conceptMapper.getByNameLatestRelease(concept1.getName(), vocabName);
    assertEquals(1, c1Get.getDefinition().size());
    assertEquals(0, c1Get.getLabel().size());

    List<Concept> conceptsList =
        conceptMapper.listLatestRelease(
            ConceptSearchParams.builder().name(concept1.getName()).build(),
            DEFAULT_PAGE,
            vocabName);
    assertEquals(1, conceptsList.get(0).getDefinition().size());
    assertEquals(0, conceptsList.get(0).getLabel().size());

    List<SuggestDto> suggestResult =
        conceptMapper.suggestLatestRelease(
            concept1.getName(), concept1.getVocabularyKey(), null, null, vocabName);
    assertEquals(0, suggestResult.get(0).getLabels().size());

    conceptMapper.updateReleaseViews(vocabName);

    c1Get = conceptMapper.getByNameLatestRelease(concept1.getName(), vocabName);
    assertEquals(2, c1Get.getDefinition().size());
    assertEquals(1, c1Get.getLabel().size());

    conceptsList =
        conceptMapper.listLatestRelease(
            ConceptSearchParams.builder().name(concept1.getName()).build(),
            DEFAULT_PAGE,
            vocabName);
    assertEquals(2, conceptsList.get(0).getDefinition().size());
    assertEquals(1, conceptsList.get(0).getLabel().size());

    suggestResult =
        conceptMapper.suggestLatestRelease(
            concept1.getName(), concept1.getVocabularyKey(), null, null, vocabName);
    assertEquals(1, suggestResult.get(0).getLabels().size());
  }

  @Test
  public void getByNameLatestReleaseTest() {
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);
    conceptMapper.createLatestReleaseView(
        DEFAULT_VOCABULARY.toLowerCase(), vocabularies[0].getKey());

    Concept conceptDB =
        conceptMapper.getByNameLatestRelease(concept1.getName(), DEFAULT_VOCABULARY);
    assertEquals(concept1.getKey(), conceptDB.getKey());
  }

  private void assertList(ConceptSearchParams searchParams, int expectedResult) {
    assertEquals(expectedResult, conceptMapper.list(searchParams, DEFAULT_PAGE).size());
    assertEquals(expectedResult, conceptMapper.count(searchParams));
    assertEquals(
        expectedResult,
        conceptMapper
            .listLatestRelease(searchParams, DEFAULT_PAGE, DEFAULT_VOCABULARY.toLowerCase())
            .size());
    assertEquals(
        expectedResult,
        conceptMapper.countLatestRelease(searchParams, DEFAULT_VOCABULARY.toLowerCase()));
  }

  private void assertSimilarity(List<KeyNameResult> similarities, Concept concept) {
    assertEquals(1, similarities.size());
    assertEquals(concept.getKey().intValue(), similarities.get(0).getKey());
    assertEquals(concept.getName(), similarities.get(0).getName());
  }

  @Override
  Concept createNewEntity() {
    Concept entity = new Concept();
    entity.setVocabularyKey(vocabularies[0].getKey());
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
   * <p>NOTE: this initializer cannot be in the base class because it gets executed only once when
   * we run several tests at the same time and provokes errors.
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
