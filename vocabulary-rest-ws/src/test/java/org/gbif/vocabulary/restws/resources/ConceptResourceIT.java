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
package org.gbif.vocabulary.restws.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.AddTagAction;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Definition;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.Vocabulary;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.gbif.vocabulary.restws.TestCredentials.ADMIN;
import static org.gbif.vocabulary.restws.utils.Constants.CONCEPTS_PATH;
import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** IT for the {@link ConceptResource}. */
@ContextConfiguration(initializers = {ConceptResourceIT.ContextInitializer.class})
public class ConceptResourceIT extends BaseResourceIT<Concept> {

  private static final String CLEAN_DB_SCRIPT = "/clean-concepts.sql";

  private static String defaultVocabularyName;
  private static long defaultVocabularyKey;
  private static String otherVocabularyName;
  private static long otherVocabularyKey;

  ConceptResourceIT(@LocalServerPort int localServerPort) {
    super(Concept.class, localServerPort);
  }

  @BeforeAll
  public static void populateData(@Autowired WebTestClient webClient) {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName("V1");

    Vocabulary created =
        webClient
            .post()
            .uri("/" + VOCABULARIES_PATH)
            .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
            .body(BodyInserters.fromValue(vocabulary))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Vocabulary.class)
            .returnResult()
            .getResponseBody();

    defaultVocabularyName = created.getName();
    defaultVocabularyKey = created.getKey();

    Vocabulary other = new Vocabulary();
    other.setName("V2");

