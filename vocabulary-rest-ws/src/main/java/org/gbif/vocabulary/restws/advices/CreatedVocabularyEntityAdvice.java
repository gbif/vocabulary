package org.gbif.vocabulary.restws.advices;

import org.gbif.vocabulary.model.VocabularyEntity;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Intercepts all the responses of POST requests to add a Location header and a {@link
 * HttpStatus#CREATED} code.
 */
@ControllerAdvice
public class CreatedVocabularyEntityAdvice implements ResponseBodyAdvice<VocabularyEntity> {

  @Override
  public boolean supports(
      MethodParameter returnType, @NotNull Class<? extends HttpMessageConverter<?>> converterType) {
    try {
      return returnType.hasMethodAnnotation(PostMapping.class)
          && !ResponseEntity.class.isAssignableFrom(returnType.getParameterType())
          && VocabularyEntity.class.isAssignableFrom(
              Class.forName(returnType.getGenericParameterType().getTypeName()));
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Unexpected parameter type", e);
    }
  }

  @Override
  public VocabularyEntity beforeBodyWrite(
      VocabularyEntity body,
      @NotNull MethodParameter returnType,
      @NotNull MediaType selectedContentType,
      @NotNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
      @NotNull ServerHttpRequest request,
      @NotNull ServerHttpResponse response) {

    HttpServletResponse httpServletResponse =
        ((ServletServerHttpResponse) response).getServletResponse();
    if (HttpStatus.OK.value() == httpServletResponse.getStatus()) {
      HttpServletRequest httpServletRequest =
          ((ServletServerHttpRequest) request).getServletRequest();
      httpServletResponse.addHeader(
          "Location", httpServletRequest.getRequestURL() + "/" + body.getName());
      httpServletResponse.setStatus(HttpStatus.CREATED.value());
    }

    return body;
  }
}
