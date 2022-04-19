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
package org.gbif.vocabulary.restws.resources.documentation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.DeprecateConceptAction;
import org.gbif.vocabulary.api.DeprecateVocabularyAction;
import org.gbif.vocabulary.api.VocabularyReleaseParams;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyRelease;
import org.gbif.vocabulary.model.export.ExportMetadata;
import org.gbif.vocabulary.model.export.VocabularyExport;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.ExportService;
import org.gbif.vocabulary.service.VocabularyService;
import org.gbif.vocabulary.tools.VocabularyDownloader;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;

import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;
import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARY_RELEASES_PATH;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Generates the documentation for the vocabulary API. */
public class VocabularyTestDoc extends DocumentationBaseTest {

  @MockBean private VocabularyService vocabularyService;
  @MockBean private ConceptService conceptService;
  @MockBean private ExportService exportService;

  @Test
  public void listVocabularyTest() throws Exception {
    List<Vocabulary> vocabularies =
        ImmutableList.of(
            createVocabulary("vocab1"), createVocabulary("vocab2"), createVocabulary("vocab3"));

    when(vocabularyService.list(any(VocabularySearchParams.class), any(PagingRequest.class)))
        .thenReturn(
            new PagingResponse<>(new PagingRequest(), (long) vocabularies.size(), vocabularies));

    MvcResult mvcResult =
        mockMvc
            .perform(
                get(getBasePath())
                    .param("q", "test")
                    .param("name", "vocab1")
                    .param("namespace", "ns")
                    .param("deprecated", "true")
                    .param("key", "1")
                    .param("offset", "0")
                    .param("limit", "20"))
            .andExpect(status().isOk())
            .andDo(
                document(
                    "{class-name}/{method-name}",
                    requestParameters(
                        parameterWithName("q").description("Search query").optional(),
                        parameterWithName("name").description("Vocabulary name").optional(),
                        parameterWithName("namespace")
                            .description("Vocabulary namespace")
                            .optional(),
                        parameterWithName("deprecated")
                            .description(
                                "Boolean to search for deprecated or non-deprecated vocabularies")
                            .optional(),
                        parameterWithName("key").description("Vocabulary key").optional(),
                        parameterWithName("offset")
                            .description("Page offset. By default 0")
                            .optional(),
                        parameterWithName("limit")
                            .description("Page limit. By default 20")
                            .optional())))
            .andReturn();

    JsonNode rootNode = OBJECT_MAPPER.readTree(mvcResult.getResponse().getContentAsString());
    List<Vocabulary> resultList =
        OBJECT_MAPPER.convertValue(
            rootNode.get("results"), new TypeReference<List<Vocabulary>>() {});

    assertEquals(vocabularies.size(), resultList.size());
  }

  @Test
  public void getVocabularyTest() throws Exception {
    Vocabulary vocabulary = createVocabulary("vocab1");
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);

    mockMvc
        .perform(get(getBasePath() + "/" + vocabulary.getName()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("name", equalTo(vocabulary.getName())));
  }

