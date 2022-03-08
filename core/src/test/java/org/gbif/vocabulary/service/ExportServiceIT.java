/*
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
package org.gbif.vocabulary.service;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.vocabulary.PostgresDBExtension;
import org.gbif.vocabulary.TestUtils;
import org.gbif.vocabulary.model.UserRoles;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyRelease;
import org.gbif.vocabulary.model.export.ExportParams;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyReleaseMapper;
import org.gbif.vocabulary.service.export.ReleasePersister;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

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
  @MockBean private ReleasePersister releasePersister;

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
    vocabulary.setName(TestUtils.getRandomName());
    vocabulary.setCreatedBy("test");
    vocabulary.setModifiedBy("test");
    vocabularyMapper.create(vocabulary);

    // create releases
    VocabularyRelease vr1 = new VocabularyRelease();
    vr1.setVocabularyKey(vocabulary.getKey());
    vr1.setVersion("1.0");
    vr1.setCreatedBy("test");
    vr1.setExportUrl("dummy url");
    vr1.setComment("comment");
    vocabularyReleaseMapper.create(vr1);

    VocabularyRelease vr2 = new VocabularyRelease();
    vr2.setVocabularyKey(vocabulary.getKey());
    vr2.setVersion("2.0");
    vr2.setCreatedBy("test");
    vr2.setExportUrl("dummy url");
    vr2.setComment("comment");
    vocabularyReleaseMapper.create(vr2);

    VocabularyRelease vr3 = new VocabularyRelease();
    vr3.setVocabularyKey(vocabulary.getKey());
    vr3.setVersion("3.0");
    vr3.setCreatedBy("test");
    vr3.setExportUrl("dummy url");
    vr3.setComment("comment");
    vocabularyReleaseMapper.create(vr3);

    // test the list service
    assertEquals(
        3, exportService.listReleases(vocabulary.getName(), null, null).getResults().size());
    assertEquals(
        1,
        exportService
            .listReleases(vocabulary.getName(), null, new PagingRequest(0, 1))
            .getResults()
            .size());
    assertThrows(
        IllegalArgumentException.class,
        () -> exportService.listReleases("foo", null, null).getResults().size());
    assertEquals(
        0, exportService.listReleases(vocabulary.getName(), "foo", null).getResults().size());

    // specific version
    List<VocabularyRelease> releases =
        exportService.listReleases(vocabulary.getName(), vr1.getVersion(), null).getResults();
    assertEquals(1, releases.size());
    assertEquals(vr1.getKey(), releases.get(0).getKey());

    // latest version test
    releases = exportService.listReleases(vocabulary.getName(), "latest", null).getResults();
    assertEquals(1, releases.size());
    assertEquals(vr3.getKey(), releases.get(0).getKey());

    releases =
        exportService
            .listReleases(vocabulary.getName(), "LATEST", new PagingRequest(0, 3))
            .getResults();
    assertEquals(1, releases.size());
    assertEquals(vr3.getKey(), releases.get(0).getKey());
  }

  @WithMockUser(authorities = UserRoles.VOCABULARY_ADMIN)
  @Test
  public void releaseVocabularyTest() throws IOException {
    // create vocabulary
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(TestUtils.getRandomName());
    vocabulary.setCreatedBy("test");
    vocabulary.setModifiedBy("test");
    vocabularyMapper.create(vocabulary);

    String exportUrl = "http://test.com";
    when(releasePersister.uploadToNexus(any(), any())).thenReturn(exportUrl);

    ExportParams exportParams =
        ExportParams.builder()
            .vocabularyName(vocabulary.getName())
            .version("1.0.0")
            .user("user")
            .comment("comment")
            .build();

    VocabularyRelease release = exportService.releaseVocabulary(exportParams);

    assertNotNull(release.getKey());
    assertEquals(exportUrl, release.getExportUrl());
    assertEquals(exportParams.getComment(), release.getComment());
    assertEquals(exportParams.getVersion(), release.getVersion());
    assertEquals(exportParams.getUser(), release.getCreatedBy());
    assertEquals(vocabulary.getKey(), release.getVocabularyKey());
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
