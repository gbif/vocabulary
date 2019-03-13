package org.gbif.vocabulary.restws.resources;

import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.restws.TestUser;

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

import static org.gbif.vocabulary.restws.TestUser.ADMIN;

/** IT for the {@link ConceptResource}. */
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
            .uri("/vocabularies")
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
        .header("Authorization", BASIC_AUTH_HEADER.apply(TestUser.ADMIN))
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
        .header("Authorization", BASIC_AUTH_HEADER.apply(TestUser.ADMIN))
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
    concept.setLabel(Collections.singletonMap(Language.ENGLISH, "Label"));
    concept.setAlternativeLabels(
        Collections.singletonMap(Language.ENGLISH, Arrays.asList("alt1", "alt2")));
    concept.setEditorialNotes(Arrays.asList("note1", "note2"));

    return concept;
  }

  @Override
  String getBasePath() {
    return "/"
        + VocabularyResource.VOCABULARIES_PATH
        + "/"
        + defaultVocabularyName
        + "/"
        + ConceptResource.CONCEPTS_PATH;
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
