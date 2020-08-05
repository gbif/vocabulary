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
package org.gbif.vocabulary.restws.advices;

import org.gbif.vocabulary.model.VocabularyEntity;

import java.io.IOException;
import java.lang.reflect.Type;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

/**
 * Intercepts all the requests with a {@link org.gbif.vocabulary.model.VocabularyEntity} in the body
 * and assigns the required {@link org.gbif.vocabulary.model.Auditable} fields.
 */
@ControllerAdvice
public class AuditRequestAdvice implements RequestBodyAdvice {

  @Override
  public boolean supports(
      @NotNull MethodParameter methodParameter,
      Type targetType,
      @NotNull Class<? extends HttpMessageConverter<?>> converterType) {
    try {
      return VocabularyEntity.class.isAssignableFrom(Class.forName(targetType.getTypeName()));
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Unexpected target type", e);
    }
  }

  @NotNull
  @Override
  public HttpInputMessage beforeBodyRead(
      @NotNull HttpInputMessage inputMessage,
      @NotNull MethodParameter parameter,
      @NotNull Type targetType,
      @NotNull Class<? extends HttpMessageConverter<?>> converterType)
      throws IOException {
    // do nothing
    return inputMessage;
  }

  @NotNull
  @Override
  public Object afterBodyRead(
      @NotNull Object body,
      @NotNull HttpInputMessage inputMessage,
      @NotNull MethodParameter parameter,
      @NotNull Type targetType,
      @NotNull Class<? extends HttpMessageConverter<?>> converterType) {
    // set auditable fields
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    VocabularyEntity vocabularyEntity = (VocabularyEntity) body;

    if (vocabularyEntity.getCreatedBy() == null) {
      vocabularyEntity.setCreatedBy(authentication.getName());
    }

    vocabularyEntity.setModifiedBy(authentication.getName());

    return vocabularyEntity;
  }

  @Override
  public Object handleEmptyBody(
      Object body,
      @NotNull HttpInputMessage inputMessage,
      @NotNull MethodParameter parameter,
      @NotNull Type targetType,
      @NotNull Class<? extends HttpMessageConverter<?>> converterType) {
    // do nothing
    return body;
  }
}
