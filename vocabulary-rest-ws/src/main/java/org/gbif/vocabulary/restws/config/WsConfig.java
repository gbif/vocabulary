package org.gbif.vocabulary.restws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "ws")
@Validated
@Getter
@Setter
public class WsConfig {

  @NotBlank
  private String apiUrl;

}
