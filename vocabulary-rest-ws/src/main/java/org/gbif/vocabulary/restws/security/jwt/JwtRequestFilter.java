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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
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
            token -> {
              try {
                SecurityContextHolder.getContext()
                    .setAuthentication(
                        authenticationManager.authenticate(new JwtAuthentication(token)));
              } catch (AuthenticationException exc) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              }
            });

    filterChain.doFilter(request, response);
  }
}
