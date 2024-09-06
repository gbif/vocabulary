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
package org.gbif.vocabulary.restws.resources.mock;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.api.DeprecateAction;
import org.gbif.vocabulary.api.DeprecateConceptAction;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.UserRoles;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.SuggestResult;
import org.gbif.vocabulary.restws.resources.ConceptResource;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.VocabularyService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;

import static org.gbif.vocabulary.model.utils.PathUtils.CONCEPTS_PATH;
import static org.gbif.vocabulary.model.utils.PathUtils.VOCABULARIES_PATH;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** Tests the {@link ConceptResource} using mocks for the server and service layers. */
public class ConceptResourceTest extends BaseResourceTest<Concept> {

  private static final String TEST_VOCABULARY_NAME = "v1";
  private static final long TEST_VOCABULARY_KEY = 1;

  @MockBean private ConceptService conceptService;
  @MockBean private VocabularyService vocabularyService;

  @Test
  public void listConceptsTest() throws Exception {
    List<Concept> concepts = ImmutableList.of(createEntity(), createEntity(), createEntity());

    when(vocabularyService.getByName(anyString()))
        .thenReturn(createVocabulary(TEST_VOCABULARY_NAME));
    when(conceptService.list(any(ConceptSearchParams.class), any(PagingRequest.class)))
        .thenReturn(new PagingResponse<>(new PagingRequest(), (long) concepts.size(), concepts));

    MvcResult mvcResult =
        mockMvc.perform(get(getBasePath())).andExpect(status().isOk()).andReturn();

    JsonNode rootNode = OBJECT_MAPPER.readTree(mvcResult.getResponse().getContentAsString());
    List<ConceptView> resultList =
        OBJECT_MAPPER.convertValue(
            rootNode.get("results"), new TypeReference<List<ConceptView>>() {});

    assertEquals(concepts.size(), resultList.size());
  }

  @Test
  public void listConceptsUnknownVocabularyTest() throws Exception {
    // mock not set, so the service returns null
    mockMvc.perform(get(getBasePath())).andExpect(status().isNotFound());
  }

  @Test
  public void getConceptTest() throws Exception {
    Concept concept = createEntity();
    when(conceptService.getByNameAndVocabulary(anyString(), anyString())).thenReturn(concept);

    mockMvc
        .perform(get(getBasePath() + "/" + concept.getName()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("name", Matchers.equalTo(concept.getName())));
  }

  @Test
  public void getConceptWithParentsTest() throws Exception {
    Concept concept = createEntity();
    concept.setKey(1L);
    concept.setParentKey(1L);
    when(conceptService.getByNameAndVocabulary(anyString(), anyString())).thenReturn(concept);

    final String parentName = "parent1";
    when(conceptService.findParents(1)).thenReturn(Collections.singletonList(parentName));

    mockMvc
        .perform(get(getBasePath() + "/" + concept.getName()).param("includeParents", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("name", Matchers.equalTo(concept.getName())))
        .andExpect(jsonPath("parents", hasItem(parentName)));
  }

  @WithMockUser(authorities = {UserRoles.VOCABULARY_ADMIN})
  @Test
  public void createConceptTest() throws Exception {
    mockVocabulary();
    when(conceptService.create(any(Concept.class))).thenReturn(TEST_KEY);
    Concept conceptToCreate = createEntity();
    Concept created = new Concept();
    BeanUtils.copyProperties(conceptToCreate, created);
    created.setKey(TEST_KEY);
    when(conceptService.get(TEST_KEY)).thenReturn(created);

    mockMvc
        .perform(
            post(getBasePath())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(conceptToCreate)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith(getBasePath() + "/" + created.getName())))
        .andExpect(jsonPath("key", is(TEST_KEY.intValue())))
        .andExpect(jsonPath("name", equalTo(created.getName())));
  }

  @WithMockUser(authorities = {UserRoles.VOCABULARY_ADMIN})
  @Test
  public void updateConceptTest() throws Exception {
    mockVocabulary();
    doNothing().when(conceptService).update(any(Concept.class));
    Concept concept = createEntity();
    concept.setKey(TEST_KEY);
    when(conceptService.get(TEST_KEY)).thenReturn(concept);

    mockMvc
        .perform(
            put(getBasePath() + "/" + concept.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(concept)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("key", is(concept.getKey().intValue())))
        .andExpect(jsonPath("name", equalTo(concept.getName())));
  }

  @WithMockUser(authorities = {UserRoles.VOCABULARY_ADMIN})
  @Test
  public void updateConceptWithWrongVocabulary() throws Exception {
    mockVocabulary();
    Concept concept = createEntity();
    concept.setVocabularyKey(TEST_VOCABULARY_KEY + 1);

    mockMvc
        .perform(
            put(getBasePath() + "/" + concept.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(concept)))
        .andExpect(status().isUnprocessableEntity());
  }

  @WithMockUser(authorities = {UserRoles.VOCABULARY_ADMIN})
  @Test
  public void updateConceptWithWrongConceptName() throws Exception {
    mockVocabulary();
    mockMvc
        .perform(
            put(getBasePath() + "/fake")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createEntity())))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  public void suggestTest() throws Exception {
    mockVocabulary();
    List<SuggestResult> suggestions = createSuggestions();
    when(conceptService.suggest(anyString(), anyLong(), any(), any(), any()))
        .thenReturn(suggestions);
    suggestTest(suggestions);
  }

  @WithMockUser(authorities = {UserRoles.VOCABULARY_ADMIN})
  @Test
  public void deprecateConceptTest() throws Exception {
    Concept concept = createEntity();
    concept.setKey(TEST_KEY);
    when(conceptService.getByNameAndVocabulary(concept.getName(), TEST_VOCABULARY_NAME))
        .thenReturn(concept);
    doNothing().when(conceptService).deprecate(anyLong(), anyString(), anyLong(), anyBoolean());

    mockMvc
        .perform(
            put(getBasePath() + "/" + concept.getName() + "/deprecate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createDeprecateAction())))
        .andExpect(status().isNoContent());
  }

  @WithMockUser(authorities = {UserRoles.VOCABULARY_ADMIN})
  @Test
  public void restoreDeprecatedConceptTest() throws Exception {
    Concept concept = createEntity();
    concept.setKey(TEST_KEY);
    when(conceptService.getByNameAndVocabulary(concept.getName(), TEST_VOCABULARY_NAME))
        .thenReturn(concept);
    doNothing().when(conceptService).restoreDeprecated(anyLong(), anyBoolean());

    mockMvc
        .perform(delete(getBasePath() + "/" + concept.getName() + "/deprecate"))
        .andExpect(status().isNoContent());
  }

  private void mockVocabulary() {
    Vocabulary vocabulary = createVocabulary(TEST_VOCABULARY_NAME);
    vocabulary.setKey(TEST_VOCABULARY_KEY);
    when(vocabularyService.getByName(vocabulary.getName())).thenReturn(vocabulary);
  }

  @Override
  String getBasePath() {
    return "/" + VOCABULARIES_PATH + "/" + TEST_VOCABULARY_NAME + "/" + CONCEPTS_PATH;
  }

  @Override
  Concept createEntity() {
    Concept concept = new Concept();
    concept.setVocabularyKey(TEST_VOCABULARY_KEY);
    concept.setName(UUID.randomUUID().toString());
    concept.setEditorialNotes(Arrays.asList("note1", "note2"));

    return concept;
  }

  @Override
  DeprecateAction createDeprecateAction() {
    return new DeprecateConceptAction();
  }
}
