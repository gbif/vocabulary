package org.gbif.vocabulary.restws.resources;

import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.restws.LoginServerExtension;
import org.gbif.vocabulary.restws.PostgresDBExtension;
import org.gbif.vocabulary.restws.TestUser;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Base64Utils;
import org.springframework.web.reactive.function.BodyInserters;

import static org.gbif.vocabulary.restws.TestUser.ADMIN;
import static org.gbif.vocabulary.restws.TestUser.EDITOR;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Base class for resource integration tests. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class BaseResourceIT<T extends VocabularyEntity & LenientEquals> {

  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  @RegisterExtension static LoginServerExtension loginServer = new LoginServerExtension();

  @Autowired WebTestClient webClient;

  static final Function<TestUser, String> BASIC_AUTH_HEADER =
      testUser ->
          "Basic "
              + Base64Utils.encodeToString(
                  (testUser.getUsername() + ":" + testUser.getPassword())
                      .getBytes(StandardCharsets.UTF_8));

  private final String urlEntityFormat = getBasePath() + "/%s";
  private final String urlDeprecateFormat = getBasePath() + "/%s/deprecate";
  private final Class<T> clazz;

  BaseResourceIT(Class<T> clazz) {
    this.clazz = clazz;
  }

  @Test
  void crudTest() {
    // create entity
    T entity = createEntity();
    T created =
        webClient
            .post()
            .uri(getBasePath())
            .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
            .body(BodyInserters.fromObject(entity))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader()
            .value("Location", Matchers.endsWith(getBasePath() + "/" + entity.getName()))
            .expectBody(clazz)
            .value(
                v -> {
                  assertEquals(ADMIN.getUsername(), v.getCreatedBy());
                  assertEquals(ADMIN.getUsername(), v.getModifiedBy());
                })
            .returnResult()
            .getResponseBody();

    // get vocabulary
    webClient
        .get()
        .uri(String.format(urlEntityFormat, created.getName()))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(clazz)
        .isEqualTo(created);

    // update vocabulary
    created.getLabel().put(Language.SPANISH, "Etiqueta");
    webClient
        .put()
        .uri(String.format(urlEntityFormat, created.getName()))
        .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR))
        .body(BodyInserters.fromObject(created))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(clazz)
        .value(
            v -> {
              assertTrue(created.lenientEquals(v));
              assertEquals(EDITOR.getUsername(), v.getModifiedBy());
            });
  }

  @Test
  public void suggestTest() {
    // create entity
    T entity1 = createEntity();
    entity1.setName("suggest111");
    webClient
        .post()
        .uri(getBasePath())
        .header("Authorization", BASIC_AUTH_HEADER.apply(TestUser.ADMIN))
        .body(BodyInserters.fromObject(entity1))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isCreated();

    // create entity
    T entity2 = createEntity();
    entity2.setName("suggest222");
    webClient
        .post()
        .uri(getBasePath())
        .header("Authorization", BASIC_AUTH_HEADER.apply(TestUser.ADMIN))
        .body(BodyInserters.fromObject(entity2))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isCreated();

    webClient
        .get()
        .uri(builder -> builder.path(getBasePath() + "/suggest").queryParam("q", "sugg").build())
        .exchange()
        .expectBodyList(KeyNameResult.class)
        .hasSize(2);

    webClient
        .get()
        .uri(builder -> builder.path(getBasePath() + "/suggest").queryParam("q", "ggest1").build())
        .exchange()
        .expectBodyList(KeyNameResult.class)
        .hasSize(1);
  }

  @Test
  void deprecationTest() {
    // create entity
    T created =
        webClient
            .post()
            .uri(getBasePath())
            .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
            .body(BodyInserters.fromObject(createEntity()))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(clazz)
            .returnResult()
            .getResponseBody();

    // deprecate
    webClient
        .put()
        .uri(String.format(urlDeprecateFormat, created.getName()))
        .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNoContent();

    // assert deprecation
    webClient
        .get()
        .uri(String.format(urlEntityFormat, created.getName()))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(Vocabulary.class)
        .value(
            v -> {
              assertNotNull(v.getDeprecated());
              assertEquals(ADMIN.getUsername(), v.getDeprecatedBy());
              assertNull(v.getReplacedByKey());
            });

    // restore deprecated
    webClient
        .delete()
        .uri(String.format(urlDeprecateFormat, created.getName()))
        .header("Authorization", BASIC_AUTH_HEADER.apply(ADMIN))
        .exchange()
        .expectStatus()
        .isNoContent();

    // assert restored entity
    webClient
        .get()
        .uri(String.format(urlEntityFormat, created.getName()))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(Vocabulary.class)
        .value(
            v -> {
              assertNull(v.getDeprecated());
              assertNull(v.getDeprecatedBy());
              assertNull(v.getReplacedByKey());
            });
  }

  abstract T createEntity();

  abstract String getBasePath();
}
