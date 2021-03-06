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
      httpServletResponse.addHeader("Access-Control-Expose-Headers", RESPONSE_TOKEN_HEADER);
    }

    return body;
  }
}
