package org.gbif.vocabulary.persistence;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.mybatis.type.StringArrayTypeHandler;
import org.gbif.mybatis.type.UriArrayTypeHandler;

import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PersistenceApplication {

  @TestConfiguration
  static class BeanConfig {

    @Bean
    ConfigurationCustomizer mybatisConfigurationCustomizer() {
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
  }
}
