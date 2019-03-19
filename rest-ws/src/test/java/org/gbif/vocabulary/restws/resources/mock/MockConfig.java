package org.gbif.vocabulary.restws.resources.mock;

import javax.sql.DataSource;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Profile("mock")
public class MockConfig {

  @Bean
  DataSource dataSource() {
    return Mockito.mock(DataSource.class);
  }

  @Bean
  PlatformTransactionManager platformTransactionManager() {
    return Mockito.mock(PlatformTransactionManager.class);
  }
}
