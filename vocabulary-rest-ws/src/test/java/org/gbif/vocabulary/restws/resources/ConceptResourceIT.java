package org.gbif.vocabulary.restws.resources;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.enums.LanguageRegion;
import org.gbif.vocabulary.restws.model.ConceptView;

import org.hamcrest.Matchers;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

/** IT for the {@link ConceptResource}. */
@ContextConfiguration(initializers = {ConceptResourceIT.ContextInitializer.class})
public class ConceptResourceIT extends BaseResourceIT<Concept> {

  private static final String CLEAN_DB_SCRIPT = "/clean-concepts.sql";

  private static String defaultVocabularyName;
  private static long defaultVocabularyKey;

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
    Concept created1 =
        webClient
            .post()
            .uri(getBasePath())
            .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
            .body(BodyInserters.fromValue(c1))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Concept.class)
            .returnResult()
            .getResponseBody();

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
    c2.setParentKey(created1.getKey());
    webClient
        .post()
        .uri(getBasePath())
        .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
        .body(BodyInserters.fromValue(c2))
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

    // list entities
    webClient
        .get()
        .uri(
            builder ->
                builder
                    .path(getBasePath())
                    .queryParam("q", "concept")
                    .queryParam("parentKey", created1.getKey())
                    .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("results")
        .value(r -> Assertions.assertEquals(1, r.size()), List.class);

    // list entities with parent
    webClient
        .get()
        .uri(builder -> builder.path(getBasePath()).queryParam("hasParent", true).build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("results")
        .value(r -> Assertions.assertEquals(1, r.size()), List.class);

    // list entities
    webClient
        .get()
        .uri(
            builder ->
                builder
                    .path(getBasePath())
                    .queryParam("name", c1.getName())
                    .queryParam("includeChildrenCount", true)
                    .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("results")
        .value(r -> Assertions.assertEquals(1, r.size()), List.class)
        .jsonPath("results[0].childrenCount")
        .value(Matchers.equalTo(1));
  }

  @Test
  public void getWithParents() {
    // create entity
    Concept c1 = createEntity();
    Concept created1 =
        webClient
            .post()
            .uri(getBasePath())
            .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
            .body(BodyInserters.fromValue(c1))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader()
            .value("Location", Matchers.endsWith(getBasePath() + "/" + c1.getName()))
            .expectBody(Concept.class)
            .value(
                v -> {
                  assertEquals(ADMIN.getUsername(), v.getCreatedBy());
                  assertEquals(ADMIN.getUsername(), v.getModifiedBy());
                })
            .returnResult()
            .getResponseBody();

    Concept c2 = createEntity();
    c2.setParentKey(c1.getKey());
    Concept created2 =
        webClient
            .post()
            .uri(getBasePath())
            .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
            .body(BodyInserters.fromValue(c2))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader()
            .value("Location", Matchers.endsWith(getBasePath() + "/" + c2.getName()))
            .expectBody(Concept.class)
            .value(
                v -> {
                  assertEquals(ADMIN.getUsername(), v.getCreatedBy());
                  assertEquals(ADMIN.getUsername(), v.getModifiedBy());
                })
            .returnResult()
            .getResponseBody();

    // get vocabulary with parents
    ConceptView expected = new ConceptView(c2);
    expected.setParents(Collections.singletonList(created1.getName()));
    webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(String.format(urlEntityFormat, created2.getName()))
                    .queryParam("includeParents", true)
                    .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(ConceptView.class)
        .equals(expected);

    // get vocabulary without parents
    expected = new ConceptView(c2);
    webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder.path(String.format(urlEntityFormat, created2.getName())).build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(ConceptView.class)
        .equals(expected);
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
