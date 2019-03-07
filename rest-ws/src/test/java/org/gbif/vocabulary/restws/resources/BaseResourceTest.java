package org.gbif.vocabulary.restws.resources;

import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.transaction.PlatformTransactionManager;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

  @Test
  public void restoreDeprecatedUnauthorizedTest() throws Exception {
    mockMvc.perform(delete(getBasePath() + "/name/deprecate")).andExpect(status().isUnauthorized());
  }

  @WithMockUser(authorities = {"USER"})
  @Test
  public void restoreDeprecatedForbiddenTest() throws Exception {
    mockMvc.perform(delete(getBasePath() + "/name/deprecate")).andExpect(status().isForbidden());
  }

  abstract String getBasePath();

  abstract T createEntity();

  Vocabulary createVocabulary() {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(UUID.randomUUID().toString());
    vocabulary.setNamespace(NAMESPACE_TEST);
    vocabulary.setLabel(Collections.singletonMap(Language.ENGLISH, "Label"));
    vocabulary.setEditorialNotes(Arrays.asList("note1", "note2"));

    return vocabulary;
  }

  Concept createConcept() {
    Concept concept = new Concept();
    concept.setName(UUID.randomUUID().toString());
    concept.setLabel(Collections.singletonMap(Language.ENGLISH, "Label"));
    concept.setAlternativeLabels(
      Collections.singletonMap(Language.ENGLISH, Arrays.asList("Label2", "Label3")));
    concept.setEditorialNotes(Arrays.asList("note1", "note2"));

    return concept;
  }
}
