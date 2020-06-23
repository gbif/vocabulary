package org.gbif.vocabulary.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "export")
@Validated
@Getter
@Setter
public class ExportConfig {

  private boolean releaseEnabled;
  private String deployUser;
  private String deployPassword;
  private String deployRepository;

  /** Custom {@link Validator} for {@link ExportConfig}. */
  public static class ExportConfigValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
      return ExportConfig.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
      ExportConfig exportConfig = (ExportConfig) target;

      if (exportConfig.isReleaseEnabled()) {
        if (Strings.isNullOrEmpty(exportConfig.getDeployUser())) {
          errors.rejectValue("deployUser", "Deploy user is required if releases are enabled");
        }
        if (Strings.isNullOrEmpty(exportConfig.getDeployPassword())) {
          errors.rejectValue(
              "deployPassword", "Deploy password is required if releases are enabled");
        }
        if (Strings.isNullOrEmpty(exportConfig.getDeployUser())) {
          errors.rejectValue(
              "deployRepository", "Deploy repository is required if releases are enabled");
        }
      }
    }
  }
}
