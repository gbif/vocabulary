package org.gbif.vocabulary.restws;

import org.gbif.api.vocabulary.UserRole;

import java.util.List;
import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.gbif.vocabulary.restws.TestUser.ADMIN;
import static org.gbif.vocabulary.restws.TestUser.EDITOR;
import static org.gbif.vocabulary.restws.TestUser.USER;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

public class LoginServerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WireMockServer wireMockServer =
      new WireMockServer(WireMockConfiguration.DYNAMIC_PORT);

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    wireMockServer.stubFor(
        post("/user/login")
            .withBasicAuth(USER.getUsername(), USER.getPassword())
            .willReturn(
                aResponse().withBody(OBJECT_MAPPER.writeValueAsString(new LoginResponse(USER)))));
    wireMockServer.stubFor(
        post("/user/login")
            .withBasicAuth(ADMIN.getUsername(), ADMIN.getPassword())
            .willReturn(
                aResponse().withBody(OBJECT_MAPPER.writeValueAsString(new LoginResponse(ADMIN)))));
    wireMockServer.stubFor(
        post("/user/login")
            .withBasicAuth(EDITOR.getUsername(), EDITOR.getPassword())
            .willReturn(
                aResponse().withBody(OBJECT_MAPPER.writeValueAsString(new LoginResponse(EDITOR)))));
    wireMockServer.stubFor(
        post("/user/login")
            .withHeader("Authorization", matching("Bearer +"))
            .willReturn(aResponse().withHeader("token", UUID.randomUUID().toString())));

    wireMockServer.start();
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    wireMockServer.stop();
  }

  public WireMockServer getWireMockServer() {
    return wireMockServer;
  }

  static class LoginResponse {
    List<UserRole> roles;
    String userName;

    LoginResponse(TestUser testUser) {
      this.roles = testUser.getRoles();
      this.userName = testUser.getUsername();
    }

    public List<UserRole> getRoles() {
      return roles;
    }

    public void setRoles(List<UserRole> roles) {
      this.roles = roles;
    }

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }
  }
}
