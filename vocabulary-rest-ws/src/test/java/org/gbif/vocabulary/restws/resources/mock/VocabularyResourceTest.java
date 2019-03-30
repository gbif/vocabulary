package org.gbif.vocabulary.restws.resources.mock;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.restws.model.DeprecateAction;
import org.gbif.vocabulary.restws.model.DeprecateVocabularyAction;
import org.gbif.vocabulary.restws.resources.VocabularyResource;
import org.gbif.vocabulary.service.VocabularyService;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
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
@Execution(ExecutionMode.SAME_THREAD)
public class VocabularyResourceTest extends BaseResourceTest<Vocabulary> {

  @MockBean private VocabularyService vocabularyService;

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
        .andExpect(jsonPath("key", is(TEST_KEY)))
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
        .andExpect(jsonPath("key", is(vocabulary.getKey())))
        .andExpect(jsonPath("name", equalTo(vocabulary.getName())));
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void updateWrongNameTest() throws Exception {
    mockMvc
        .perform(
            put(getBasePath() + "/fake")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createEntity())))
        .andExpect(status().isBadRequest());
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
    doNothing().when(vocabularyService).deprecate(anyInt(), anyString(), anyInt(), anyBoolean());

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
    doNothing().when(vocabularyService).restoreDeprecated(anyInt(), anyBoolean());

    mockMvc
        .perform(delete(getBasePath() + "/" + vocabulary.getName() + "/deprecate"))
        .andExpect(status().isNoContent());
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
