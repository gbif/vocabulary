package org.gbif.vocabulary.restws.security.jwt;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Intercepts all requests to look for a JWT token. If found it will set to the {@link
 * org.springframework.security.core.context.SecurityContext} a {@link JwtAuthentication} that
 * contains the token in order to process by the {@link JwtAuthenticationProvider}.
 */
public class JwtRequestFilter extends OncePerRequestFilter {

  private final AuthenticationManager authenticationManager;

  public JwtRequestFilter(AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    JwtUtils.findTokenInRequest(request)
        .ifPresent(
            token ->
                SecurityContextHolder.getContext()
                    .setAuthentication(
                        authenticationManager.authenticate(new JwtAuthentication(token))));

    filterChain.doFilter(request, response);
  }
}
