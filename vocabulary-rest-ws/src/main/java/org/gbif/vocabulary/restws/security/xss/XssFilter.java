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
package org.gbif.vocabulary.restws.security.xss;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Checks for XSS scripts in the request.
 *
 * <p>Note: Spring escapes the characters, maybe some checks here are not needed.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XssFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (XssUtils.containsXSS(request.getQueryString())
        || headersContainsXss(request)
        || attributesContainsXss(request)) {
      response.sendError(HttpStatus.BAD_REQUEST.value(), "Potentially malicious XSS script found");
    }

    filterChain.doFilter(request, response);
  }

  private boolean headersContainsXss(HttpServletRequest request) {
    Enumeration<String> headers = request.getHeaderNames();
    while (headers.hasMoreElements()) {
      if (XssUtils.containsXSS(request.getHeader(headers.nextElement()))) {
        return true;
      }
    }
    return false;
  }

  private boolean attributesContainsXss(HttpServletRequest request) {
    Enumeration<String> attributes = request.getAttributeNames();
    while (attributes.hasMoreElements()) {
      if (XssUtils.containsXSS(String.valueOf(request.getAttribute(attributes.nextElement())))) {
        return true;
      }
    }
    return false;
  }
}
