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
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.api.VocabularyListParams;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Definition;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.export.Export;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import static org.gbif.vocabulary.model.utils.PathUtils.VOCABULARIES_PATH;
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
    v1 = vocabularyClient.get(v1.getName());
    assertNotNull(v1.getKey());

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
  void definitionTest() {
    Vocabulary v1 = createEntity();
    v1 = vocabularyClient.create(v1);

    Definition definition =
        Definition.builder().language(LanguageRegion.ENGLISH).value("Label").build();

    Definition createdDefinition = vocabularyClient.addDefinition(v1.getName(), definition);
    definition.setKey(createdDefinition.getKey());
    assertTrue(definition.lenientEquals(createdDefinition));

    assertEquals(
        createdDefinition,
        vocabularyClient.getDefinition(v1.getName(), createdDefinition.getKey()));

    List<Definition> definitionList =
        vocabularyClient.listDefinitions(v1.getName(), Collections.emptyList());
    assertEquals(1, definitionList.size());
    assertTrue(createdDefinition.lenientEquals(definitionList.get(0)));
    assertEquals(createdDefinition.getKey(), definitionList.get(0).getKey());

    assertEquals(
        1,
        vocabularyClient
            .listDefinitions(v1.getName(), Collections.singletonList(LanguageRegion.ENGLISH))
            .size());
    assertEquals(
        0,
        vocabularyClient
            .listDefinitions(v1.getName(), Collections.singletonList(LanguageRegion.SPANISH))
            .size());

    definition.setValue("Label2");
    Definition updatedDefinition = vocabularyClient.updateDefinition(v1.getName(), definition);
    assertTrue(definition.lenientEquals(updatedDefinition));

    vocabularyClient.deleteDefinition(v1.getName(), updatedDefinition.getKey());
    assertEquals(0, vocabularyClient.listDefinitions(v1.getName(), Collections.emptyList()).size());
  }

  @Test
  void labelsTest() {
    Vocabulary v1 = createEntity();
    v1 = vocabularyClient.create(v1);

    Label label = Label.builder().language(LanguageRegion.ENGLISH).value("Label").build();

    Long labelKey = vocabularyClient.addLabel(v1.getName(), label);
    label.setKey(labelKey);
    assertTrue(labelKey > 0);

    List<Label> labelList = vocabularyClient.listLabels(v1.getName(), Collections.emptyList());
    assertEquals(1, labelList.size());
    assertEquals(labelKey, labelList.get(0).getKey());
    assertTrue(label.lenientEquals(labelList.get(0)));

    assertEquals(
        1,
        vocabularyClient
            .listLabels(v1.getName(), Collections.singletonList(LanguageRegion.ENGLISH))
            .size());
    assertEquals(
        0,
        vocabularyClient
            .listLabels(v1.getName(), Collections.singletonList(LanguageRegion.SPANISH))
            .size());

    vocabularyClient.deleteLabel(v1.getName(), labelKey);
    assertEquals(0, vocabularyClient.listLabels(v1.getName(), Collections.emptyList()).size());
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
