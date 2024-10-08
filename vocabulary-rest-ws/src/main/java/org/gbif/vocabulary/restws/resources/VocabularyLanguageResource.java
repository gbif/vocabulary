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

import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.LanguageRegion.LanguageRegionAllFieldsSerializer;
import org.gbif.vocabulary.model.utils.PathUtils;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
    name = "Vocabulary Languages",
    description = "Languages that can be used in the vocabularies and concepts.",
    extensions =
        @io.swagger.v3.oas.annotations.extensions.Extension(
            name = "Order",
            properties = @ExtensionProperty(name = "Order", value = "0400")))
@RestController
@RequestMapping(PathUtils.VOCABULARY_LANGUAGE_PATH)
public class VocabularyLanguageResource {

  private static final SimpleModule LANGUAGE_SERIALIZER_MODULE =
      new SimpleModule().setMixInAnnotation(LanguageRegion.class, LanguageRegionMixin.class);
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(LANGUAGE_SERIALIZER_MODULE);

  @Operation(
      operationId = "listLanguages",
      summary = "List all languages",
      description = "Lists all current languages.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0100")))
  @ApiResponse(responseCode = "200", description = "Languages returned")
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public String listLanguageRegions() throws JsonProcessingException {
    return OBJECT_MAPPER.writeValueAsString(LanguageRegion.values());
  }

  @JsonSerialize(using = LanguageRegionAllFieldsSerializer.class)
  public abstract static class LanguageRegionMixin {}
}
