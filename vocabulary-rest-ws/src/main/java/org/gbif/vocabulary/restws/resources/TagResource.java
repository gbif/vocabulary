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
package org.gbif.vocabulary.restws.resources;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.restws.documentation.Docs;
import org.gbif.vocabulary.service.TagService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.vocabulary.restws.utils.Constants.TAGS_PATH;

@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Tags",
    description = "Tags allow grouping concepts semantically.")
@RestController
@RequestMapping(value = TAGS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class TagResource {

  private final TagService tagService;

  TagResource(TagService tagService) {
    this.tagService = tagService;
  }

  @Operation(
      operationId = "listTags",
      summary = "List all tags",
      description = "Lists all current tags.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0100")))
  @Pageable.OffsetLimitParameters
  @Docs.DefaultSearchResponses
  @GetMapping
  public PagingResponse<Tag> listTags(Pageable page) {
    return tagService.list(page);
  }

  @Operation(
      operationId = "getTag",
      summary = "Get details of a single tag",
      description = "Details of a single tag.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0110")))
  @Docs.TagNamePathParameter
  @ApiResponse(responseCode = "200", description = "Tag found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{name}")
  public Tag getTag(@PathVariable("name") String tagName) {
    return tagService.getByName(tagName);
  }

  @Operation(
      operationId = "createTag",
      summary = "Create a new tag",
      description = "Creates a new tag.\n\n The default color is white.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0201")))
  @ApiResponse(responseCode = "201", description = "Tag created and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping
  public Tag create(@RequestBody Tag tag) {
    int key = tagService.create(tag);
    return tagService.get(key);
  }

  @Operation(
      operationId = "updateTag",
      summary = "Update an existing tag",
      description = "Updates the existing tag.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0202")))
  @Docs.TagNamePathParameter
  @ApiResponse(responseCode = "200", description = "Tag updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping("{name}")
  public Tag update(@PathVariable("name") String tagName, @RequestBody Tag tag) {
    Tag oldTag = tagService.getByName(tagName);
    checkArgument(oldTag != null, "Tag not found for name " + tagName);
    checkArgument(
        oldTag.getKey().equals(tag.getKey()), "Key of the tag different than the persisted one");

    tagService.update(tag);
    return tagService.get(tag.getKey());
  }

  @Operation(
      operationId = "deleteTag",
      summary = "Delete an existing tag",
      description = "Deletes the existing tag.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0203")))
  @Docs.TagNamePathParameter
  @ApiResponse(responseCode = "204", description = "Tag deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{name}")
  public void delete(@PathVariable("name") String tagName) {
    Tag tag = tagService.getByName(tagName);
    checkArgument(tag != null, "Tag not found for name " + tagName);
    tagService.delete(tag.getKey());
  }
}
