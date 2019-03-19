package org.gbif.vocabulary.restws.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
    return new UsernamePasswordAuthenticationToken(
        authentication.getName(),
        authentication.getCredentials(),
        extractRoles(OBJECT_READER.readTree(response.getBody())));
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
