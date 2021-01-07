/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.vocabulary.restws.resources.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.DeprecateAction;
import org.gbif.vocabulary.api.DeprecateVocabularyAction;
import org.gbif.vocabulary.api.VocabularyReleaseParams;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyRelease;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.restws.resources.VocabularyResource;
import org.gbif.vocabulary.service.ExportService;
import org.gbif.vocabulary.service.VocabularyService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests the {@link VocabularyResource} using mocks for the server and service layers. */
public class VocabularyResourceTest extends BaseResourceTest<Vocabulary> {

  @MockBean private VocabularyService vocabularyService;
  @MockBean private ExportService exportService;

  @Test
  public void listVocabulariesTest() throws Exception {
    List<Vocabulary> vocabularies =
        ImmutableList.of(createEntity(), createEntity(), createEntity());

    when(vocabularyService.list(any(VocabularySearchParams.class), any(PagingRequest.class)))
        .thenReturn(
            new PagingResponse<>(new PagingRequest(), (long) vocabularies.size(), vocabularies));

    MvcResult mvcResult =
        mockMvc.perform(get(getBasePath())).andExpect(status().isOk()).andReturn();

    JsonNode rootNode = OBJECT_MAPPER.readTree(mvcResult.getResponse().getContentAsString());
    List<Vocabulary> resultList =
        OBJECT_MAPPER.convertValue(
            rootNode.get("results"), new TypeReference<List<Vocabulary>>() {});

    assertEquals(vocabularies.size(), resultList.size());
  }

  @Test
  public void getVocabularyTest() throws Exception {
    Vocabulary vocabulary = createEntity();
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);

