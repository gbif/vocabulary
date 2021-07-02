package org.gbif.vocabulary.restws.resources;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.client.TagClient;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.restws.LoginServerExtension;
import org.gbif.vocabulary.restws.PostgresDBExtension;
import org.gbif.ws.client.ClientBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.gbif.vocabulary.restws.TestCredentials.ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ContextConfiguration(initializers = {TagResourceIT.ContextInitializer.class})
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TagResourceIT {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  @RegisterExtension static LoginServerExtension loginServer = new LoginServerExtension();

  private final TagClient tagClient;

  TagResourceIT(@LocalServerPort int localServerPort) {
    ClientBuilder clientBuilder = new ClientBuilder();
    tagClient =
        clientBuilder
            .withUrl("http://localhost:" + localServerPort)
            .withCredentials(ADMIN.getUsername(), ADMIN.getPassword())
            .withObjectMapper(OBJECT_MAPPER)
            .build(TagClient.class);
  }

  @Test
  public void crudTest() {
    Tag tag = new Tag();
    tag.setName("tag");

    Tag createdTag = tagClient.create(tag);
    assertEquals(createdTag, tagClient.getTag(tag.getName()));

    createdTag.setColor("#000000");
    Tag updatedTag = tagClient.update(createdTag);
    assertEquals(createdTag.getColor(), updatedTag.getColor());

    PagingResponse<Tag> tags = tagClient.listTags(new PagingRequest(0, 5));
    assertEquals(1, tags.getCount());
    assertEquals(1, tags.getResults().size());

    tagClient.delete(updatedTag.getName());

    tags = tagClient.listTags(new PagingRequest(0, 5));
    assertEquals(0, tags.getCount());
    assertEquals(0, tags.getResults().size());
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
