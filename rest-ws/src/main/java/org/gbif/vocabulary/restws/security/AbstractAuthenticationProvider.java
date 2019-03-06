package org.gbif.vocabulary.restws.security;

import org.gbif.api.vocabulary.UserRole;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/** Base {@link AuthenticationProvider} to support the different kinds of authentication. */
public abstract class AbstractAuthenticationProvider implements AuthenticationProvider {

  protected static final ObjectReader OBJECT_READER = new ObjectMapper().reader();
  private static final String REGISTRY_LOGIN_URL = "/user/login";

  private final RestTemplate restTemplate;
  private final SecurityConfig config;

  public AbstractAuthenticationProvider(RestTemplate restTemplate, SecurityConfig config) {
    this.restTemplate = restTemplate;
    this.config = config;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    // create headers
    HttpHeaders headers = createHttHeaders(authentication);

    try {
      ResponseEntity<String> response =
          restTemplate.postForEntity(
              config.getRegistryApiUrl() + REGISTRY_LOGIN_URL,
              new HttpEntity<>(headers),
              String.class);

      return createAuthentication(authentication, response);
    } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
      throw new BadCredentialsException("Wrong credentials for user: " + authentication.getName());
    } catch (Exception e) {
      throw new AuthenticationServiceException(
          "Could not authenticate user: " + authentication.getName(), e);
    }
  }

  public abstract HttpHeaders createHttHeaders(Authentication authentication);

  public abstract Authentication createAuthentication(
      Authentication authentication, ResponseEntity<String> response) throws IOException;

  protected Collection<SimpleGrantedAuthority> extractRoles(JsonNode root) {
    List<SimpleGrantedAuthority> roles = new ArrayList<>();
    root.get("roles")
        .forEach(r -> roles.add(new SimpleGrantedAuthority(UserRole.valueOf(r.asText()).name())));

    return roles;
  }
}
