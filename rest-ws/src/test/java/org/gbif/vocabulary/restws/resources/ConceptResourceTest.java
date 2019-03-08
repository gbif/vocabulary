package org.gbif.vocabulary.restws.resources;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.restws.model.DeprecateAction;
import org.gbif.vocabulary.restws.model.DeprecateConceptAction;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.VocabularyService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

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

/** Tests the {@link ConceptResource} using mocks for the server and service layers. */
@Execution(ExecutionMode.SAME_THREAD)
public class ConceptResourceTest extends BaseResourceTest<Concept> {

  private static final String TEST_VOCABULARY_NAME = "v1";
  private static final int TEST_VOCABULARY_KEY = 1;

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
    List<Vocabulary> resultList =
        OBJECT_MAPPER.convertValue(rootNode.get("results"), new TypeReference<List<Concept>>() {});

    assertEquals(concepts.size(), resultList.size());
  }

  @Test
  public void listConceptsUnknownVocabularyTest() throws Exception {
    mockMvc.perform(get(getBasePath())).andExpect(status().isBadRequest());
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

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
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
        .andExpect(jsonPath("key", is(TEST_KEY)))
        .andExpect(jsonPath("name", equalTo(created.getName())));
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void createConceptWithWrongVocabulary() throws Exception {
    mockVocabulary();
    Concept concept = createEntity();
    concept.setVocabularyKey(TEST_VOCABULARY_KEY + 1);

    mockMvc
        .perform(
            post(getBasePath())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(concept)))
        .andExpect(status().isBadRequest());
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
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
        .andExpect(jsonPath("key", is(concept.getKey())))
        .andExpect(jsonPath("name", equalTo(concept.getName())));
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
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
        .andExpect(status().isBadRequest());
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void updateConceptWithWrongConceptName() throws Exception {
    mockVocabulary();
    mockMvc
        .perform(
            put(getBasePath() + "/fake")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createEntity())))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void suggestTest() throws Exception {
    mockVocabulary();
    List<KeyNameResult> suggestions = createSuggestions();
    when(conceptService.suggest(anyString(), anyInt())).thenReturn(suggestions);
    suggestTest(suggestions);
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void deprecateConceptTest() throws Exception {
    Concept concept = createEntity();
    concept.setKey(TEST_KEY);
    when(conceptService.getByNameAndVocabulary(concept.getName(), TEST_VOCABULARY_NAME))
        .thenReturn(concept);
    doNothing().when(conceptService).deprecate(anyInt(), anyString(), anyInt(), anyBoolean());

    mockMvc
        .perform(
            put(getBasePath() + "/" + concept.getName() + "/deprecate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createDeprecateAction())))
        .andExpect(status().isNoContent());
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void restoreDeprecatedConceptTest() throws Exception {
    Concept concept = createEntity();
    concept.setKey(TEST_KEY);
    when(conceptService.getByNameAndVocabulary(concept.getName(), TEST_VOCABULARY_NAME))
        .thenReturn(concept);
    doNothing().when(conceptService).restoreDeprecated(anyInt(), anyBoolean());

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
    return "/vocabularies/" + TEST_VOCABULARY_NAME + "/concepts";
  }

  @Override
  Concept createEntity() {
    Concept concept = new Concept();
    concept.setVocabularyKey(TEST_VOCABULARY_KEY);
    concept.setName(UUID.randomUUID().toString());
    concept.setLabel(Collections.singletonMap(Language.ENGLISH, "Label"));
    concept.setAlternativeLabels(
        Collections.singletonMap(Language.ENGLISH, Arrays.asList("Label2", "Label3")));
    concept.setEditorialNotes(Arrays.asList("note1", "note2"));

    return concept;
  }

  @Override
  DeprecateAction createDeprecateAction() {
    return new DeprecateConceptAction();
  }
}
