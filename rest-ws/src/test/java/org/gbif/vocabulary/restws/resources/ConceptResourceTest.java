package org.gbif.vocabulary.restws.resources;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.VocabularyService;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ConceptResourceTest extends BaseResourceTest<Concept> {

  private static final String DEFAULT_VOCABULARY = "v1";

  @MockBean private ConceptService conceptService;
  @MockBean private VocabularyService vocabularyService;

  @Test
  public void listConceptsTest() throws Exception {
    List<Concept> concepts = ImmutableList.of(createEntity(), createEntity(), createEntity());

    when(vocabularyService.getByName(anyString())).thenReturn(createVocabulary());
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
  public void listConceptsUnkownVocabularyTest() throws Exception {
    when(vocabularyService.getByName(anyString())).thenReturn(null);
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

  @Test
  public void getConceptNotFoundTest() throws Exception {
    when(conceptService.getByNameAndVocabulary(anyString(), anyString())).thenReturn(null);
    mockMvc.perform(get(getBasePath() + "/name")).andExpect(status().isNotFound());
  }

  @Override
  String getBasePath() {
    return "/vocabularies/" + DEFAULT_VOCABULARY + "/concepts";
  }

  @Override
  Concept createEntity() {
    return super.createConcept();
  }
}
