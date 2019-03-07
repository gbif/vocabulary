package org.gbif.vocabulary.restws.advices;

import org.gbif.vocabulary.restws.model.DeprecateAction;

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

@ControllerAdvice
public class DeprecationRequestAdvice implements RequestBodyAdvice {

  @Override
  public boolean supports(
      MethodParameter methodParameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    try {
      return DeprecateAction.class.isAssignableFrom(Class.forName(targetType.getTypeName()));
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Unexpected target type", e);
    }
  }

  @Override
  public HttpInputMessage beforeBodyRead(
      HttpInputMessage inputMessage,
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType)
      throws IOException {
    // do nothing
    return inputMessage;
  }

  @Override
  public Object afterBodyRead(
      Object body,
      HttpInputMessage inputMessage,
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    // set auditable fields
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    DeprecateAction deprecateAction = (DeprecateAction) body;
    deprecateAction.setDeprecatedBy(authentication.getName());

    return body;
  }

  @Override
  public Object handleEmptyBody(
      Object body,
      HttpInputMessage inputMessage,
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    // do nothing
    return body;
  }
}
