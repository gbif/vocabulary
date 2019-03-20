package org.gbif.vocabulary.restws.security;

import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "security")
@Validated
public class SecurityConfig {

  @NotBlank private String loginApiBasePath;

  @NotBlank private String stopSecret;

  public String getLoginApiBasePath() {
    return loginApiBasePath;
  }

  public void setLoginApiBasePath(String loginApiBasePath) {
    this.loginApiBasePath = loginApiBasePath;
  }

  public String getStopSecret() {
    return stopSecret;
  }

  public void setStopSecret(String stopSecret) {
    this.stopSecret = stopSecret;
  }
}
