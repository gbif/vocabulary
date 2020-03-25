package org.gbif.vocabulary.restws.security;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "security")
@Validated
@Getter
@Setter
public class SecurityConfig {
  @NotBlank private String loginApiBasePath;
  @NotBlank private String actuatorSecret;
}
