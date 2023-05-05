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
package org.gbif.vocabulary.restws.documentation;

import org.gbif.vocabulary.model.LanguageRegion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

public class Docs {

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameter(name = "name", description = "The name of the vocabulary.", in = ParameterIn.PATH)
  public @interface VocabularyNamePathParameter {}

  @Docs.VocabularyNameInConceptPathParameter
  @Docs.ConceptNamePathParameter
  public @interface ConceptPathParameters{}

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameter(
      name = "vocabularyName",
      description = "The name of the vocabulary of the concept.",
      in = ParameterIn.PATH)
  public @interface VocabularyNameInConceptPathParameter {}

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameter(name = "name", description = "The name of the concept.", in = ParameterIn.PATH)
  public @interface ConceptNamePathParameter {}

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameter(name = "key", description = "The key of the definition.", in = ParameterIn.PATH)
  public @interface DefinitionKeyPathParameter {}

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameter(name = "key", description = "The key of the label.", in = ParameterIn.PATH)
  public @interface LabelKeyPathParameter {}

  @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Parameter(
      name = "locale",
      description = "Locale to filter by",
      schema = @Schema(implementation = LanguageRegion.class),
      in = ParameterIn.QUERY)
  public @interface LocaleParameter {}

  @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Parameter(
      name = "lang",
      description = "Languages to filter by",
      schema = @Schema(implementation = LanguageRegion.class),
      in = ParameterIn.QUERY,
      explode = Explode.TRUE)
  public @interface LangParameter {}

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameter(name = "name", description = "The name of the tag.", in = ParameterIn.PATH)
  public @interface TagNamePathParameter {}

  /**
   * Documents responses to every read-only operation on subentities: comments, tags, machine tags,
   * etc.
   */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @ApiResponses({
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
    @ApiResponse(
        responseCode = "404",
        description = "Entity or subentity not found",
        content = @Content),
    @ApiResponse(
        responseCode = "5XX",
        description = "System failure – try again",
        content = @Content)
  })
  public @interface DefaultUnsuccessfulReadResponses {}

  /**
   * Documents responses to every write operation on subentities: comments, tags, machine tags, etc.
   */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @ApiResponses({
    @ApiResponse(
        responseCode = "400",
        description = "Bad request: the JSON is invalid or the request is not well-formed",
        content = @Content),
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
    @ApiResponse(
        responseCode = "422",
        description =
            "The request is syntactically correct but the fields are invalid (required fields not set, duplicated keys, inconsistent keys, etc.)",
        content = @Content)
  })
  public @interface DefaultUnsuccessfulWriteResponses {}

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Successful search"),
    @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  })
  public @interface DefaultSearchResponses {}
}
