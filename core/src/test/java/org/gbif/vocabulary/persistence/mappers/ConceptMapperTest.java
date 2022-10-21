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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.gbif.vocabulary.TestUtils;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.ChildrenResult;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
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

  private static long[] vocabularyKeys = new long[2];

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
    vocabularyKeys[0] = vocabulary.getKey();

    Vocabulary vocabulary2 = new Vocabulary();
    vocabulary2.setName("Default2");
    vocabulary2.setCreatedBy("test");
    vocabulary2.setModifiedBy("test");
    vocabularyMapper.create(vocabulary2);
    vocabularyKeys[1] = vocabulary2.getKey();
  }

  @Test
  public void listConceptsTest() {
    Concept concept1 = createNewEntity();
    concept1.setName("Concept1");
    conceptMapper.create(concept1);

    conceptMapper.addAlternativeLabel(
        Label.builder()
            .entityKey(concept1.getKey())
            .language(LanguageRegion.ENGLISH)
            .value("alternative example")
            .build());

    Concept concept2 = createNewEntity();
    concept2.setName("Concept2");
    concept2.setParentKey(concept1.getKey());
    conceptMapper.create(concept2);

    conceptMapper.addHiddenLabel(
        HiddenLabel.builder().entityKey(concept2.getKey()).value("misspelt example").build());

    Concept concept3 = createNewEntity();
    concept3.setName("Concept3");
    concept3.setParentKey(concept1.getKey());
    concept3.setEditorialNotes(Collections.singletonList("editorial notes"));
    conceptMapper.create(concept3);
    concept3.setReplacedByKey(concept2.getKey());
    conceptMapper.update(concept3);

    assertList(ConceptSearchParams.builder().query("concept1").key(concept1.getKey()).build(), 1);
    assertList(ConceptSearchParams.builder().query("concept1").key(Long.MAX_VALUE).build(), 0);
    assertList(ConceptSearchParams.builder().key(concept1.getKey()).build(), 1);
    assertList(ConceptSearchParams.builder().query("conc").build(), 3);
    assertList(ConceptSearchParams.builder().query("example").build(), 2);
    assertList(ConceptSearchParams.builder().query("altern ex").build(), 1);
    assertList(ConceptSearchParams.builder().query("oncept").build(), 0);
    assertList(ConceptSearchParams.builder().vocabularyKey(vocabularyKeys[0]).build(), 3);
    assertList(ConceptSearchParams.builder().parentKey(concept1.getKey()).build(), 2);
    assertList(ConceptSearchParams.builder().parent(concept1.getName()).build(), 2);
    assertList(ConceptSearchParams.builder().parentKey(concept2.getKey()).build(), 0);
    assertList(ConceptSearchParams.builder().parent(concept2.getName()).build(), 0);
    assertList(ConceptSearchParams.builder().name("Concept1").build(), 1);
    assertList(ConceptSearchParams.builder().name("Concepto").build(), 0);
    assertList(
        ConceptSearchParams.builder().query("exa").vocabularyKey(vocabularyKeys[0]).build(), 2);
    assertList(
        ConceptSearchParams.builder().parentKey(concept1.getKey()).name("Concept3").build(), 1);
    assertList(ConceptSearchParams.builder().replacedByKey(concept2.getKey()).build(), 1);
    assertList(ConceptSearchParams.builder().hasParent(true).build(), 2);
    assertList(ConceptSearchParams.builder().hasReplacement(true).build(), 1);
  }

  @Test
  public void suggestTest() {
    // create entities for the test
    Concept c1 = createNewEntity();
    c1.setName("Suggest111");

    conceptMapper.create(c1);
    assertNotNull(c1.getKey());

    conceptMapper.addLabel(
        Label.builder()
            .entityKey(c1.getKey())
            .language(LanguageRegion.ENGLISH)
            .value("labelenglish")
            .build());

    conceptMapper.addLabel(
        Label.builder()
            .entityKey(c1.getKey())
            .language(LanguageRegion.SPANISH)
            .value("labelspanish")
            .build());

    conceptMapper.addHiddenLabel(
        HiddenLabel.builder().entityKey(c1.getKey()).value("lab,l").build());
    conceptMapper.addHiddenLabel(
        HiddenLabel.builder().entityKey(c1.getKey()).value("lbel").build());

    Concept c2 = createNewEntity();
    c2.setName("Suggest222");
    conceptMapper.create(c2);
    assertNotNull(c2.getKey());

    conceptMapper.addLabel(
        Label.builder()
            .entityKey(c2.getKey())
            .language(LanguageRegion.ENGLISH)
            .value("Label")
            .build());

    conceptMapper.addHiddenLabel(
        HiddenLabel.builder().entityKey(c2.getKey()).value("lab,l").build());
    conceptMapper.addHiddenLabel(
        HiddenLabel.builder().entityKey(c2.getKey()).value("lbel").build());

    // check result values
    List<KeyNameResult> result = conceptMapper.suggest("suggest1", c1.getVocabularyKey(), null);
    assertEquals("Suggest111", result.get(0).getName());
    assertEquals(c1.getKey().intValue(), result.get(0).getKey());

    // assert expected number of results
    assertEquals(2, conceptMapper.suggest("su", c1.getVocabularyKey(), null).size());
    assertEquals(2, conceptMapper.suggest("gge", c1.getVocabularyKey(), null).size());
    assertEquals(1, conceptMapper.suggest("22", c1.getVocabularyKey(), null).size());
    assertEquals(0, conceptMapper.suggest("zz", c1.getVocabularyKey(), null).size());
    assertEquals(0, conceptMapper.suggest(null, c1.getVocabularyKey(), null).size());
    assertEquals(2, conceptMapper.suggest("label", c1.getVocabularyKey(), null).size());
    assertEquals(1, conceptMapper.suggest("labeleng", c1.getVocabularyKey(), null).size());
    assertEquals(
        0, conceptMapper.suggest("labeleng", c1.getVocabularyKey(), LanguageRegion.SPANISH).size());
    assertEquals(
        1, conceptMapper.suggest("labeleng", c1.getVocabularyKey(), LanguageRegion.ENGLISH).size());

    Concept c3 = createNewEntity();
    c3.setVocabularyKey(vocabularyKeys[1]);
    c3.setName("Suggest333");
    conceptMapper.create(c3);
    assertNotNull(c3.getKey());

    assertEquals(2, conceptMapper.suggest("su", c1.getVocabularyKey(), null).size());
    assertEquals(1, conceptMapper.suggest("su", c3.getVocabularyKey(), null).size());
    assertEquals(1, conceptMapper.suggest("33", c3.getVocabularyKey(), null).size());
    assertEquals(0, conceptMapper.suggest("33", c1.getVocabularyKey(), null).size());
  }

  @Test
  public void findSimilaritiesTest() {
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);

    conceptMapper.addLabel(
        Label.builder()
            .entityKey(concept1.getKey())
            .language(LanguageRegion.SPANISH)
            .value("primero ")
            .build());

    HiddenLabel hiddenLabel1 =
        HiddenLabel.builder().entityKey(concept1.getKey()).value("primeiro").build();
    conceptMapper.addHiddenLabel(hiddenLabel1);

    HiddenLabel hiddenLabel2 =
        HiddenLabel.builder().entityKey(concept1.getKey()).value("otro primeiro").build();
    conceptMapper.addHiddenLabel(hiddenLabel2);

    // check hidden labels
    List<KeyNameResult> similarities =
        conceptMapper.findSimilarities(
            normalizeLabel("primeiro"), null, concept1.getVocabularyKey(), null);
    assertSimilarity(similarities, concept1);

    // remove hidden labels and there shouldn't be similarities
    concept1 = conceptMapper.get(concept1.getKey());
    conceptMapper.deleteHiddenLabel(hiddenLabel1.getKey());
    conceptMapper.deleteHiddenLabel(hiddenLabel2.getKey());

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
        Label.builder()
            .entityKey(concept1.getKey())
            .language(LanguageRegion.ENGLISH)
            .value("normalization")
            .build());

    conceptMapper.addHiddenLabel(
        HiddenLabel.builder().entityKey(concept1.getKey()).value("norm").build());
    conceptMapper.addHiddenLabel(
        HiddenLabel.builder().entityKey(concept1.getKey()).value("another norm").build());

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
        Label.builder()
            .entityKey(concept1.getKey())
            .language(LanguageRegion.ENGLISH)
            .value("l1")
            .build());

    conceptMapper.addAlternativeLabel(
        Label.builder()
            .entityKey(concept1.getKey())
            .language(LanguageRegion.SPANISH)
            .value("l uno")
            .build());

    conceptMapper.addHiddenLabel(
        HiddenLabel.builder().entityKey(concept1.getKey()).value("ll1").build());
    conceptMapper.addHiddenLabel(
        HiddenLabel.builder().entityKey(concept1.getKey()).value("l1l").build());

    Concept concept2 = createNewEntity();
    concept2.setName("C2");
    conceptMapper.create(concept2);

    conceptMapper.addLabel(
        Label.builder()
            .entityKey(concept2.getKey())
            .language(LanguageRegion.ENGLISH)
            .value("l2")
            .build());

    conceptMapper.addHiddenLabel(
        HiddenLabel.builder().entityKey(concept2.getKey()).value("ll2").build());
    conceptMapper.addHiddenLabel(
        HiddenLabel.builder().entityKey(concept2.getKey()).value("l2l").build());

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

  @Test
  public void findParentsTest() {
    // just created, doesn't have parent
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);
    assertTrue(conceptMapper.findParents(concept1.getKey()).isEmpty());

    // concept2 will have concept1 as parent
    Concept concept2 = createNewEntity();
    concept2.setParentKey(concept1.getKey());
    conceptMapper.create(concept2);
    List<String> parents = conceptMapper.findParents(concept2.getKey());
    assertEquals(1, parents.size());
    assertEquals(concept1.getName(), parents.get(0));

    // concept3 will have concept2 as parent
    Concept concept3 = createNewEntity();
    concept3.setParentKey(concept2.getKey());
    conceptMapper.create(concept3);
    parents = conceptMapper.findParents(concept3.getKey());
    assertEquals(2, parents.size());
    assertTrue(parents.contains(concept1.getName()));
    assertTrue(parents.contains(concept2.getName()));
  }

  @Test
  public void countChildrenTest() {
    Concept concept1 = createNewEntity();
    conceptMapper.create(concept1);
    assertTrue(conceptMapper.findParents(concept1.getKey()).isEmpty());

    Concept concept2 = createNewEntity();
    concept2.setParentKey(concept1.getKey());
    conceptMapper.create(concept2);

    Concept concept3 = createNewEntity();
    concept3.setParentKey(concept1.getKey());
    conceptMapper.create(concept3);

    Concept concept4 = createNewEntity();
    concept4.setParentKey(concept3.getKey());
    conceptMapper.create(concept4);

    List<ChildrenResult> counts =
        conceptMapper.countChildren(
            Arrays.asList(
                concept1.getKey(), concept2.getKey(), concept3.getKey(), concept4.getKey()));
    assertEquals(3, counts.size());
    assertTrue(counts.contains(new ChildrenResult(concept1.getKey(), concept2.getName())));
    assertTrue(counts.contains(new ChildrenResult(concept1.getKey(), concept3.getName())));
    assertTrue(counts.contains(new ChildrenResult(concept3.getKey(), concept4.getName())));
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
            ConceptSearchParams.builder().vocabularyKey(vocabularyKeys[0]).build(), DEFAULT_PAGE);
    assertEquals(2, conceptsFound.size());

    conceptMapper.deleteAllConcepts(vocabularyKeys[0]);
    conceptsFound =
        conceptMapper.list(
            ConceptSearchParams.builder().vocabularyKey(vocabularyKeys[0]).build(), DEFAULT_PAGE);
    assertEquals(0, conceptsFound.size());
  }

  @Test
  public void labelsTest() {
    Concept concept = createNewEntity();
    conceptMapper.create(concept);

    Label label =
        Label.builder()
            .entityKey(concept.getKey())
            .language(LanguageRegion.ENGLISH)
            .value("test")
            .build();
    conceptMapper.addLabel(label);

    List<Label> labels = conceptMapper.listLabels(concept.getKey());
    assertEquals(1, labels.size());

    label = conceptMapper.getLabel(label.getKey());
    assertEquals("test", label.getValue());
    assertEquals(LanguageRegion.ENGLISH, label.getLanguage());

    label.setValue("test2");
    conceptMapper.updateLabel(label);
    label = conceptMapper.getLabel(label.getKey());
    assertEquals("test2", label.getValue());

    conceptMapper.deleteLabel(label.getKey());
    labels = conceptMapper.listLabels(concept.getKey());
    assertEquals(0, labels.size());
  }

  @Test
  public void alternativeLabelsTest() {
    Concept concept = createNewEntity();
    conceptMapper.create(concept);

    Label label =
        Label.builder()
            .entityKey(concept.getKey())
            .language(LanguageRegion.ENGLISH)
            .value("test")
            .build();
    conceptMapper.addAlternativeLabel(label);

    List<Label> labels = conceptMapper.listAlternativeLabels(concept.getKey(), DEFAULT_PAGE);
    assertEquals(1, labels.size());
    assertEquals(labels.size(), conceptMapper.countAlternativeLabels(concept.getKey()));

    label = conceptMapper.getAlternativeLabel(label.getKey());
    assertEquals("test", label.getValue());
    assertEquals(LanguageRegion.ENGLISH, label.getLanguage());

    label.setValue("test2");
    conceptMapper.updateAlternativeLabel(label);
    label = conceptMapper.getAlternativeLabel(label.getKey());
    assertEquals("test2", label.getValue());

    labels = conceptMapper.listAlternativeLabels(concept.getKey(), PAGE_FN.apply(0, 0L));
    assertEquals(0, labels.size());
    assertEquals(1, conceptMapper.countAlternativeLabels(concept.getKey()));

    conceptMapper.deleteAlternativeLabel(label.getKey());
    labels = conceptMapper.listAlternativeLabels(concept.getKey(), DEFAULT_PAGE);
    assertEquals(0, labels.size());
    assertEquals(labels.size(), conceptMapper.countAlternativeLabels(concept.getKey()));
  }

  @Test
  public void hiddenLabelsTest() {
    Concept concept = createNewEntity();
    conceptMapper.create(concept);

    HiddenLabel label = HiddenLabel.builder().entityKey(concept.getKey()).value("test").build();
    conceptMapper.addHiddenLabel(label);

    List<HiddenLabel> labels = conceptMapper.listHiddenLabels(concept.getKey(), DEFAULT_PAGE);
    assertEquals(1, labels.size());
    assertEquals(labels.size(), conceptMapper.countHiddenLabels(concept.getKey()));

    labels = conceptMapper.listHiddenLabels(concept.getKey(), PAGE_FN.apply(0, 0L));
    assertEquals(0, labels.size());
    assertEquals(1, conceptMapper.countHiddenLabels(concept.getKey()));

    label = conceptMapper.getHiddenLabel(label.getKey());
    assertEquals("test", label.getValue());

    label.setValue("test2");
    conceptMapper.updateHiddenLabel(label);
    label = conceptMapper.getHiddenLabel(label.getKey());
    assertEquals("test2", label.getValue());

    conceptMapper.deleteHiddenLabel(label.getKey());
    labels = conceptMapper.listHiddenLabels(concept.getKey(), DEFAULT_PAGE);
    assertEquals(0, labels.size());
    assertEquals(labels.size(), conceptMapper.countHiddenLabels(concept.getKey()));
  }

  private void assertList(ConceptSearchParams searchParams, int expectedResult) {
    assertEquals(expectedResult, conceptMapper.list(searchParams, DEFAULT_PAGE).size());
    assertEquals(expectedResult, conceptMapper.count(searchParams));
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
    entity.setName(TestUtils.getRandomName());
    entity.setDefinitions(
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
