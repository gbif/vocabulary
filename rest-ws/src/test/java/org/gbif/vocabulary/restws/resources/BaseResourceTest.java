package org.gbif.vocabulary.restws.resources;

import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.restws.model.DeprecateAction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Base class for resources tests. */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
abstract class BaseResourceTest<T extends VocabularyEntity> {

  // util constants
  static final int TEST_KEY = 1;
  static final String NAMESPACE_TEST = "ns";

  static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired MockMvc mockMvc;

  @MockBean private DataSource dataSource;
  @MockBean private PlatformTransactionManager platformTransactionManager;

  @Test
  public void getNotFoundEntityTest() throws Exception {
    mockMvc.perform(get(getBasePath() + "/foo")).andExpect(status().isNotFound());
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void createNullEntityTest() throws Exception {
    mockMvc
        .perform(post(getBasePath()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void unauthorizedCreateTest() throws Exception {
    mockMvc
        .perform(
            post(getBasePath())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createEntity())))
        .andExpect(status().isUnauthorized());
  }

  @WithMockUser
  @Test
  public void forbiddenCreateTest() throws Exception {
    mockMvc
        .perform(
            post(getBasePath())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createEntity())))
        .andExpect(status().isForbidden());
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void updateNullEntityTest() throws Exception {
    mockMvc
        .perform(put(getBasePath() + "/fake").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void unauthorizedUpdateTest() throws Exception {
    mockMvc
        .perform(
            put(getBasePath() + "/fake")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createEntity())))
        .andExpect(status().isUnauthorized());
  }

  @WithMockUser()
  @Test
  public void forbiddenUpdateTest() throws Exception {
    mockMvc
        .perform(
            put(getBasePath() + "/fake")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createEntity())))
        .andExpect(status().isForbidden());
  }

  @Test
  public void suggestWithoutQueryTest() throws Exception {
    mockMvc.perform(get(getBasePath() + "/suggest")).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void deprecateUnauthorizedTest() throws Exception {
    mockMvc.perform(put(getBasePath() + "/fake/deprecate")).andExpect(status().isUnauthorized());
  }

  @WithMockUser(authorities = {"USER"})
  @Test
  public void deprecateForbiddenTest() throws Exception {
    mockMvc.perform(put(getBasePath() + "/fake/deprecate")).andExpect(status().isForbidden());
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void deprecateEntityNotFoundTest() throws Exception {
    mockMvc
        .perform(
            put(getBasePath() + "/fake/deprecate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createDeprecateAction())))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void restoreDeprecatedUnauthorizedTest() throws Exception {
    mockMvc.perform(delete(getBasePath() + "/name/deprecate")).andExpect(status().isUnauthorized());
  }

  @WithMockUser(authorities = {"USER"})
  @Test
  public void restoreDeprecatedForbiddenTest() throws Exception {
    mockMvc.perform(delete(getBasePath() + "/name/deprecate")).andExpect(status().isForbidden());
  }

  @WithMockUser(authorities = {"VOCABULARY_ADMIN"})
  @Test
  public void restoreDeprecatedEntityNotFoundNameTest() throws Exception {
    mockMvc.perform(delete(getBasePath() + "/fake/deprecate")).andExpect(status().isBadRequest());
  }

  Vocabulary createVocabulary(String name) {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(name);
    vocabulary.setNamespace(NAMESPACE_TEST);
    vocabulary.setLabel(Collections.singletonMap(Language.ENGLISH, "Label"));
    vocabulary.setEditorialNotes(Arrays.asList("note1", "note2"));

    return vocabulary;
  }

  void suggestTest(List<KeyNameResult> suggestions) throws Exception {
    MvcResult mvcResult =
        mockMvc
            .perform(get(getBasePath() + "/suggest?q=foo"))
            .andExpect(status().isOk())
            .andReturn();

    List<KeyNameResult> resultList =
        OBJECT_MAPPER.readValue(
            mvcResult.getResponse().getContentAsString(),
            new TypeReference<List<KeyNameResult>>() {});
    assertEquals(suggestions.size(), resultList.size());
  }

  List<KeyNameResult> createSuggestions() {
    KeyNameResult keyNameResult1 = new KeyNameResult();
    keyNameResult1.setKey(1);
    keyNameResult1.setName("n1");
    KeyNameResult keyNameResult2 = new KeyNameResult();
    keyNameResult2.setKey(2);
    keyNameResult2.setName("n2");
    return ImmutableList.of(keyNameResult1, keyNameResult2);
  }

  abstract String getBasePath();

  abstract T createEntity();

  abstract DeprecateAction createDeprecateAction();
}
