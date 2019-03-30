package org.gbif.vocabulary.restws.resources;

import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.model.Vocabulary;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.BodyInserters;

import static org.gbif.vocabulary.restws.TestCredentials.ADMIN;
import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;

/** IT for the {@link VocabularyResource}. */
@ContextConfiguration(initializers = {VocabularyResourceIT.ContexInitializer.class})
public class VocabularyResourceIT extends BaseResourceIT<Vocabulary> {

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
        .body(BodyInserters.fromObject(v1))
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
        .value(r -> Assertions.assertEquals(1, r.size()), List.class);

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
        .value(r -> Assertions.assertEquals(2, r.size()), List.class);
  }

  @Override
  Vocabulary createEntity() {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(UUID.randomUUID().toString());
    vocabulary.setNamespace(TEST_NAMESPACE);
    vocabulary.setEditorialNotes(Arrays.asList("note1", "note2"));
    vocabulary.setLabel(Collections.singletonMap(Language.ENGLISH, "Label"));
    return vocabulary;
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
