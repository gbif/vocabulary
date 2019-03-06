package org.gbif.vocabulary.restws.security.jwt;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * JWT {@link org.springframework.security.core.Authentication} that contains a jwt token and
 * information about the user.
 */
public class JwtAuthentication extends AbstractAuthenticationToken {

  private String username;
  private String token;

  public JwtAuthentication(String token) {
    super(null);
    this.token = token;
    super.setAuthenticated(false);
  }

  public JwtAuthentication(
      String username, String token, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    super.setAuthenticated(true);
    this.username = username;
    this.token = token;
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return username;
  }

  public String getToken() {
    return token;
  }
}
