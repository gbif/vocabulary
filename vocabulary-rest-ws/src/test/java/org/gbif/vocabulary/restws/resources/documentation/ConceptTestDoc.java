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
package org.gbif.vocabulary.restws.resources.documentation;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.AddTagAction;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.api.DeprecateConceptAction;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.TagService;
import org.gbif.vocabulary.service.VocabularyService;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;

import static org.gbif.vocabulary.restws.utils.Constants.CONCEPTS_PATH;
import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Generates the documentation for the concept API. */
public class ConceptTestDoc extends DocumentationBaseTest {

  @MockBean private ConceptService conceptService;
  @MockBean private VocabularyService vocabularyService;
  @MockBean private TagService tagService;

  @Test
  public void listConceptsTest() throws Exception {
    List<Concept> concepts =
        ImmutableList.of(
            createConcept("concept1"), createConcept("concept2"), createConcept("concept3"));

    when(vocabularyService.getByName(anyString()))
        .thenReturn(createVocabulary(TEST_VOCABULARY_NAME));
    when(conceptService.list(any(ConceptSearchParams.class), any(PagingRequest.class)))
        .thenReturn(new PagingResponse<>(new PagingRequest(), (long) concepts.size(), concepts));

    MvcResult mvcResult =
        mockMvc
            .perform(
                get(getBasePath())
                    .param("q", "test")
                    .param("parentKey", "2")
                    .param("parent", "concept2")
                    .param("replacedByKey", "2")
                    .param("name", "concept1")
                    .param("deprecated", "false")
                    .param("key", "1")
                    .param("hasParent", "true")
                    .param("hasReplacement", "false")
                    .param("includeChildrenCount", "true")
                    .param("includeChildren", "true")
                    .param("includeParents", "true")
                    .param("tags", "tag1,tag2")
                    .param("offset", "0")
                    .param("limit", "20"))
            .andExpect(status().isOk())
            .andDo(
                document(
                    "{class-name}/{method-name}",
                    requestParameters(
                        parameterWithName("q").description("Search query").optional(),
                        parameterWithName("parentKey").description("Parent key").optional(),
                        parameterWithName("parent").description("Parent name").optional(),
                        parameterWithName("replacedByKey")
                            .description("Replacement key")
                            .optional(),
                        parameterWithName("name").description("Vocabulary name").optional(),
                        parameterWithName("deprecated")
                            .description(
                                "Boolean to search for deprecated or non-deprecated vocabularies")
                            .optional(),
                        parameterWithName("key").description("Vocabulary key").optional(),
                        parameterWithName("hasParent")
                            .description("Boolean to search for concepts with parent")
                            .optional(),
                        parameterWithName("hasReplacement")
                            .description("Boolean to search for concepts with replacement")
                            .optional(),
                        parameterWithName("includeChildrenCount")
                            .description("Boolean to include the children count in the response")
                            .optional(),
                        parameterWithName("includeChildren")
                            .description("Boolean to include the children names in the response")
                            .optional(),
                        parameterWithName("includeParents")
                            .description("Boolean to include the parents names in the response")
                            .optional(),
                        parameterWithName("tags")
                            .description(
                                "Concept tags. For multiple values you can separate them by commas or send this parameter multiple times")
                            .optional(),
                        parameterWithName("offset")
                            .description("Page offset. By default 0")
                            .optional(),
                        parameterWithName("limit")
                            .description("Page limit. By default 20")
                            .optional())))
            .andReturn();

    JsonNode rootNode = OBJECT_MAPPER.readTree(mvcResult.getResponse().getContentAsString());
    List<ConceptView> resultList =
        OBJECT_MAPPER.convertValue(
            rootNode.get("results"), new TypeReference<List<ConceptView>>() {});

    assertEquals(concepts.size(), resultList.size());
  }

  @Test
  public void getConceptTest() throws Exception {
    Concept concept = createConcept("concept1");
    when(conceptService.getByNameAndVocabulary(anyString(), anyString())).thenReturn(concept);

    mockMvc
        .perform(get(getBasePath() + "/" + concept.getName()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("name", equalTo(concept.getName())));
  }

  @Test
  public void createConceptTest() throws Exception {
    setSecurityContext();
    mockVocabulary();
    when(conceptService.create(any(Concept.class))).thenReturn(TEST_KEY);
    Concept conceptToCreate = createConcept("concept1");
    Concept created = new Concept();
    BeanUtils.copyProperties(conceptToCreate, created);
    created.setKey(TEST_KEY);
    when(conceptService.get(TEST_KEY)).thenReturn(created);

    mockMvc
        .perform(
            post(getBasePath())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(conceptToCreate))
                .with(authorizationDocumentation()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith(getBasePath() + "/" + created.getName())))
        .andExpect(jsonPath("key", is(TEST_KEY.intValue())))
        .andExpect(jsonPath("name", equalTo(created.getName())))
        .andDo(documentFields(Concept.class));
  }

