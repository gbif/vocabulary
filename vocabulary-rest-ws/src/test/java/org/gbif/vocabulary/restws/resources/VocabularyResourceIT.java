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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.api.VocabularyListParams;
import org.gbif.vocabulary.api.VocabularyView;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.export.Export;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** IT for the {@link VocabularyResource}. */
@ContextConfiguration(initializers = {VocabularyResourceIT.ContexInitializer.class})
public class VocabularyResourceIT extends BaseResourceIT<Vocabulary> {

  private static final String CLEAN_DB_SCRIPT = "/clean-db.sql";

  private static final String TEST_NAMESPACE = "ns";

  VocabularyResourceIT(@LocalServerPort int localServerPort) {
    super(Vocabulary.class, localServerPort);
  }

  @Test
  public void listTest() {
    final String namespace = "listns";

    // create entity
    Vocabulary v1 = createEntity();
    v1.setNamespace(namespace);
    vocabularyClient.create(v1);
    VocabularyView view1 = vocabularyClient.get(v1.getName());
    assertNotNull(view1.getVocabulary().getKey());
    assertTrue(view1.getLabelsLink().endsWith("/labels"));

    // list entities
    PagingResponse<VocabularyView> vocabularies =
        vocabularyClient.listVocabularies(
            VocabularyListParams.builder().namespace(namespace).build());
    assertEquals(1, vocabularies.getResults().size());

    // create entity
    Vocabulary v2 = createEntity();
    v2.setNamespace(namespace);
    VocabularyView view2 = vocabularyClient.create(v2);
    assertNotNull(view2.getVocabulary().getKey());
    assertTrue(view2.getLabelsLink().endsWith("/labels"));

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
    VocabularyView vView1 = vocabularyClient.create(v1);
    assertNotNull(vView1.getVocabulary().getKey());

    Concept c1 = new Concept();
    c1.setName("C1");
    c1.setVocabularyKey(v1.getKey());
    ConceptView cView1 = conceptClient.create(v1.getName(), c1);

    byte[] bytesResponse = vocabularyClient.exportVocabulary(v1.getName());
    Export export = OBJECT_MAPPER.readValue(bytesResponse, Export.class);
    assertNotNull(export.getMetadata().getCreatedDate());
    assertEquals(v1.getName(), export.getVocabularyExport().getVocabulary().getName());
    assertEquals(1, export.getConceptExports().size());

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

  @Test
  public void deleteVocabularyTest() {
    // create entity
    Vocabulary v1 = createEntity();
    vocabularyClient.create(v1);
    assertNotNull(vocabularyClient.get(v1.getName()));

    // delete
    vocabularyClient.deleteVocabulary(v1.getName());
    assertNull(vocabularyClient.get(v1.getName()));
  }

  @Test
  void labelsTest() {
    Vocabulary v1 = createEntity();
    VocabularyView vView1 = vocabularyClient.create(v1);

    Label label =
        Label.builder()
            .entityKey(vView1.getEntity().getKey())
            .language(LanguageRegion.ENGLISH)
            .value("Label")
            .build();

    Label createdLabel = vocabularyClient.addLabel(v1.getName(), label);
    label.setKey(createdLabel.getKey());
    assertEquals(label, createdLabel);

    assertEquals(createdLabel, vocabularyClient.getLabel(v1.getName(), createdLabel.getKey()));

    List<Label> labelList = vocabularyClient.listLabels(v1.getName());
    assertEquals(1, labelList.size());
    assertEquals(createdLabel, labelList.get(0));

    label.setValue("Label2");
    Label updatedLabel = vocabularyClient.updateLabel(v1.getName(), label);
    assertEquals(label, updatedLabel);

    vocabularyClient.deleteLabel(v1.getName(), updatedLabel.getKey());
    assertEquals(0, vocabularyClient.listLabels(v1.getName()).size());
  }

  @Override
  Vocabulary createEntity() {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName("N" + UUID.randomUUID().toString().replace("-", ""));
    vocabulary.setNamespace(TEST_NAMESPACE);
    vocabulary.setEditorialNotes(Arrays.asList("note1", "note2"));
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