  @Test
  public void createVocabularyTest() throws Exception {
    setSecurityContext();
    Vocabulary vocabularyToCreate = createVocabulary("vocab1");
    when(vocabularyService.create(any(Vocabulary.class))).thenReturn(TEST_KEY);
    Vocabulary created = new Vocabulary();
    BeanUtils.copyProperties(vocabularyToCreate, created);
    created.setKey(TEST_KEY);
    when(vocabularyService.get(TEST_KEY)).thenReturn(created);

    mockMvc
        .perform(
            post(getBasePath())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(vocabularyToCreate))
                .with(authorizationDocumentation()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith(getBasePath() + "/" + created.getName())))
        .andExpect(jsonPath("key", is(TEST_KEY.intValue())))
        .andExpect(jsonPath("name", equalTo(created.getName())))
        .andDo(documentFields(Vocabulary.class));
  }

  @Test
  public void updateVocabularyTest() throws Exception {
    setSecurityContext();
    Vocabulary vocabulary = createVocabulary("vocab1");
    vocabulary.setKey(TEST_KEY);

    doNothing().when(vocabularyService).update(any(Vocabulary.class));
    when(vocabularyService.get(TEST_KEY)).thenReturn(vocabulary);

    mockMvc
        .perform(
            put(getBasePath() + "/" + vocabulary.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(vocabulary))
                .with(authorizationDocumentation()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("key", is(vocabulary.getKey().intValue())))
        .andExpect(jsonPath("name", equalTo(vocabulary.getName())))
        .andDo(documentFields(Vocabulary.class));
  }

  @Test
  public void suggestVocabularyTest() throws Exception {
    List<KeyNameResult> suggestions = createSuggestions();
    when(vocabularyService.suggest(anyString(), any())).thenReturn(suggestions);
    suggestTest(suggestions);
  }

  @Test
  public void deprecateVocabularyTest() throws Exception {
    setSecurityContext();
    Vocabulary vocabulary = createVocabulary("vocab1");
    vocabulary.setKey(TEST_KEY);
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);
    doNothing().when(vocabularyService).deprecate(anyLong(), anyString(), anyLong(), anyBoolean());

    mockMvc
        .perform(
            put(getBasePath() + "/" + vocabulary.getName() + "/deprecate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(new DeprecateVocabularyAction()))
                .with(authorizationDocumentation()))
        .andExpect(status().isNoContent())
        .andDo(documentRequestFields(DeprecateConceptAction.class));
  }

  @Test
  public void restoreVocabularyTest() throws Exception {
    setSecurityContext();
    Vocabulary vocabulary = createVocabulary("vocab1");
    vocabulary.setKey(TEST_KEY);
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);
    doNothing().when(vocabularyService).restoreDeprecated(anyLong(), anyBoolean());

    mockMvc
        .perform(
            delete(getBasePath() + "/" + vocabulary.getName() + "/deprecate")
                .with(authorizationDocumentation()))
        .andExpect(status().isNoContent());
  }

  @Test
  public void exportVocabularyTest() throws Exception {
    Vocabulary vocabulary = createVocabulary("exportableVocab");
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);
    when(conceptService.list(any(), any())).thenReturn(new PagingResponse<Concept>(0L, 0, 0L));
    when(exportService.exportVocabulary(anyString()))
        .thenReturn(Files.createTempFile("export", ".json"));

    mockMvc
        .perform(get(getBasePath() + "/" + vocabulary.getName() + "/export"))
        .andExpect(status().isOk())
        .andExpect(header().exists("Content-Disposition"));
  }

  @Test
  public void releaseVocabularyVersionTest() throws Exception {
    setSecurityContext();
    Vocabulary vocabulary = createVocabulary("vocab");
    vocabulary.setKey(TEST_KEY);
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);

    VocabularyRelease vr1 = new VocabularyRelease();
    vr1.setExportUrl("/vocab-1.0.zip");
    vr1.setVocabularyKey(vocabulary.getKey());
    vr1.setVersion("1.0");

    when(exportService.releaseVocabulary(any())).thenReturn(vr1);

    mockMvc
        .perform(
            post(getBasePath() + "/" + vocabulary.getName() + "/" + VOCABULARY_RELEASES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    OBJECT_MAPPER.writeValueAsString(
                        new VocabularyReleaseParams("1.0", "comments"))))
        .andExpect(status().isCreated())
        .andDo(documentRequestFields(VocabularyReleaseParams.class));
  }

  @Test
  public void listVocabularyReleasesTest() throws Exception {
    Vocabulary vocabulary = createVocabulary("vocab");
    vocabulary.setKey(TEST_KEY);
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);

    VocabularyRelease vr1 = new VocabularyRelease();
    vr1.setExportUrl("/vocab-1.0.zip");
    vr1.setVocabularyKey(vocabulary.getKey());
    vr1.setVersion("1.0");

    when(exportService.listReleases(anyString(), anyString(), any()))
        .thenReturn(
            new PagingResponse<>(new PagingRequest(0, 5), 1L, Collections.singletonList(vr1)));

    mockMvc
        .perform(
            get(getBasePath() + "/" + vocabulary.getName() + "/" + VOCABULARY_RELEASES_PATH)
                .queryParam("version", "1.0"))
        .andExpect(status().isOk())
        .andReturn();
  }

  @Test
  public void getVocabularyReleaseTest() throws Exception {
    Vocabulary vocabulary = createVocabulary("vocab");
    vocabulary.setKey(TEST_KEY);
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);

    VocabularyRelease vr1 = new VocabularyRelease();
    vr1.setExportUrl("/vocab-1.0.zip");
    vr1.setVocabularyKey(vocabulary.getKey());
    vr1.setVersion("latest");

    when(exportService.listReleases(anyString(), anyString(), any()))
        .thenReturn(
            new PagingResponse<>(new PagingRequest(0, 5), 1L, Collections.singletonList(vr1)));

    mockMvc
        .perform(
            get(
                getBasePath()
                    + "/"
                    + vocabulary.getName()
                    + "/"
                    + VOCABULARY_RELEASES_PATH
                    + "/"
                    + vr1.getVersion()))
        .andExpect(status().isOk());
  }

  @Test
  public void getReleaseExportTest() throws Exception {
    Vocabulary vocabulary = createVocabulary("vocab");
    vocabulary.setKey(TEST_KEY);
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);

    VocabularyRelease vr1 = new VocabularyRelease();
    vr1.setExportUrl("/vocab-1.0.zip");
    vr1.setVocabularyKey(vocabulary.getKey());
    vr1.setVersion("latest");

    when(exportService.listReleases(anyString(), anyString(), any()))
        .thenReturn(
            new PagingResponse<>(new PagingRequest(0, 5), 1L, Collections.singletonList(vr1)));

    try (MockedStatic<VocabularyDownloader> vocabDownloader =
        mockStatic(VocabularyDownloader.class)) {

      VocabularyExport export = new VocabularyExport();

      ExportMetadata metadata = new ExportMetadata();
      metadata.setVersion("1.0.0");
      metadata.setCreatedDate(LocalDateTime.now());
      export.setMetadata(metadata);

      export.setVocabulary(vocabulary);

      Concept concept = new Concept();
      concept.setKey(1l);
      concept.setName("Concept");
      concept.setLabel(Collections.singletonMap(LanguageRegion.ENGLISH, "concept"));
      export.setConcepts(Collections.singletonList(concept));

      Path exportPath = Files.createTempFile("export", "json");
      Files.write(exportPath, OBJECT_MAPPER.writeValueAsBytes(export));

      vocabDownloader
          .when(() -> VocabularyDownloader.downloadVocabularyExport(vr1.getExportUrl()))
          .thenReturn(exportPath);

      mockMvc
          .perform(
              get(
                  getBasePath()
                      + "/"
                      + vocabulary.getName()
                      + "/"
                      + VOCABULARY_RELEASES_PATH
                      + "/"
                      + vr1.getVersion()
                      + "/export"))
          .andExpect(status().isOk());
    }
  }

  @Test
  public void deleteVocabularyTest() throws Exception {
    Vocabulary vocabulary = createVocabulary("vocab1");
    vocabulary.setKey(1L);
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);
    doNothing().when(vocabularyService).deleteVocabulary(1L);

    mockMvc
        .perform(
            delete(getBasePath() + "/" + vocabulary.getName()).with(authorizationDocumentation()))
        .andExpect(status().isNoContent());
  }

  @Override
  String getBasePath() {
    return "/" + VOCABULARIES_PATH;
  }
}
