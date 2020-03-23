package org.gbif.vocabulary.restws;

import org.gbif.api.vocabulary.UserRole;

import java.util.List;
import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.http.HttpStatus;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.gbif.vocabulary.restws.TestCredentials.*;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
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
            .withBasicAuth(INVALID_USER.getUsername(), INVALID_USER.getPassword())
            .willReturn(aResponse().withStatus(HttpStatus.UNAUTHORIZED.value())));
    wireMockServer.stubFor(
        post("/user/login")
            .withHeader("Authorization", equalTo("Bearer " + JWT_USER.getToken()))
            .willReturn(
                aResponse()
                    .withBody(OBJECT_MAPPER.writeValueAsString(new LoginResponse(JWT_USER)))
                    .withHeader("token", UUID.randomUUID().toString())));
    wireMockServer.stubFor(
        post("/user/login")
            .withHeader("Authorization", equalTo("Bearer " + JWT_ADMIN.getToken()))
            .willReturn(
                aResponse()
                    .withBody(OBJECT_MAPPER.writeValueAsString(new LoginResponse(JWT_ADMIN)))
                    .withHeader("token", UUID.randomUUID().toString())));
    wireMockServer.stubFor(
        post("/user/login")
            .withHeader("Authorization", equalTo("Bearer " + JWT_EDITOR.getToken()))
            .willReturn(
                aResponse()
                    .withBody(OBJECT_MAPPER.writeValueAsString(new LoginResponse(JWT_EDITOR)))
                    .withHeader("token", UUID.randomUUID().toString())));
    wireMockServer.stubFor(
        post("/user/login")
            .withHeader("Authorization", equalTo("Bearer " + INVALID_JWT_USER.getToken()))
            .willReturn(aResponse().withStatus(HttpStatus.UNAUTHORIZED.value())));

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

    LoginResponse(TestCredentials testCredentials) {
      this.roles = testCredentials.getRoles();
      this.userName = testCredentials.getUsername();
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
