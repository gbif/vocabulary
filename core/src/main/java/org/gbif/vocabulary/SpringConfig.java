package org.gbif.vocabulary;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.mybatis.type.StringArrayTypeHandler;
import org.gbif.mybatis.type.UriArrayTypeHandler;

import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:config.properties")
public class SpringConfig {

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

}