  @Test
  public void updateConceptTest() throws Exception {
    setSecurityContext();
    mockVocabulary();
    doNothing().when(conceptService).update(any(Concept.class));
    Concept concept = createConcept("concept1");
    concept.setKey(TEST_KEY);
    when(conceptService.get(TEST_KEY)).thenReturn(concept);

    mockMvc
        .perform(
            put(getBasePath() + "/" + concept.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(concept))
                .with(authorizationDocumentation()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("key", is(concept.getKey().intValue())))
        .andExpect(jsonPath("name", equalTo(concept.getName())))
        .andDo(documentFields(Concept.class));
  }

  @Test
  public void suggestConceptTest() throws Exception {
    mockVocabulary();
    List<KeyNameResult> suggestions = createSuggestions();
    when(conceptService.suggest(anyString(), anyLong())).thenReturn(suggestions);
    suggestTest(suggestions);
  }

  @Test
  public void deprecateConceptTest() throws Exception {
    setSecurityContext();
    Concept concept = createConcept("concept1");
    concept.setKey(TEST_KEY);
    when(conceptService.getByNameAndVocabulary(concept.getName(), TEST_VOCABULARY_NAME))
        .thenReturn(concept);
    doNothing().when(conceptService).deprecate(anyLong(), anyString(), anyLong(), anyBoolean());

    mockMvc
        .perform(
            put(getBasePath() + "/" + concept.getName() + "/deprecate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(new DeprecateConceptAction()))
                .with(authorizationDocumentation()))
        .andExpect(status().isNoContent())
        .andDo(documentRequestFields(DeprecateConceptAction.class));
  }

  @Test
  public void restoreDeprecatedConceptTest() throws Exception {
    setSecurityContext();
    Concept concept = createConcept("concept1");
    concept.setKey(TEST_KEY);
    when(conceptService.getByNameAndVocabulary(concept.getName(), TEST_VOCABULARY_NAME))
        .thenReturn(concept);
    doNothing().when(conceptService).restoreDeprecated(anyLong(), anyBoolean());

    mockMvc
        .perform(
            delete(getBasePath() + "/" + concept.getName() + "/deprecate")
                .with(authorizationDocumentation()))
        .andExpect(status().isNoContent());
  }

  @Test
  public void listTagsTest() throws Exception {
    Concept concept = createConcept("concept1");
    concept.setKey(TEST_KEY);
    List<Tag> tags = ImmutableList.of(createTag("tag1"), createTag("tag2"));

    when(conceptService.getByNameAndVocabulary(anyString(), anyString())).thenReturn(concept);
    when(conceptService.listTags(anyLong())).thenReturn(tags);

    MvcResult mvcResult =
        mockMvc
            .perform(get(getBasePath() + "/" + concept.getName() + "/tags"))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode rootNode = OBJECT_MAPPER.readTree(mvcResult.getResponse().getContentAsString());
    List<Tag> resultList = OBJECT_MAPPER.convertValue(rootNode, new TypeReference<List<Tag>>() {});

    assertEquals(tags.size(), resultList.size());
  }

  @Test
  public void addTagTest() throws Exception {
    setSecurityContext();
    Concept concept = createConcept("concept1");
    concept.setKey(TEST_KEY);
    Tag tag = createTag("tag1");
    tag.setKey(TEST_KEY.intValue());

    when(conceptService.getByNameAndVocabulary(anyString(), anyString())).thenReturn(concept);
    when(tagService.getByName(anyString())).thenReturn(tag);
    doNothing().when(conceptService).addTag(anyLong(), anyInt());

    mockMvc
        .perform(
            put(getBasePath() + "/" + concept.getName() + "/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(new AddTagAction(tag.getName()))))
        .andExpect(status().isNoContent())
        .andDo(documentRequestFields(AddTagAction.class));
  }

  @Test
  public void removeTagTest() throws Exception {
    setSecurityContext();
    Concept concept = createConcept("concept1");
    concept.setKey(TEST_KEY);
    Tag tag = createTag("tag1");
    tag.setKey(TEST_KEY.intValue());

    when(conceptService.getByNameAndVocabulary(anyString(), anyString())).thenReturn(concept);
    when(tagService.getByName(anyString())).thenReturn(tag);
    doNothing().when(conceptService).removeTag(anyLong(), anyInt());

    mockMvc
        .perform(delete(getBasePath() + "/" + concept.getName() + "/tags/" + tag.getName()))
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
}
