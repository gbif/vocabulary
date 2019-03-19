package org.gbif.vocabulary.restws.security.jwt;

import java.util.Optional;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;

class JwtUtils {

  // Patterns that catches case insensitive versions of word 'bearer'
  private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)bearer");

  private JwtUtils() {}

  /** Tries to find the token in the {@link HttpHeaders#AUTHORIZATION} header. */
  static Optional<String> findTokenInRequest(HttpServletRequest request) {
    // check header first
    return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
        .filter(JwtUtils::containsBearer)
        .map(JwtUtils::removeBearer);
  }

  /**
   * Removes 'bearer' token, leading an trailing whitespaces.
   *
   * @param token to be clean
   * @return a token without whitespaces and the word 'bearer'
   */
  private static String removeBearer(String token) {
    return BEARER_PATTERN.matcher(token).replaceAll("").trim();
  }

  private static boolean containsBearer(String header) {
    return BEARER_PATTERN.matcher(header).find();
  }
}