    mockMvc
        .perform(get(getBasePath() + "/" + vocabulary.getName()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("name", equalTo(vocabulary.getName())));
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void createVocabularyTest() throws Exception {
    Vocabulary vocabularyToCreate = createEntity();
    when(vocabularyService.create(any(Vocabulary.class))).thenReturn(TEST_KEY);
    Vocabulary created = new Vocabulary();
    BeanUtils.copyProperties(vocabularyToCreate, created);
    created.setKey(TEST_KEY);
    when(vocabularyService.get(TEST_KEY)).thenReturn(created);

    mockMvc
        .perform(
            post(getBasePath())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(vocabularyToCreate)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith(getBasePath() + "/" + created.getName())))
        .andExpect(jsonPath("key", is(TEST_KEY.intValue())))
        .andExpect(jsonPath("name", equalTo(created.getName())));
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void updateTest() throws Exception {
    Vocabulary vocabulary = createEntity();
    vocabulary.setKey(TEST_KEY);

    doNothing().when(vocabularyService).update(any(Vocabulary.class));
    when(vocabularyService.get(TEST_KEY)).thenReturn(vocabulary);

    mockMvc
        .perform(
            put(getBasePath() + "/" + vocabulary.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(vocabulary)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("key", is(vocabulary.getKey().intValue())))
        .andExpect(jsonPath("name", equalTo(vocabulary.getName())));
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void updateWrongNameTest() throws Exception {
    // mock not set, so the service returns null
    mockMvc
        .perform(
            put(getBasePath() + "/fake")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createEntity())))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  public void suggestTest() throws Exception {
    List<KeyNameResult> suggestions = createSuggestions();
    when(vocabularyService.suggest(anyString())).thenReturn(suggestions);
    suggestTest(suggestions);
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void deprecateTest() throws Exception {
    Vocabulary vocabulary = createEntity();
    vocabulary.setKey(TEST_KEY);
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);
    doNothing().when(vocabularyService).deprecate(anyLong(), anyString(), anyLong(), anyBoolean());

    mockMvc
        .perform(
            put(getBasePath() + "/" + vocabulary.getName() + "/deprecate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createDeprecateAction())))
        .andExpect(status().isNoContent());
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void restoreDeprecatedTest() throws Exception {
    Vocabulary vocabulary = createEntity();
    vocabulary.setKey(TEST_KEY);
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);
    doNothing().when(vocabularyService).restoreDeprecated(anyLong(), anyBoolean());

    mockMvc
        .perform(delete(getBasePath() + "/" + vocabulary.getName() + "/deprecate"))
        .andExpect(status().isNoContent());
  }

  @Test
  public void releaseVocabularyUnauthorizedTest() throws Exception {
    mockMvc
        .perform(
            post(getBasePath() + "/foo/" + VOCABULARY_RELEASES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    OBJECT_MAPPER.writeValueAsString(
                        new VocabularyReleaseParams("1.0", "comment"))))
        .andExpect(status().isUnauthorized());
  }

  @WithMockUser(authorities = {"VOCABULARY_EDITOR"})
  @Test
  public void releaseVocabularyForbiddenForEditorsTest() throws Exception {
    mockMvc
        .perform(
            post(getBasePath() + "/foo/" + VOCABULARY_RELEASES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    OBJECT_MAPPER.writeValueAsString(
                        new VocabularyReleaseParams("1.0", "comment"))))
        .andExpect(status().isForbidden());
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void releaseVocabularyVersionTest() throws Exception {
    Vocabulary vocabulary = createEntity();
    vocabulary.setKey(TEST_KEY);
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);

    VocabularyRelease release = new VocabularyRelease();
    release.setExportUrl("/test.zip");
    release.setVocabularyKey(vocabulary.getKey());
    release.setVersion("1.0");
    when(exportService.releaseVocabulary(any())).thenReturn(release);

    // do the call
    String url = getBasePath() + "/" + vocabulary.getName() + "/" + VOCABULARY_RELEASES_PATH;
    MvcResult mvcResult =
        mockMvc
            .perform(
                post(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        OBJECT_MAPPER.writeValueAsString(
                            new VocabularyReleaseParams("1.0", "comment"))))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith(url + "/" + release.getVersion())))
            .andReturn();

    // assert result
    VocabularyRelease releaseReturned =
        OBJECT_MAPPER.readValue(
            mvcResult.getResponse().getContentAsString(), VocabularyRelease.class);
    assertTrue(release.lenientEquals(releaseReturned));
  }

  @Test
  public void listReleasesTest() throws Exception {
    Vocabulary vocabulary = createEntity();
    vocabulary.setKey(TEST_KEY);
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);

    VocabularyRelease vr1 = new VocabularyRelease();
    vr1.setExportUrl("/test.zip");
    vr1.setVocabularyKey(vocabulary.getKey());
    vr1.setVersion("1.0");

    VocabularyRelease vr2 = new VocabularyRelease();
    vr2.setExportUrl("/test2.zip");
    vr2.setVocabularyKey(vocabulary.getKey());
    vr2.setVersion("2.0");

    when(exportService.listReleases(anyString(), any(), any()))
        .thenReturn(new PagingResponse<>(new PagingRequest(0, 5), 2L, Arrays.asList(vr1, vr2)));

    MvcResult mvcResult =
        mockMvc
            .perform(
                get(getBasePath() + "/" + vocabulary.getName() + "/" + VOCABULARY_RELEASES_PATH))
            .andExpect(status().isOk())
            .andReturn();

    // assert result
    JsonNode rootNode = OBJECT_MAPPER.readTree(mvcResult.getResponse().getContentAsString());
    List<VocabularyRelease> resultList =
        OBJECT_MAPPER.convertValue(
            rootNode.get("results"), new TypeReference<List<VocabularyRelease>>() {});
    assertEquals(2, resultList.size());
  }

  @Test
  public void getReleaseTest() throws Exception {
    Vocabulary vocabulary = createEntity();
    vocabulary.setKey(TEST_KEY);
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);

    VocabularyRelease vr1 = new VocabularyRelease();
    vr1.setExportUrl("/test.zip");
    vr1.setVocabularyKey(vocabulary.getKey());
    vr1.setVersion("1.0");

    when(exportService.listReleases(anyString(), anyString(), any()))
        .thenReturn(
            new PagingResponse<>(new PagingRequest(0, 5), 2L, Collections.singletonList(vr1)));

    MvcResult mvcResult =
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
            .andExpect(status().isOk())
            .andReturn();

    // assert result
    VocabularyRelease release =
        OBJECT_MAPPER.readValue(
            mvcResult.getResponse().getContentAsString(), VocabularyRelease.class);
    assertTrue(vr1.lenientEquals(release));
  }

  @Override
  String getBasePath() {
    return "/" + VOCABULARIES_PATH;
  }

  @Override
  Vocabulary createEntity() {
    return super.createVocabulary(UUID.randomUUID().toString());
  }

  @Override
  DeprecateAction createDeprecateAction() {
    return new DeprecateVocabularyAction();
  }
}
