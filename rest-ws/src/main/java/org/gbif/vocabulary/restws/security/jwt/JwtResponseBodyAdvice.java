package org.gbif.vocabulary.restws.security.jwt;

import javax.servlet.http.HttpServletResponse;

import org.assertj.core.util.Strings;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** For the Jwt requests, it adds the new generated JWT token to the response. */
@ControllerAdvice
public class JwtResponseBodyAdvice implements ResponseBodyAdvice {

  private static final String RESPONSE_TOKEN_HEADER = "token";

  @Override
  public boolean supports(MethodParameter returnType, Class converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {

    // if it was a JWT request set the new token in the response
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthentication
        && !Strings.isNullOrEmpty(((JwtAuthentication) authentication).getToken())) {
      HttpServletResponse httpServletResponse =
          ((ServletServerHttpResponse) response).getServletResponse();
      httpServletResponse.setHeader(
          RESPONSE_TOKEN_HEADER, ((JwtAuthentication) authentication).getToken());
      httpServletResponse.setHeader("Access-Control-Expose-Headers", RESPONSE_TOKEN_HEADER);
    }

    return body;
  }
}
