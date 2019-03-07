package org.gbif.vocabulary.restws.resources;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.restws.model.DeprecateVocabularyAction;
import org.gbif.vocabulary.service.VocabularyService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class VocabularyResourceTest extends BaseResourceTest {

  private static final String BASE_PATH = "/vocabularies";

  @MockBean private VocabularyService vocabularyService;

  @Test
  public void listVocabulariesTest() throws Exception {
    List<Vocabulary> vocabularies =
        ImmutableList.of(createVocabulary(), createVocabulary(), createVocabulary());

    VocabularySearchParams searchParams = VocabularySearchParams.builder().build();
    when(vocabularyService.list(any(VocabularySearchParams.class), any(PagingRequest.class)))
        .thenReturn(
            new PagingResponse<Vocabulary>(
                new PagingRequest(), (long) vocabularies.size(), vocabularies));

    MvcResult mvcResult = mockMvc.perform(get(BASE_PATH)).andExpect(status().isOk()).andReturn();

    JsonNode rootNode = OBJECT_MAPPER.readTree(mvcResult.getResponse().getContentAsString());
    List<Vocabulary> resultList =
        OBJECT_MAPPER.convertValue(
            rootNode.get("results"), new TypeReference<List<Vocabulary>>() {});

    assertEquals(vocabularies.size(), resultList.size());
  }

  @Test
  public void getVocabularyTest() throws Exception {
    Vocabulary vocabulary = createVocabulary();
    when(vocabularyService.getByName(anyString())).thenReturn(vocabulary);

    mockMvc
        .perform(get(BASE_PATH + "/foo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("name", equalTo(vocabulary.getName())));
  }

  @Test
  public void getNotFoundVocabularyTest() throws Exception {
    when(vocabularyService.getByName(anyString())).thenReturn(null);
    mockMvc.perform(get(BASE_PATH + "/foo")).andExpect(status().isNotFound());
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void createVocabularyTest() throws Exception {
    Vocabulary vocabulary = createVocabulary();
    when(vocabularyService.create(any(Vocabulary.class))).thenReturn(TEST_KEY);
    Vocabulary created = new Vocabulary();
    BeanUtils.copyProperties(vocabulary, created);
    created.setKey(TEST_KEY);
    when(vocabularyService.get(TEST_KEY)).thenReturn(created);

    mockMvc
        .perform(
            post(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(vocabulary)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith(BASE_PATH + "/" + created.getName())))
        .andExpect(jsonPath("key", is(TEST_KEY)))
        .andExpect(jsonPath("name", equalTo(vocabulary.getName())));
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void createNullEntityTest() throws Exception {
    Vocabulary vocabulary = createVocabulary();
    when(vocabularyService.create(any(Vocabulary.class))).thenReturn(TEST_KEY);
    Vocabulary created = new Vocabulary();
    BeanUtils.copyProperties(vocabulary, created);
    created.setKey(TEST_KEY);
    when(vocabularyService.get(TEST_KEY)).thenReturn(created);

    mockMvc
        .perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void unauthorizedCreateTest() throws Exception {
    mockMvc
        .perform(
            post(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createVocabulary())))
        .andExpect(status().isUnauthorized());
  }

  @WithMockUser
  @Test
  public void forbiddenCreateTest() throws Exception {
    mockMvc
        .perform(
            post(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createVocabulary())))
        .andExpect(status().isForbidden());
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void updateTest() throws Exception {
    Vocabulary vocabulary = createVocabulary();
    vocabulary.setKey(TEST_KEY);

    doNothing().when(vocabularyService).update(any(Vocabulary.class));
    when(vocabularyService.get(TEST_KEY)).thenReturn(vocabulary);

    mockMvc
        .perform(
            put(BASE_PATH + "/" + vocabulary.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(vocabulary)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("name", equalTo(vocabulary.getName())));
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void updateWrongNameTest() throws Exception {
    Vocabulary vocabulary = createVocabulary();
    vocabulary.setKey(TEST_KEY);

    doNothing().when(vocabularyService).update(any(Vocabulary.class));
    when(vocabularyService.get(TEST_KEY)).thenReturn(vocabulary);

    mockMvc
        .perform(
            put(BASE_PATH + "/fake")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(vocabulary)))
        .andExpect(status().isBadRequest());
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void updateNullEntityTest() throws Exception {
    mockMvc
        .perform(put(BASE_PATH + "/fake").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void unauthorizedUpdateTest() throws Exception {
    mockMvc
        .perform(
            put(BASE_PATH + "/fake")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createVocabulary())))
        .andExpect(status().isUnauthorized());
  }

  @WithMockUser()
  @Test
  public void forbiddenUpdateTest() throws Exception {
    mockMvc
        .perform(
            put(BASE_PATH + "/fake")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createVocabulary())))
        .andExpect(status().isForbidden());
  }

  @Test
  public void suggestTest() throws Exception {
    KeyNameResult keyNameResult1 = new KeyNameResult();
    keyNameResult1.setKey(1);
    keyNameResult1.setName("n1");
    KeyNameResult keyNameResult2 = new KeyNameResult();
    keyNameResult2.setKey(2);
    keyNameResult2.setName("n2");
    List<KeyNameResult> suggestions = ImmutableList.of(keyNameResult1, keyNameResult2);

    when(vocabularyService.suggest(anyString())).thenReturn(suggestions);

    MvcResult mvcResult =
        mockMvc.perform(get(BASE_PATH + "/suggest?q=foo")).andExpect(status().isOk()).andReturn();

    List<KeyNameResult> resultList =
        OBJECT_MAPPER.readValue(
            mvcResult.getResponse().getContentAsString(),
            new TypeReference<List<KeyNameResult>>() {});
    assertEquals(suggestions.size(), resultList.size());
  }

  @Test
  public void suggestWithoutQueryTest() throws Exception {
    mockMvc.perform(get(BASE_PATH + "/suggest")).andExpect(status().isBadRequest()).andReturn();
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void deprecateTest() throws Exception {
    Vocabulary vocabulary = createVocabulary();
    vocabulary.setKey(TEST_KEY);
    when(vocabularyService.getByName(anyString())).thenReturn(vocabulary);
    doNothing().when(vocabularyService).deprecate(anyInt(), anyString(), anyInt(), anyBoolean());

    mockMvc
        .perform(
            put(BASE_PATH + "/" + vocabulary.getName() + "/deprecate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(new DeprecateVocabularyAction())))
        .andExpect(status().isNoContent());
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void deprecateWrongNameTest() throws Exception {
    mockMvc
        .perform(
            put(BASE_PATH + "/fake/deprecate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(new DeprecateVocabularyAction())))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void deprecateUnauthorizedTest() throws Exception {
    mockMvc
        .perform(
            put(BASE_PATH + "/fake/deprecate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(new DeprecateVocabularyAction())))
        .andExpect(status().isUnauthorized());
  }

  @WithMockUser(authorities = {"USER"})
  @Test
  public void deprecateForbiddenTest() throws Exception {
    mockMvc
        .perform(
            put(BASE_PATH + "/fake/deprecate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(new DeprecateVocabularyAction())))
        .andExpect(status().isForbidden());
  }

  private Vocabulary createVocabulary() {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(UUID.randomUUID().toString());
    vocabulary.setNamespace(NAMESPACE_TEST);
    vocabulary.setLabel(Collections.singletonMap(Language.ENGLISH, "Label"));
    vocabulary.setEditorialNotes(Arrays.asList("note1", "note2"));

    return vocabulary;
  }
}
