package org.gbif.vocabulary.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "export")
@Validated
@Getter
@Setter
public class ExportConfig {

  @NotBlank private String deployUser;
  @NotBlank private String deployPassword;
  @NotBlank private String deployRepository;
  private boolean deployEnabled;

}
