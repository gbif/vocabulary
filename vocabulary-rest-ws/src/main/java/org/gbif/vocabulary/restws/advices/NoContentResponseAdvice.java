package org.gbif.vocabulary.restws.advices;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * It intercepts all the requests that return void to set the {@link HttpStatus#NO_CONTENT} to the
 * response.
 */
@ControllerAdvice
public class NoContentResponseAdvice implements ResponseBodyAdvice<Void> {

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return returnType.getParameterType().isAssignableFrom(void.class);
  }

  @Override
  public Void beforeBodyWrite(
      Void body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {

    if (HttpStatus.OK.value()
        == ((ServletServerHttpResponse) response).getServletResponse().getStatus()) {
      response.setStatusCode(HttpStatus.NO_CONTENT);
    }

    return body;
  }
}
