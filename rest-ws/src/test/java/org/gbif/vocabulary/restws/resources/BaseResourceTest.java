package org.gbif.vocabulary.restws.resources;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
abstract class BaseResourceTest {

  // util constants
  static final int TEST_KEY = 1;
  static final String NAMESPACE_TEST = "ns";

  static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired MockMvc mockMvc;

  @MockBean private DataSource dataSource;
  @MockBean private PlatformTransactionManager platformTransactionManager;
}
