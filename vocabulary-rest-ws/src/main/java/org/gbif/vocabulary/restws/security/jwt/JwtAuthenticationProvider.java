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
package org.gbif.vocabulary.restws.security.jwt;

import org.gbif.vocabulary.restws.security.AbstractAuthenticationProvider;
import org.gbif.vocabulary.restws.security.SecurityConfig;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Custom {@link org.springframework.security.authentication.AuthenticationProvider} to authenticate
 * requests with a JWT token.
 */
@Component("jwtAuthenticationProvider")
public class JwtAuthenticationProvider extends AbstractAuthenticationProvider {

  @Autowired
  public JwtAuthenticationProvider(RestTemplate restTemplate, SecurityConfig config) {
    super(restTemplate, config);
  }

  @Override
  public HttpHeaders createHttHeaders(Authentication authentication) {
    String token = ((JwtAuthentication) authentication).getToken();
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  @Override
  public Authentication createAuthentication(
      Authentication authentication, ResponseEntity<String> response) throws IOException {
    JsonNode responseJsonNode = OBJECT_READER.readTree(response.getBody());
    String newToken = response.getHeaders().getFirst("token");

    return new JwtAuthentication(extractUsername(responseJsonNode), newToken, extractRoles(responseJsonNode));
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return JwtAuthentication.class.isAssignableFrom(authentication);
  }
}
