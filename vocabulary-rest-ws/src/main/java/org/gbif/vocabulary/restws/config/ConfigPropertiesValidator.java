package org.gbif.vocabulary.restws.config;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Custom {@link Validator} for the configuration properties. Needed to validate properties than
 * depend on another property.
 */
@Component
public class ConfigPropertiesValidator implements Validator {

  @Override
  public boolean supports(Class<?> clazz) {
    return ExportConfig.class.isAssignableFrom(clazz)
        || MessagingConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    if (target instanceof ExportConfig) {
      ExportConfig.validate((ExportConfig) target, errors);
    } else if (target instanceof MessagingConfig) {
      MessagingConfig.validate((MessagingConfig) target, errors);
    }
  }
}
