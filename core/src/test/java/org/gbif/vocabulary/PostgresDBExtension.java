package org.gbif.vocabulary;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresDBExtension implements BeforeAllCallback, AfterAllCallback {

  private static final String DB = "vocabulary";
  private static final String POSTGRES_IMAGE = "postgres:11.1";

  private final PostgreSQLContainer postgresContainer =
      new PostgreSQLContainer(POSTGRES_IMAGE).withDatabaseName(DB);

  @Override
  public void beforeAll(ExtensionContext context) {
    postgresContainer.start();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    postgresContainer.stop();
  }

  public PostgreSQLContainer getPostgresContainer() {
    return postgresContainer;
  }
}
