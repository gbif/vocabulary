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

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.client.TagClient;
import org.gbif.vocabulary.client.VocabularyClient;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.utils.LenientEquals;
import org.gbif.vocabulary.restws.LoginServerExtension;
import org.gbif.vocabulary.restws.PostgresDBExtension;
import org.gbif.vocabulary.restws.TestCredentials;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javax.sql.DataSource;

import static org.gbif.vocabulary.restws.TestCredentials.ADMIN;
import static org.gbif.vocabulary.restws.TestCredentials.EDITOR;
import static org.gbif.vocabulary.restws.TestCredentials.INVALID_JWT_USER;
import static org.gbif.vocabulary.restws.TestCredentials.INVALID_USER;
import static org.gbif.vocabulary.restws.TestCredentials.JWT_ADMIN;
import static org.gbif.vocabulary.restws.TestCredentials.JWT_EDITOR;
import static org.gbif.vocabulary.restws.TestCredentials.JWT_USER;
import static org.gbif.vocabulary.restws.TestCredentials.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Base class for resource integration tests. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class BaseResourceIT<T extends VocabularyEntity & LenientEquals<T>> {

  protected static final ObjectMapper OBJECT_MAPPER =
      JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport()
          .registerModule(new JavaTimeModule());

  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  @RegisterExtension static LoginServerExtension loginServer = new LoginServerExtension();

  @Autowired private DataSource dataSource;

  @Autowired WebTestClient webClient;

  protected final VocabularyClient vocabularyClient;
  protected final ConceptClient conceptClient;
  protected final TagClient tagClient;

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

  BaseResourceIT(Class<T> clazz, int localServerPort) {
    this.clazz = clazz;
    ClientBuilder clientBuilder = new ClientBuilder();
    vocabularyClient =
        clientBuilder
            .withUrl("http://localhost:" + localServerPort)
            .withCredentials(ADMIN.getUsername(), ADMIN.getPassword())
            .withObjectMapper(OBJECT_MAPPER)
            .build(VocabularyClient.class);
    conceptClient =
        clientBuilder
            .withUrl("http://localhost:" + localServerPort)
            .withCredentials(ADMIN.getUsername(), ADMIN.getPassword())
            .withObjectMapper(OBJECT_MAPPER)
            .build(ConceptClient.class);
    tagClient =
        clientBuilder
            .withUrl("http://localhost:" + localServerPort)
            .withCredentials(ADMIN.getUsername(), ADMIN.getPassword())
            .withObjectMapper(OBJECT_MAPPER)
            .build(TagClient.class);
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
    Label label =
        Label.builder()
            .language(LanguageRegion.SPANISH)
            .value("Etiqueta")
            .build();
    webClient
        .post()
        .uri(String.format(urlEntityFormat, created.getName()) + "/label")
        .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR))
        .body(BodyInserters.fromValue(label))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBodyList(Long.class);
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
    entity1.setName("Suggest111");
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
    entity2.setName("Suggest222");
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

    Label label =
        Label.builder()
            .language(LanguageRegion.SPANISH)
            .value("Label")
            .build();
    webClient
        .post()
        .uri(String.format(urlEntityFormat, entity1.getName()) + "/label")
        .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR))
        .body(BodyInserters.fromValue(label))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBodyList(Long.class);

    webClient
        .get()
        .uri(
            builder ->
                builder
                    .path(getBasePath() + "/suggest")
                    .queryParam("q", label.getValue())
                    .queryParam("locale", label.getLanguage().getLocale())
                    .build())
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
