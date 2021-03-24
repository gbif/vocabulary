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
package org.gbif.vocabulary.restws.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@link org.springframework.security.authentication.AuthenticationProvider} for request with basic
 * auth.
 */
@Component("basicAuthAuthenticationProvider")
public class BasicAuthAuthenticationProvider extends AbstractAuthenticationProvider {

  @Autowired
  public BasicAuthAuthenticationProvider(RestTemplate restTemplate, SecurityConfig config) {
    super(restTemplate, config);
  }

  @Override
  public HttpHeaders createHttHeaders(Authentication authentication) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth(authentication.getName(), authentication.getCredentials().toString());
    return headers;
  }

  @Override
  public Authentication createAuthentication(
      Authentication authentication, ResponseEntity<String> response) throws IOException {
    JsonNode responseJsonNode = OBJECT_READER.readTree(response.getBody());

    return new UsernamePasswordAuthenticationToken(
        extractUsername(responseJsonNode),
        authentication.getCredentials(),
        extractRoles(responseJsonNode));
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
