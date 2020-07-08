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
