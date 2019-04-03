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
