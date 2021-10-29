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
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.service.TagService;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;

import static org.gbif.vocabulary.restws.utils.Constants.TAGS_PATH;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/* Generates the documentation for the tags API. */
public class TagTestDoc extends DocumentationBaseTest {

  private static final int TEST_KEY = 1;

  @MockBean private TagService tagService;

  @Test
  public void createTagTest() throws Exception {
    Tag tag = createTag("tag1");

    setSecurityContext();
    when(tagService.create(any(Tag.class))).thenReturn(TEST_KEY);
    tag.setKey(TEST_KEY);
    when(tagService.get(TEST_KEY)).thenReturn(tag);

    mockMvc
        .perform(
            post(getBasePath())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(tag))
                .with(authorizationDocumentation()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("key", is(TEST_KEY)))
        .andExpect(jsonPath("name", equalTo(tag.getName())))
        .andDo(documentFields(Tag.class));
  }

  @Test
  public void listTagsTest() throws Exception {
    List<Tag> tags = ImmutableList.of(createTag("tag1"), createTag("tag2"), createTag("tag3"));

    when(tagService.list(any()))
        .thenReturn(new PagingResponse<>(new PagingRequest(), (long) tags.size(), tags));

    MvcResult mvcResult =
        mockMvc
            .perform(get(getBasePath()).param("offset", "0").param("limit", "20"))
            .andExpect(status().isOk())
            .andDo(
                document(
                    "{class-name}/{method-name}",
                    requestParameters(
                        parameterWithName("offset")
                            .description("Page offset. By default 0")
                            .optional(),
                        parameterWithName("limit")
                            .description("Page limit. By default 20")
                            .optional())))
            .andReturn();

    JsonNode rootNode = OBJECT_MAPPER.readTree(mvcResult.getResponse().getContentAsString());
    List<Tag> resultList =
        OBJECT_MAPPER.convertValue(rootNode.get("results"), new TypeReference<List<Tag>>() {});

    assertEquals(tags.size(), resultList.size());
  }

  @Test
  public void getTagTest() throws Exception {
    Tag tag = createTag("tag1");
    when(tagService.getByName(tag.getName())).thenReturn(tag);

    mockMvc
        .perform(get(getBasePath() + "/" + tag.getName()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("name", equalTo(tag.getName())));
  }

  @Test
  public void updateTagTest() throws Exception {
    setSecurityContext();
    doNothing().when(tagService).update(any(Tag.class));
    Tag tag = createTag("tag1");
    tag.setKey(TEST_KEY);
    when(tagService.getByName(tag.getName())).thenReturn(tag);
    tag.setKey(TEST_KEY);
    when(tagService.get(tag.getKey())).thenReturn(tag);

    mockMvc
        .perform(
            put(getBasePath() + "/" + tag.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(tag))
                .with(authorizationDocumentation()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("key", is(tag.getKey())))
        .andExpect(jsonPath("name", equalTo(tag.getName())))
        .andDo(documentFields(Tag.class));
  }

  @Test
  public void deleteTagTest() throws Exception {
    setSecurityContext();
    doNothing().when(tagService).delete(TEST_KEY);
    Tag tag = createTag("tag1");
    tag.setKey(TEST_KEY);
    when(tagService.getByName(tag.getName())).thenReturn(tag);

    mockMvc
        .perform(
            delete(getBasePath() + "/" + tag.getName())
                .contentType(MediaType.APPLICATION_JSON)
                .with(authorizationDocumentation()))
        .andExpect(status().isNoContent());
  }

  @Override
  String getBasePath() {
    return "/" + TAGS_PATH;
  }
}