    Vocabulary otherCreated =
        webClient
            .post()
            .uri("/" + VOCABULARIES_PATH)
            .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
            .body(BodyInserters.fromValue(other))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Vocabulary.class)
            .returnResult()
            .getResponseBody();

    otherVocabularyName = otherCreated.getName();
    otherVocabularyKey = otherCreated.getKey();
  }

  @Test
  void listTest() {
    Concept c1 = createEntity();
    c1.setName("Concept1");
    ConceptView view1 = conceptClient.create(defaultVocabularyName, c1);

    // create a concept in other vocab to see that the list concepts filters by vocab key
    Concept otherVocabConcept = createEntity();
    otherVocabConcept.setVocabularyKey(otherVocabularyKey);
    otherVocabConcept.setName("OtherVocabConcept");
    conceptClient.create(otherVocabularyName, otherVocabConcept);

    // list entities
    PagingResponse<ConceptView> concepts =
        conceptClient.listConcepts(
            defaultVocabularyName, ConceptListParams.builder().name(c1.getName()).build());
    assertEquals(1, concepts.getResults().size());

    // Add another concept
    Concept c2 = createEntity();
    c2.setName("Concept2");
    c2.setParentKey(view1.getConcept().getKey());
    ConceptView view2 = conceptClient.create(defaultVocabularyName, c2);

    // list entities
    concepts =
        conceptClient.listConcepts(
            defaultVocabularyName, ConceptListParams.builder().q("concept").build());
    assertEquals(2, concepts.getResults().size());

    // list entities
    concepts =
        conceptClient.listConcepts(
            defaultVocabularyName,
            ConceptListParams.builder()
                .q("concept")
                .parentKey(view1.getConcept().getKey())
                .build());
    assertEquals(1, concepts.getResults().size());

    concepts =
        conceptClient.listConcepts(
            defaultVocabularyName,
            ConceptListParams.builder().q("concept").parent(view1.getConcept().getName()).build());
    assertEquals(1, concepts.getResults().size());

    // list entities with parent
    concepts =
        conceptClient.listConcepts(
            defaultVocabularyName, ConceptListParams.builder().hasParent(true).build());
    assertEquals(1, concepts.getResults().size());

    // list entities with children count
    concepts =
        conceptClient.listConcepts(
            defaultVocabularyName,
            ConceptListParams.builder()
                .name(view1.getConcept().getName())
                .includeChildrenCount(true)
                .build());
    assertEquals(1, concepts.getResults().size());
    assertEquals(1, concepts.getResults().get(0).getChildrenCount());

    // list entities with children
    concepts =
        conceptClient.listConcepts(
            defaultVocabularyName,
            ConceptListParams.builder()
                .name(view1.getConcept().getName())
                .includeChildren(true)
                .build());
    assertEquals(1, concepts.getResults().size());
    assertEquals(c2.getName(), concepts.getResults().get(0).getChildren().get(0));

    // list entities with parents
    concepts =
        conceptClient.listConcepts(
            defaultVocabularyName,
            ConceptListParams.builder().name(c2.getName()).includeParents(true).build());
    assertEquals(1, concepts.getResults().size());
    assertEquals(c1.getName(), concepts.getResults().get(0).getParents().get(0));
  }

  @Test
  public void getWithParentsAndChildren() {
    // create entity
    Concept c1 = createEntity();
    ConceptView view1 = conceptClient.create(defaultVocabularyName, c1);

    Concept c2 = createEntity();
    c2.setParentKey(view1.getConcept().getKey());
    ConceptView view2 = conceptClient.create(defaultVocabularyName, c2);

    // get concept with parents
    view2.setParents(Collections.singletonList(view1.getConcept().getName()));

    ConceptView conceptView =
        conceptClient.get(defaultVocabularyName, view2.getConcept().getName(), true, false);
    assertEquals(view2, conceptView);

    // get concept without parents
    view2.setParents(null);
    conceptView =
        conceptClient.get(defaultVocabularyName, view2.getConcept().getName(), false, false);
    assertEquals(view2, conceptView);

    // include children test
    view1.setChildren(Collections.singletonList(view2.getConcept().getName()));
    conceptView =
        conceptClient.get(defaultVocabularyName, view1.getConcept().getName(), false, true);
    assertEquals(view1, conceptView);
  }

  @Test
  void definitionTest() {
    Concept c1 = createEntity();
    ConceptView cView1 = conceptClient.create(defaultVocabularyName, c1);

    Definition definition =
        Definition.builder().language(LanguageRegion.ENGLISH).value("Label").build();

    Definition createdDefinition =
        conceptClient.addDefinition(defaultVocabularyName, c1.getName(), definition);
    definition.setKey(createdDefinition.getKey());
    assertTrue(definition.lenientEquals(createdDefinition));

    assertEquals(
        createdDefinition,
        conceptClient.getDefinition(
            defaultVocabularyName, c1.getName(), createdDefinition.getKey()));

    List<Definition> definitionList =
        conceptClient.listDefinitions(defaultVocabularyName, c1.getName(), new ArrayList<>());
    assertEquals(1, definitionList.size());
    assertTrue(createdDefinition.lenientEquals(definitionList.get(0)));
    assertEquals(createdDefinition.getKey(), definitionList.get(0).getKey());

    assertEquals(
        1,
        conceptClient
            .listDefinitions(
                defaultVocabularyName,
                c1.getName(),
                Collections.singletonList(LanguageRegion.ENGLISH))
            .size());
    assertEquals(
        0,
        conceptClient
            .listDefinitions(
                defaultVocabularyName,
                c1.getName(),
                Collections.singletonList(LanguageRegion.SPANISH))
            .size());

    definition.setValue("Label2");
    Definition updatedDefinition =
        conceptClient.updateDefinition(defaultVocabularyName, c1.getName(), definition);
    assertTrue(definition.lenientEquals(updatedDefinition));

    conceptClient.deleteDefinition(defaultVocabularyName, c1.getName(), updatedDefinition.getKey());
    assertEquals(
        0,
        conceptClient
            .listDefinitions(defaultVocabularyName, c1.getName(), new ArrayList<>())
            .size());
  }

  @Test
  public void tagsTest() {
    Tag tag = new Tag();
    tag.setName("myTag");
    tagClient.create(tag);

    Concept c1 = createEntity();
    ConceptView view1 = conceptClient.create(defaultVocabularyName, c1);

    conceptClient.addTag(
        defaultVocabularyName, view1.getConcept().getName(), new AddTagAction(tag.getName()));

    List<Tag> tags = conceptClient.listTags(defaultVocabularyName, view1.getConcept().getName());
    assertEquals(1, tags.size());

    PagingResponse<ConceptView> concepts =
        conceptClient.listConcepts(
            defaultVocabularyName,
            ConceptListParams.builder().tags(Collections.singletonList(tag.getName())).build());
    assertEquals(1, concepts.getResults().size());
    assertEquals(view1.getConcept().getKey(), concepts.getResults().get(0).getConcept().getKey());

    conceptClient.removeTag(defaultVocabularyName, view1.getConcept().getName(), tag.getName());

    tags = conceptClient.listTags(defaultVocabularyName, view1.getConcept().getName());
    assertEquals(0, tags.size());

    concepts =
        conceptClient.listConcepts(
            defaultVocabularyName,
            ConceptListParams.builder().tags(Collections.singletonList(tag.getName())).build());
    assertEquals(0, concepts.getResults().size());
  }

  @Test
  void labelsTest() {
    Concept c1 = createEntity();
    ConceptView cView1 = conceptClient.create(defaultVocabularyName, c1);

    Label label = Label.builder().language(LanguageRegion.ENGLISH).value("Label").build();

    Long labelKey = conceptClient.addLabel(defaultVocabularyName, c1.getName(), label);
    label.setKey(labelKey);
    assertTrue(labelKey > 0);

    List<Label> labelList =
        conceptClient.listLabels(defaultVocabularyName, c1.getName(), Collections.emptyList());
    assertEquals(1, labelList.size());
    assertTrue(label.lenientEquals(labelList.get(0)));
    assertEquals(labelKey, labelList.get(0).getKey());

    assertEquals(
        1,
        conceptClient
            .listLabels(
                defaultVocabularyName,
                c1.getName(),
                Collections.singletonList(LanguageRegion.ENGLISH))
            .size());
    assertEquals(
        0,
        conceptClient
            .listLabels(
                defaultVocabularyName,
                c1.getName(),
                Collections.singletonList(LanguageRegion.SPANISH))
            .size());

    conceptClient.deleteLabel(defaultVocabularyName, c1.getName(), labelKey);
    assertEquals(
        0,
        conceptClient
            .listLabels(defaultVocabularyName, c1.getName(), Collections.emptyList())
            .size());
  }

  @Test
  void alternativeLabelsTest() {
    Concept c1 = createEntity();
    ConceptView cView1 = conceptClient.create(defaultVocabularyName, c1);

    Label label = Label.builder().language(LanguageRegion.ENGLISH).value("Label").build();

    Long labelKey = conceptClient.addAlternativeLabel(defaultVocabularyName, c1.getName(), label);
    label.setKey(labelKey);
    assertTrue(labelKey > 0);

    PagingResponse<Label> labelList =
        conceptClient.listAlternativeLabels(
            defaultVocabularyName, c1.getName(), null, new PagingRequest());
    assertEquals(1, labelList.getResults().size());
    assertTrue(label.lenientEquals(labelList.getResults().get(0)));

    assertEquals(
        1,
        conceptClient
            .listAlternativeLabels(
                defaultVocabularyName,
                c1.getName(),
                Collections.singletonList(LanguageRegion.ENGLISH),
                new PagingRequest())
            .getResults()
            .size());
    assertEquals(
        0,
        conceptClient
            .listAlternativeLabels(
                defaultVocabularyName,
                c1.getName(),
                Collections.singletonList(LanguageRegion.SPANISH),
                new PagingRequest())
            .getResults()
            .size());

    conceptClient.deleteAlternativeLabel(defaultVocabularyName, c1.getName(), labelKey);
    assertEquals(
        0,
        conceptClient
            .listAlternativeLabels(defaultVocabularyName, c1.getName(), null, new PagingRequest())
            .getResults()
            .size());
  }

  @Test
  void hiddenLabelsTest() {
    Concept c1 = createEntity();
    ConceptView cView1 = conceptClient.create(defaultVocabularyName, c1);

    HiddenLabel label = HiddenLabel.builder().value("Label").build();

    Long labelKey = conceptClient.addHiddenLabel(defaultVocabularyName, c1.getName(), label);
    label.setKey(labelKey);
    assertTrue(labelKey > 0);

    PagingResponse<HiddenLabel> labelList =
        conceptClient.listHiddenLabels(defaultVocabularyName, c1.getName(), new PagingRequest());
    assertEquals(1, labelList.getResults().size());
    assertTrue(label.lenientEquals(labelList.getResults().get(0)));

    conceptClient.deleteHiddenLabel(defaultVocabularyName, c1.getName(), labelKey);
    assertEquals(
        0,
        conceptClient
            .listHiddenLabels(defaultVocabularyName, c1.getName(), new PagingRequest())
            .getResults()
            .size());
  }

  @Override
  Concept createEntity() {
    Concept concept = new Concept();
    concept.setName("N" + UUID.randomUUID().toString().replace("-", ""));
    concept.setVocabularyKey(defaultVocabularyKey);
    concept.setEditorialNotes(Arrays.asList("note1", "note2"));

    return concept;
  }

  @Override
  String getCleanDbScript() {
    return CLEAN_DB_SCRIPT;
  }

  @Override
  String getBasePath() {
    return "/" + VOCABULARIES_PATH + "/" + defaultVocabularyName + "/" + CONCEPTS_PATH;
  }

  static class ContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
              "spring.datasource.url=" + database.getPostgresContainer().getJdbcUrl(),
              "spring.datasource.username=" + database.getPostgresContainer().getUsername(),
              "spring.datasource.password=" + database.getPostgresContainer().getPassword(),
              "security.loginApiBasePath=" + loginServer.getWireMockServer().baseUrl())
          .applyTo(configurableApplicationContext.getEnvironment());
    }
  }
}
