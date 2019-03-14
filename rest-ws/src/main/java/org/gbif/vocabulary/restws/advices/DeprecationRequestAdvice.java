package org.gbif.vocabulary.restws.advices;

import org.gbif.vocabulary.restws.model.DeprecateAction;
import org.gbif.vocabulary.restws.model.DeprecateConceptAction;
import org.gbif.vocabulary.restws.model.DeprecateVocabularyAction;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.function.BiPredicate;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

@ControllerAdvice
public class DeprecationRequestAdvice implements RequestBodyAdvice {

  private static final BiPredicate<Class, Type> IS_ASSIGNABLE =
      (expected, targetType) -> {
        try {
          return expected.isAssignableFrom(Class.forName(targetType.getTypeName()));
        } catch (ClassNotFoundException e) {
          throw new IllegalStateException("Unexpected target type", e);
        }
      };

  @Override
  public boolean supports(
      MethodParameter methodParameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    return IS_ASSIGNABLE.test(DeprecateAction.class, targetType);
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
    DeprecateAction deprecateAction = (DeprecateAction) body;
    deprecateAction.setDeprecatedBy(getAuthenticatedUser());

    return deprecateAction;
  }

  @Override
  public Object handleEmptyBody(
      Object body,
      HttpInputMessage inputMessage,
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {

    if (IS_ASSIGNABLE.test(DeprecateVocabularyAction.class, targetType)) {
      DeprecateVocabularyAction action = new DeprecateVocabularyAction();
      action.setDeprecatedBy(getAuthenticatedUser());
      return action;
    } else if (IS_ASSIGNABLE.test(DeprecateConceptAction.class, targetType)) {
      DeprecateConceptAction action = new DeprecateConceptAction();
      action.setDeprecatedBy(getAuthenticatedUser());
      return action;
    }

    return body;
  }

  private String getAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
  }
}
