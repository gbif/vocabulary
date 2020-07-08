package org.gbif.vocabulary.restws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.Errors;
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

  static void validate(ExportConfig exportConfig, Errors errors) {
    if (exportConfig.isReleaseEnabled()) {
      if (Strings.isNullOrEmpty(exportConfig.getDeployUser())) {
        errors.rejectValue(
            "deployUser", "export-config", "Deploy user is required if releases are enabled");
      }
      if (Strings.isNullOrEmpty(exportConfig.getDeployPassword())) {
        errors.rejectValue(
            "deployPassword",
            "export-config",
            "Deploy password is required if releases are enabled");
      }
      if (Strings.isNullOrEmpty(exportConfig.getDeployUser())) {
        errors.rejectValue(
            "deployRepository",
            "export-config",
            "Deploy repository is required if releases are enabled");
      }
    }
  }
}
