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
package org.gbif.vocabulary;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.mybatis.type.StringArrayTypeHandler;
import org.gbif.mybatis.type.UriArrayTypeHandler;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * This class contains the necessary spring configuration to use this module. This configuration
 * should be imported by all the users of this module.
 */
@Configuration
@PropertySource(value = "classpath:core.properties")
@ComponentScan("org.gbif.vocabulary.service")
@MapperScan("org.gbif.vocabulary.persistence.mappers")
public class SpringConfig {

  /**
   * We import only the necessary mybatis config from other modules. We can't import all the classes
   * of the package because some classes are missing a default constructor.
   */
  @Bean
  ConfigurationCustomizer mybatisConfigCustomizer() {
    return configuration -> {
      // importing type handlers from common-mybatis project
      configuration.getTypeHandlerRegistry().register(UriArrayTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("UriArrayTypeHandler", UriArrayTypeHandler.class);
      configuration.getTypeHandlerRegistry().register(StringArrayTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("StringArrayTypeHandler", StringArrayTypeHandler.class);
      configuration.getTypeAliasRegistry().registerAlias("Pageable", Pageable.class);
    };
  }

  /**
   * Added to force clients to use the java validations. If not added here, clients could disable
   * it.
   */
  @Bean
  public MethodValidationPostProcessor methodValidationPostProcessor() {
    return new MethodValidationPostProcessor();
  }
}
