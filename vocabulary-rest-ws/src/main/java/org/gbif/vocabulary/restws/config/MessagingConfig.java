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

@ConfigurationProperties(prefix = "messaging")
@Validated
@Getter
@Setter
public class MessagingConfig {

  private boolean enabled;
  private String host;
  private String virtualHost;
  private int port = 5672;
  private String username;
  private String password;

  static void validate(MessagingConfig messagingConfig, Errors errors) {
    if (messagingConfig.isEnabled()) {
      if (Strings.isNullOrEmpty(messagingConfig.getHost())) {
        errors.rejectValue(
            "host", "messaging-config-validation", "Host is required if messaging is enabled");
      }
      if (Strings.isNullOrEmpty(messagingConfig.getVirtualHost())) {
        errors.rejectValue(
            "virtualHost",
            "messaging-config-validation",
            "Virtual Host is required if messaging is enabled");
      }
      if (Strings.isNullOrEmpty(messagingConfig.getUsername())) {
        errors.rejectValue(
            "username",
            "messaging-config-validation",
            "Username Host is required if messaging is enabled");
      }
      if (Strings.isNullOrEmpty(messagingConfig.getPassword())) {
        errors.rejectValue(
            "password",
            "messaging-config-validation",
            "Password Host is required if messaging is enabled");
      }
    }
  }
}
