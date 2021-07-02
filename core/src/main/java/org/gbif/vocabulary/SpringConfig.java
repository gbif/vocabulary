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
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
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
      // importing type handlers from common-mybatis project. Not needed for the type handlers
      // define in this project since their path is specified in the core.properties file
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

  @Configuration
  @EnableGlobalMethodSecurity(securedEnabled = true, jsr250Enabled = true, prePostEnabled = true)
  public static class RegistryMethodSecurityConfiguration
      extends GlobalMethodSecurityConfiguration {

    @Override
    protected AccessDecisionManager accessDecisionManager() {
      AffirmativeBased accessDecisionManager = (AffirmativeBased) super.accessDecisionManager();

      // Remove the ROLE_ prefix from RoleVoter for @Secured and hasRole checks on methods
      accessDecisionManager.getDecisionVoters().stream()
          .filter(RoleVoter.class::isInstance)
          .map(RoleVoter.class::cast)
          .forEach(it -> it.setRolePrefix(""));

      return accessDecisionManager;
    }
  }
}
