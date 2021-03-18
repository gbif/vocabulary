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
