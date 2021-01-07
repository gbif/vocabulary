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
package org.gbif.vocabulary.restws.resources;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.enums.LanguageRegion;

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

/** IT for the {@link ConceptResource}. */
@ContextConfiguration(initializers = {ConceptResourceIT.ContextInitializer.class})
public class ConceptResourceIT extends BaseResourceIT<Concept> {

  private static final String CLEAN_DB_SCRIPT = "/clean-concepts.sql";

  private static String defaultVocabularyName;
  private static long defaultVocabularyKey;

  ConceptResourceIT(@LocalServerPort int localServerPort) {
    super(Concept.class, localServerPort);
  }

  @BeforeAll
  public static void populateData(@Autowired WebTestClient webClient) {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName("v1");

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
  }

  @Test
  void listTest() {
    Concept c1 = createEntity();
    c1.setName("concept1");
    Concept created1 = conceptClient.create(defaultVocabularyName, c1);

    // list entities
    PagingResponse<ConceptView> concepts =
        conceptClient.listConcepts(
            defaultVocabularyName, ConceptListParams.builder().name(c1.getName()).build());
    assertEquals(1, concepts.getResults().size());

    // Add another concept
    Concept c2 = createEntity();
    c2.setName("concept2");
    c2.setParentKey(created1.getKey());
    c2 = conceptClient.create(defaultVocabularyName, c2);

    // list entities
    concepts =
        conceptClient.listConcepts(
            defaultVocabularyName, ConceptListParams.builder().query("concept").build());
    assertEquals(2, concepts.getResults().size());

    // list entities
    concepts =
        conceptClient.listConcepts(
            defaultVocabularyName,
            ConceptListParams.builder().query("concept").parentKey(created1.getKey()).build());
    assertEquals(1, concepts.getResults().size());

    concepts =
        conceptClient.listConcepts(
            defaultVocabularyName,
            ConceptListParams.builder().query("concept").parent(created1.getName()).build());
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
                .name(created1.getName())
                .includeChildrenCount(true)
                .build());
    assertEquals(1, concepts.getResults().size());
    assertEquals(1, concepts.getResults().get(0).getChildrenCount());

    // list entities with children
    concepts =
        conceptClient.listConcepts(
            defaultVocabularyName,
            ConceptListParams.builder().name(created1.getName()).includeChildren(true).build());
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
    Concept created1 = conceptClient.create(defaultVocabularyName, c1);

    Concept c2 = createEntity();
    c2.setParentKey(created1.getKey());
    Concept created2 = conceptClient.create(defaultVocabularyName, c2);

    // get concept with parents
    ConceptView expected = new ConceptView(created2);
    expected.setParents(Collections.singletonList(created1.getName()));

    ConceptView conceptView =
        conceptClient.get(defaultVocabularyName, created2.getName(), true, false);
    assertEquals(expected, conceptView);

    // get concept without parents
    expected = new ConceptView(created2);
    conceptView = conceptClient.get(defaultVocabularyName, created2.getName(), false, false);
    assertEquals(expected, conceptView);

    // include children test
    expected = new ConceptView(created1);
    expected.setChildren(Collections.singletonList(created2.getName()));
    conceptView = conceptClient.get(defaultVocabularyName, created1.getName(), false, true);
    assertEquals(expected, conceptView);
  }

  @Override
  Concept createEntity() {
    Concept concept = new Concept();
    concept.setName(UUID.randomUUID().toString());
    concept.setVocabularyKey(defaultVocabularyKey);
    concept.setLabel(
        Collections.singletonMap(LanguageRegion.ENGLISH, UUID.randomUUID().toString()));
    concept.setAlternativeLabels(
        Collections.singletonMap(
            LanguageRegion.ENGLISH,
            Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString())));
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
