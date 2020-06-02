package org.gbif.vocabulary.restws.resources.documentation;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.restws.model.DeprecateConceptAction;
import org.gbif.vocabulary.restws.model.DeprecateVocabularyAction;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.VocabularyService;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
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

/** Generates the documentation for the vocabulary API. */
public class VocabularyTestDoc extends DocumentationBaseTest {

  @MockBean private VocabularyService vocabularyService;
  @MockBean private ConceptService conceptService;

  @Test
  public void listVocabularyTest() throws Exception {
    List<Vocabulary> vocabularies =
        ImmutableList.of(
            createVocabulary("vocab1"), createVocabulary("vocab2"), createVocabulary("vocab3"));

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
    when(vocabularyService.suggest(anyString())).thenReturn(suggestions);
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

    mockMvc
        .perform(get(getBasePath() + "/" + vocabulary.getName() + "/export"))
        .andExpect(status().isOk())
        .andExpect(header().exists("Content-Disposition"));
  }

  @Override
  String getBasePath() {
    return "/" + VOCABULARIES_PATH;
  }
}
