package org.gbif.vocabulary.restws.resources;

import org.gbif.api.model.registry.LenientEquals;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.model.enums.LanguageRegion;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.restws.LoginServerExtension;
import org.gbif.vocabulary.restws.PostgresDBExtension;
import org.gbif.vocabulary.restws.TestCredentials;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import javax.sql.DataSource;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Base64Utils;
import org.springframework.web.reactive.function.BodyInserters;

import static org.gbif.vocabulary.restws.TestCredentials.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Base class for resource integration tests. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class BaseResourceIT<T extends VocabularyEntity & LenientEquals<T>> {

  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  @RegisterExtension static LoginServerExtension loginServer = new LoginServerExtension();

  @Autowired private DataSource dataSource;

  @Autowired WebTestClient webClient;

  static final Function<TestCredentials, String> BASIC_AUTH_HEADER =
      testCredentials ->
          "Basic "
              + Base64Utils.encodeToString(
                  (testCredentials.getUsername() + ":" + testCredentials.getPassword())
                      .getBytes(StandardCharsets.UTF_8));

  static final Function<TestCredentials, String> JWT_AUTH_HEADER =
      testCredentials -> "Bearer " + testCredentials.getToken();

  private final String urlDeprecateFormat = getBasePath() + "/%s/deprecate";
  private final Class<T> clazz;
  protected final String urlEntityFormat = getBasePath() + "/%s";

  BaseResourceIT(Class<T> clazz) {
    this.clazz = clazz;
  }

  @BeforeEach
  public void cleanDB() throws SQLException {
    Connection connection = dataSource.getConnection();
    ScriptUtils.executeSqlScript(connection, new ClassPathResource(getCleanDbScript()));
    connection.close();
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
            .body(BodyInserters.fromValue(entity))
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
    created.getLabel().put(LanguageRegion.SPANISH, "Etiqueta");
    webClient
        .put()
        .uri(String.format(urlEntityFormat, created.getName()))
        .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR))
        .body(BodyInserters.fromValue(created))
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
  void invalidBasicAuthTest() {
    webClient
        .post()
        .uri(getBasePath())
        .header("Authorization", BASIC_AUTH_HEADER.apply(INVALID_USER))
        .body(BodyInserters.fromValue(createEntity()))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void basicAuthInsufficientCredentialsTest() {
    // create entity with invalid credentials using JWT
    webClient
        .post()
        .uri(getBasePath())
        .header("Authorization", BASIC_AUTH_HEADER.apply(USER))
        .body(BodyInserters.fromValue(createEntity()))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void jwtSuccessTest() {
    // create entity with admin using JWT
    webClient
        .post()
        .uri(getBasePath())
        .header("Authorization", JWT_AUTH_HEADER.apply(JWT_ADMIN))
        .body(BodyInserters.fromValue(createEntity()))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(clazz)
        .value(
            v -> {
              assertEquals(JWT_ADMIN.getUsername(), v.getCreatedBy());
              assertEquals(JWT_ADMIN.getUsername(), v.getModifiedBy());
            });

    // create entity with editor using JWT
    webClient
        .post()
        .uri(getBasePath())
        .header("Authorization", JWT_AUTH_HEADER.apply(JWT_EDITOR))
        .body(BodyInserters.fromValue(createEntity()))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(clazz)
        .value(
            v -> {
              assertEquals(JWT_EDITOR.getUsername(), v.getCreatedBy());
              assertEquals(JWT_EDITOR.getUsername(), v.getModifiedBy());
            });
  }

  @Test
  void jwtInsufficientCredentialsTest() {
    // create entity with invalid credentials using JWT
    webClient
        .post()
        .uri(getBasePath())
        .header("Authorization", JWT_AUTH_HEADER.apply(JWT_USER))
        .body(BodyInserters.fromValue(createEntity()))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void invalidJwtTokenTest() {
    webClient
        .post()
        .uri(getBasePath())
        .header("Authorization", JWT_AUTH_HEADER.apply(INVALID_JWT_USER))
        .body(BodyInserters.fromValue(createEntity()))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  public void suggestTest() {
    // create entity
    T entity1 = createEntity();
    entity1.setName("suggest111");
    webClient
        .post()
        .uri(getBasePath())
        .header("Authorization", BASIC_AUTH_HEADER.apply(TestCredentials.ADMIN))
        .body(BodyInserters.fromValue(entity1))
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
        .header("Authorization", BASIC_AUTH_HEADER.apply(TestCredentials.ADMIN))
        .body(BodyInserters.fromValue(entity2))
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
            .body(BodyInserters.fromValue(createEntity()))
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

  abstract String getCleanDbScript();

  abstract String getBasePath();
}
