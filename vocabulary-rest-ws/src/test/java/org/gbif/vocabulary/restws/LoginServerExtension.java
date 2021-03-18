/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.vocabulary.restws;

import org.gbif.api.vocabulary.UserRole;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.http.HttpStatus;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.gbif.vocabulary.restws.TestCredentials.*;

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
