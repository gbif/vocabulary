package org.gbif.vocabulary.restws.resources;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.enums.LanguageRegion;
import org.gbif.vocabulary.model.export.VocabularyExport;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.BodyInserters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.gbif.vocabulary.restws.TestCredentials.ADMIN;
import static org.gbif.vocabulary.restws.utils.Constants.CONCEPTS_PATH;
import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** IT for the {@link VocabularyResource}. */
@ContextConfiguration(initializers = {VocabularyResourceIT.ContexInitializer.class})
public class VocabularyResourceIT extends BaseResourceIT<Vocabulary> {

  private static final String CLEAN_DB_SCRIPT = "/clean-db.sql";

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());
  private static final String TEST_NAMESPACE = "ns";

  VocabularyResourceIT() {
    super(Vocabulary.class);
  }

  @Test
  void listTest() {
    final String namespace = "listns";

    // create entity
    Vocabulary v1 = createEntity();
    v1.setNamespace(namespace);
    webClient
        .post()
        .uri(getBasePath())
        .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
        .body(BodyInserters.fromValue(v1))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isCreated();

    // list entities
    webClient
        .get()
        .uri(builder -> builder.path(getBasePath()).queryParam("namespace", namespace).build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("results")
        .value(r -> assertEquals(1, r.size()), List.class);

    // create entity
    Vocabulary v2 = createEntity();
    v2.setNamespace(namespace);
    webClient
        .post()
        .uri(getBasePath())
        .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
        .body(BodyInserters.fromObject(v2))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isCreated();

    // list entities
    webClient
        .get()
        .uri(builder -> builder.path(getBasePath()).queryParam("namespace", namespace).build())
        .attribute("namespace", namespace)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("results")
        .value(r -> assertEquals(2, r.size()), List.class);
  }

  @Test
  public void exportVocabularyTest() throws IOException {
    // create entity
    Vocabulary v1 = createEntity();
    v1 =
        webClient
            .post()
            .uri(getBasePath())
            .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
            .body(BodyInserters.fromValue(v1))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Vocabulary.class)
            .returnResult()
            .getResponseBody();

    Concept c1 = new Concept();
    c1.setName("c1");
    c1.setVocabularyKey(v1.getKey());
    webClient
        .post()
        .uri(getBasePath() + "/" + v1.getName() + "/" + CONCEPTS_PATH)
        .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
        .body(BodyInserters.fromObject(c1))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isCreated();

    byte[] bytesResponse =
        webClient
            .get()
            .uri(getBasePath() + "/" + v1.getName() + "/export")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .exists("Content-Disposition")
            .expectBody()
            .returnResult()
            .getResponseBody();

    VocabularyExport export = OBJECT_MAPPER.readValue(bytesResponse, VocabularyExport.class);
    assertNotNull(export.getMetadata().getCreatedDate());
    assertEquals(v1.getName(), export.getVocabulary().getName());
    assertEquals(1, export.getConcepts().size());
  }

  @Override
  Vocabulary createEntity() {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(UUID.randomUUID().toString());
    vocabulary.setNamespace(TEST_NAMESPACE);
    vocabulary.setEditorialNotes(Arrays.asList("note1", "note2"));
    vocabulary.setLabel(
        Collections.singletonMap(LanguageRegion.ENGLISH, UUID.randomUUID().toString()));
    return vocabulary;
  }

  @Override
  String getCleanDbScript() {
    return CLEAN_DB_SCRIPT;
  }

  @Override
  String getBasePath() {
    return "/" + VOCABULARIES_PATH;
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
