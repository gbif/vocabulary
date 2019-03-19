package org.gbif.vocabulary.restws.security.jwt;

import org.gbif.vocabulary.restws.security.AbstractAuthenticationProvider;
import org.gbif.vocabulary.restws.security.SecurityConfig;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
    JsonNode jsonNode = OBJECT_READER.readTree(response.getBody());
    String username = jsonNode.get("userName").asText();
    String newToken = response.getHeaders().getFirst("token");

    return new JwtAuthentication(username, newToken, extractRoles(jsonNode));
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return JwtAuthentication.class.isAssignableFrom(authentication);
  }
}
