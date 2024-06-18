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

import org.gbif.vocabulary.api.DeprecateAction;
import org.gbif.vocabulary.model.UserRoles;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.SuggestResult;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Base class for resources tests. */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.liquibase.enabled=false"})
@ActiveProfiles({"mock", "test"})
abstract class BaseResourceTest<T extends VocabularyEntity> {

  // util constants
  static final Long TEST_KEY = 1L;
  static final String NAMESPACE_TEST = "ns";

  static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired MockMvc mockMvc;

  @Test
  public void getNotFoundEntityTest() throws Exception {
    mockMvc.perform(get(getBasePath() + "/foo")).andExpect(status().isNotFound());
  }

  @WithMockUser(authorities = {UserRoles.VOCABULARY_ADMIN})
  @Test
  public void createNullEntityTest() throws Exception {
    mockMvc
        .perform(post(getBasePath()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @WithMockUser(authorities = {UserRoles.VOCABULARY_ADMIN})
  @Test
  public void updateNullEntityTest() throws Exception {
    mockMvc
        .perform(put(getBasePath() + "/fake").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @WithMockUser(authorities = {UserRoles.VOCABULARY_ADMIN})
  @Test
  public void deprecateEntityNotFoundTest() throws Exception {
    // mock not set, so the service returns null
    mockMvc
        .perform(
            put(getBasePath() + "/fake/deprecate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(createDeprecateAction())))
        .andExpect(status().isNotFound());
  }

  @WithMockUser(authorities = {UserRoles.VOCABULARY_ADMIN})
  @Test
  public void restoreDeprecatedEntityNotFoundNameTest() throws Exception {
    // mock not set, so the service returns null
    mockMvc.perform(delete(getBasePath() + "/fake/deprecate")).andExpect(status().isNotFound());
  }

  @Test
  public void corsHeadersTest() throws Exception {
    mockMvc
        .perform(
            options(getBasePath())
                .header("origin", "localhost")
                .header("access-control-request-headers", "authorization,content-type")
                .header("access-control-request-method", "GET"))
        .andExpect(header().string("Access-Control-Allow-Origin", "*"))
        .andExpect(
            header().string("Access-Control-Allow-Methods", "HEAD,GET,POST,DELETE,PUT,OPTIONS"))
        .andExpect(header().string("Access-Control-Allow-Headers", "authorization, content-type"));
  }

  Vocabulary createVocabulary(String name) {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(name);
    vocabulary.setNamespace(NAMESPACE_TEST);
    vocabulary.setEditorialNotes(Arrays.asList("note1", "note2"));

    return vocabulary;
  }

  void suggestTest(List<SuggestResult> suggestions) throws Exception {
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

  List<SuggestResult> createSuggestions() {
    SuggestResult result1 = new SuggestResult();
    result1.setName("n1");
    SuggestResult result2 = new SuggestResult();
    result2.setName("n2");
    return ImmutableList.of(result1, result2);
  }

  abstract String getBasePath();

  abstract T createEntity();

  abstract DeprecateAction createDeprecateAction();
}
