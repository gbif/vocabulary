/*
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
package org.gbif.vocabulary.restws.advices;

import org.gbif.vocabulary.api.DeprecateAction;
import org.gbif.vocabulary.api.DeprecateConceptAction;
import org.gbif.vocabulary.api.DeprecateVocabularyAction;

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

import lombok.extern.slf4j.Slf4j;

/**
 * Intercepts all the deprecation requests and sets the auditable fields. The user is taken from the
 * security context.
 */
@ControllerAdvice
@Slf4j
public class DeprecationRequestAdvice implements RequestBodyAdvice {

  private static final BiPredicate<Class, Type> IS_ASSIGNABLE =
      (expected, targetType) -> {
        try {
          return expected.isAssignableFrom(Class.forName(targetType.getTypeName()));
        } catch (ClassNotFoundException e) {
          log.debug("Unexpected parameter type", e);
          return false;
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
