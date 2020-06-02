package org.gbif.vocabulary.service;

import java.util.List;
import java.util.UUID;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.vocabulary.PostgresDBExtension;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyRelease;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyReleaseMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Integration tests for the {@link ExportService}. */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = {ExportServiceIT.ContexInitializer.class})
@ActiveProfiles("test")
public class ExportServiceIT {

  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  private final VocabularyReleaseMapper vocabularyReleaseMapper;
  private final ExportService exportService;
  private final VocabularyMapper vocabularyMapper;

  @Autowired
  ExportServiceIT(
      VocabularyReleaseMapper vocabularyReleaseMapper,
      ExportService exportService,
      VocabularyMapper vocabularyMapper) {
    this.vocabularyReleaseMapper = vocabularyReleaseMapper;
    this.exportService = exportService;
    this.vocabularyMapper = vocabularyMapper;
  }

  @Test
  public void listReleasesTest() {
    // create vocabulary
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(UUID.randomUUID().toString());
    vocabulary.setCreatedBy("test");
    vocabulary.setModifiedBy("test");
    vocabularyMapper.create(vocabulary);

    // create releases
    VocabularyRelease vr1 = new VocabularyRelease();
    vr1.setVocabularyKey(vocabulary.getKey());
    vr1.setVersion("1.0");
    vr1.setCreatedBy("test");
    vr1.setExportUrl("dummy url");
    vocabularyReleaseMapper.create(vr1);

    VocabularyRelease vr2 = new VocabularyRelease();
    vr2.setVocabularyKey(vocabulary.getKey());
    vr2.setVersion("2.0");
    vr2.setCreatedBy("test");
    vr2.setExportUrl("dummy url");
    vocabularyReleaseMapper.create(vr2);

    VocabularyRelease vr3 = new VocabularyRelease();
    vr3.setVocabularyKey(vocabulary.getKey());
    vr3.setVersion("3.0");
    vr3.setCreatedBy("test");
    vr3.setExportUrl("dummy url");
    vocabularyReleaseMapper.create(vr3);

    // test the list service
    assertEquals(3, exportService.listReleases(vocabulary.getName(), null, null).size());
    assertEquals(
        1, exportService.listReleases(vocabulary.getName(), null, new PagingRequest(0, 1)).size());
    assertThrows(
        IllegalArgumentException.class, () -> exportService.listReleases("foo", null, null).size());
    assertEquals(0, exportService.listReleases(vocabulary.getName(), "foo", null).size());

    // specific version
    List<VocabularyRelease> releases =
        exportService.listReleases(vocabulary.getName(), vr1.getVersion(), null);
    assertEquals(1, releases.size());
    assertEquals(vr1.getKey(), releases.get(0).getKey());

    // latest version test
    releases = exportService.listReleases(vocabulary.getName(), "latest", null);
    assertEquals(1, releases.size());
    assertEquals(vr3.getKey(), releases.get(0).getKey());

    releases = exportService.listReleases(vocabulary.getName(), "LATEST", new PagingRequest(0, 3));
    assertEquals(1, releases.size());
    assertEquals(vr3.getKey(), releases.get(0).getKey());
  }

  static class ContexInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
              "spring.datasource.url=" + database.getPostgresContainer().getJdbcUrl(),
              "spring.datasource.username=" + database.getPostgresContainer().getUsername(),
              "spring.datasource.password=" + database.getPostgresContainer().getPassword())
          .applyTo(configurableApplicationContext.getEnvironment());
    }
  }
}
