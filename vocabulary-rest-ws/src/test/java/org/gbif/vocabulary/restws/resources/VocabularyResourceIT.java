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
package org.gbif.vocabulary.restws.resources;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.VocabularyListParams;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.export.VocabularyExport;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** IT for the {@link VocabularyResource}. */
@ContextConfiguration(initializers = {VocabularyResourceIT.ContexInitializer.class})
public class VocabularyResourceIT extends BaseResourceIT<Vocabulary> {

  private static final String CLEAN_DB_SCRIPT = "/clean-db.sql";

  private static final String TEST_NAMESPACE = "ns";

  VocabularyResourceIT(@LocalServerPort int localServerPort) {
    super(Vocabulary.class, localServerPort);
  }

  @Test
  void listTest() {
    final String namespace = "listns";

    // create entity
    Vocabulary v1 = createEntity();
    v1.setNamespace(namespace);
    vocabularyClient.create(v1);
    v1 = vocabularyClient.get(v1.getName());

    // list entities
    PagingResponse<Vocabulary> vocabularies =
        vocabularyClient.listVocabularies(
            VocabularyListParams.builder().namespace(namespace).build());
    assertEquals(1, vocabularies.getResults().size());

    // create entity
    Vocabulary v2 = createEntity();
    v2.setNamespace(namespace);
    v2 = vocabularyClient.create(v2);
    assertNotNull(v2.getKey());

    // list entities
    vocabularies =
        vocabularyClient.listVocabularies(
            VocabularyListParams.builder().namespace(namespace).build());
    assertEquals(2, vocabularies.getResults().size());
  }

  @Test
  public void exportVocabularyTest() throws IOException {
    // create entity
    Vocabulary v1 = createEntity();
    v1 = vocabularyClient.create(v1);
    assertNotNull(v1.getKey());

    Concept c1 = new Concept();
    c1.setName("C1");
    c1.setVocabularyKey(v1.getKey());
    c1 = conceptClient.create(v1.getName(), c1);

    byte[] bytesResponse = vocabularyClient.exportVocabulary(v1.getName());
    VocabularyExport export = OBJECT_MAPPER.readValue(bytesResponse, VocabularyExport.class);
    assertNotNull(export.getMetadata().getCreatedDate());
    assertEquals(v1.getName(), export.getVocabulary().getName());
    assertEquals(1, export.getConcepts().size());

    // test the headers in the response
    webClient
        .get()
        .uri(getBasePath() + "/" + v1.getName() + "/export")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .exists("Content-Disposition");
  }

  @Override
  Vocabulary createEntity() {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName("N" + UUID.randomUUID().toString().replace("-", ""));
    vocabulary.setNamespace(TEST_NAMESPACE);
    vocabulary.setEditorialNotes(Arrays.asList("note1", "note2"));
    vocabulary.setLabel(
        Collections.singletonMap(LanguageRegion.ENGLISH, UUID.randomUUID().toString()));
    return vocabulary;
  }

  @Override
  String getCleanDbScript() {
    return CLEAN_DB_SCRIPT;
  }

  @Override
  String getBasePath() {
    return "/" + VOCABULARIES_PATH;
  }

  static class ContexInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
              "spring.datasource.url=" + database.getPostgresContainer().getJdbcUrl(),
              "spring.datasource.username=" + database.getPostgresContainer().getUsername(),
              "spring.datasource.password=" + database.getPostgresContainer().getPassword(),
              "security.loginApiBasePath=" + loginServer.getWireMockServer().baseUrl())
          .applyTo(configurableApplicationContext.getEnvironment());
    }
  }
}
