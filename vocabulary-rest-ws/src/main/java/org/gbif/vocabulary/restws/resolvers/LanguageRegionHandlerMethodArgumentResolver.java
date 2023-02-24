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
package org.gbif.vocabulary.restws.resolvers;

import org.gbif.vocabulary.model.LanguageRegion;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

public class LanguageRegionHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return LanguageRegion.class.equals(parameter.getParameterType()) || isList(parameter);
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    String paramName = parameter.getParameterName();

    if (paramName == null) {
      return null;
    }

    if (isList(parameter)) {
      String[] locales = webRequest.getParameterMap().get(paramName);
      if (locales == null) {
        return null;
      }
      return Arrays.stream(locales).map(this::convertToLanguageRegion).collect(Collectors.toList());
    } else {
      String locale = webRequest.getParameter(paramName);
      if (locale == null || locale.isEmpty()) {
        return null;
      }
      return convertToLanguageRegion(locale);
    }
  }

  private LanguageRegion convertToLanguageRegion(String locale) {
    LanguageRegion languageRegion = LanguageRegion.fromLocale(locale);

    if (languageRegion == LanguageRegion.UNKNOWN) {
      languageRegion = LanguageRegion.valueOf(locale);
      if (languageRegion == LanguageRegion.UNKNOWN) {
        throw new IllegalArgumentException("Unknown language region: " + locale);
      }
    }
    return languageRegion;
  }

  private boolean isList(MethodParameter parameter) {
    return List.class.equals(parameter.getParameterType())
        && LanguageRegion.class
            .getName()
            .equals(
                ((ParameterizedTypeImpl) parameter.getGenericParameterType())
                    .getActualTypeArguments()[0].getTypeName());
  }
}
