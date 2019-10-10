package org.gbif.vocabulary.restws.resources;

import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.gbif.vocabulary.restws.TestCredentials.ADMIN;
import static org.gbif.vocabulary.restws.utils.Constants.CONCEPTS_PATH;
import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;

/**
 * IT for the {@link ConceptResource}.
 *
 * <p>These tests are intended to run in parallel. This should be taken into account when adding new
 * * tests since we're not cleaning the DB after each test and htis can interferred with other
 * tests.
 */
@ContextConfiguration(initializers = {ConceptResourceIT.ContexInitializer.class})
public class ConceptResourceIT extends BaseResourceIT<Concept> {

  private static String defaultVocabularyName;
  private static int defaultVocabularyKey;

  ConceptResourceIT() {
    super(Concept.class);
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
            .body(BodyInserters.fromObject(vocabulary))
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
    webClient
        .post()
        .uri(getBasePath())
        .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
        .body(BodyInserters.fromObject(c1))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isCreated();

    // list entities
    webClient
        .get()
        .uri(builder -> builder.path(getBasePath()).queryParam("name", c1.getName()).build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("results")
        .value(r -> Assertions.assertEquals(1, r.size()), List.class);

    Concept c2 = createEntity();
    c2.setName("concept2");
    webClient
        .post()
        .uri(getBasePath())
        .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
        .body(BodyInserters.fromObject(c2))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isCreated();

    // list entities
    webClient
        .get()
        .uri(builder -> builder.path(getBasePath()).queryParam("q", "concept").build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("results")
        .value(r -> Assertions.assertEquals(2, r.size()), List.class);
  }

  @Override
  Concept createEntity() {
    Concept concept = new Concept();
    concept.setName(UUID.randomUUID().toString());
    concept.setVocabularyKey(defaultVocabularyKey);
    concept.setLabel(Collections.singletonMap(Language.ENGLISH, UUID.randomUUID().toString()));
    concept.setAlternativeLabels(
        Collections.singletonMap(
            Language.ENGLISH,
            Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString())));
    concept.setEditorialNotes(Arrays.asList("note1", "note2"));

    return concept;
  }

  @Override
  String getBasePath() {
    return "/" + VOCABULARIES_PATH + "/" + defaultVocabularyName + "/" + CONCEPTS_PATH;
  }

  static class ContexInitializer
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
