package org.gbif.vocabulary.service;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
@ActiveProfiles("test")
abstract class MockServiceBaseTest {

  @MockBean private DataSource dataSource;
  @MockBean private PlatformTransactionManager platformTransactionManager;
}
