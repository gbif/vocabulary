package org.gbif.vocabulary.restws.resolvers;

import org.gbif.vocabulary.model.LanguageRegion;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class LanguageRegionHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return LanguageRegion.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    String paramName = parameter.getParameterName();
    String locale = paramName != null ? webRequest.getParameter(paramName) : null;

    if (locale == null || locale.isEmpty()) {
      return null;
    }

    LanguageRegion languageRegion = LanguageRegion.fromLocale(locale);

    if (languageRegion == LanguageRegion.UNKNOWN) {
      throw new IllegalArgumentException("Unknown language region: " + locale);
    }

    return languageRegion;
  }
}
