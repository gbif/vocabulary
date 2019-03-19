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

  public String getLoginApiBasePath() {
    return loginApiBasePath;
  }

  public void setLoginApiBasePath(String loginApiBasePath) {
    this.loginApiBasePath = loginApiBasePath;
  }
}
