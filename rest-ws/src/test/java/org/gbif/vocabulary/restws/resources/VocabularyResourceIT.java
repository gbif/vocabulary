package org.gbif.vocabulary.restws.resources;

import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.restws.LoginServerExtension;
import org.gbif.vocabulary.restws.PostgresDBExtension;
import org.gbif.vocabulary.restws.TestUser;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Base64Utils;
import org.springframework.web.reactive.function.BodyInserters;

@ExtendWith({SpringExtension.class, LoginServerExtension.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {VocabularyResourceIT.ContexInitializer.class})
public class VocabularyResourceIT {

  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  @RegisterExtension static LoginServerExtension loginServer = new LoginServerExtension();

  @Autowired private WebTestClient webClient;

  // TODO: ....
  @Test
  public void exampleTest() {
    webClient.get().uri("/vocabularies").exchange().expectStatus().isOk();

    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName("name");
    webClient
        .post()
        .uri("/vocabularies")
        .header(
            "Authorization",
            "Basic "
                + Base64Utils.encodeToString(
                    (TestUser.ADMIN.getUsername() + ":" + TestUser.ADMIN.getPassword())
                        .getBytes(StandardCharsets.UTF_8)))
        .body(BodyInserters.fromObject(vocabulary))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isCreated();
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
