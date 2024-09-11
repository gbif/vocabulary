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

import org.gbif.api.documentation.CommonParameters;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.AddTagAction;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.api.DeprecateConceptAction;
import org.gbif.vocabulary.model.*;
import org.gbif.vocabulary.model.exception.EntityNotFoundException;
import org.gbif.vocabulary.model.exception.EntityNotFoundException.EntityType;
import org.gbif.vocabulary.model.search.ChildrenResult;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.LookupResult;
import org.gbif.vocabulary.model.search.SuggestResult;
import org.gbif.vocabulary.restws.config.WsConfig;
import org.gbif.vocabulary.restws.documentation.Docs;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.TagService;
import org.gbif.vocabulary.service.VocabularyService;

import java.util.*;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.util.Strings;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.vocabulary.model.utils.PathUtils.*;
import static org.gbif.vocabulary.restws.resources.LatestReleaseCache.conceptSuggestLatestReleaseCache;

@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Concepts",
    description =
        "A concept is an entity that belongs to a vocabulary.\n\n"
            + "Concepts can also be deprecated and they can be nested. This API also allows to query the concepts from"
            + "the latest release of a vocabulary.",
    extensions =
        @io.swagger.v3.oas.annotations.extensions.Extension(
            name = "Order",
            properties = @ExtensionProperty(name = "Order", value = "0200")))
@RestController
@RequestMapping(
    value = VOCABULARIES_PATH + "/{vocabularyName}/" + CONCEPTS_PATH,
    produces = MediaType.APPLICATION_JSON_VALUE)
public class ConceptResource {

  private final ConceptService conceptService;
  private final VocabularyService vocabularyService;
  private final TagService tagService;
  private final WsConfig wsConfig;

  ConceptResource(
      ConceptService conceptService,
      VocabularyService vocabularyService,
      TagService tagService,
      WsConfig wsConfig) {
    this.conceptService = conceptService;
    this.vocabularyService = vocabularyService;
    this.tagService = tagService;
    this.wsConfig = wsConfig;
  }

  @Parameters(
      value = {
        @Parameter(
            name = "parentKey",
            description = "The key of the parent concept.",
            schema = @Schema(implementation = Long.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "parent",
            description = "The name of the parent concept.",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "replacedByKey",
            description = "The key of the replacement of the concept.",
            schema = @Schema(implementation = Long.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "name",
            description = "The name of the concept",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "deprecated",
            description = "Is the concept deprecated?",
            schema = @Schema(implementation = Boolean.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "key",
            description = "The key of the concept.",
            schema = @Schema(implementation = Long.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "hasParent",
            description = "Does the concept have parent?",
            schema = @Schema(implementation = Boolean.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "hasReplacement",
            description = "Does the concept have replacement?",
            schema = @Schema(implementation = Boolean.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "includeChildrenCount",
            description =
                "Should the search results include the count of the children of the concept?",
            schema = @Schema(implementation = Boolean.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "includeChildren",
            description = "Should the search results include the children of the concept?",
            schema = @Schema(implementation = Boolean.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "includeParents",
            description = "Should the search results include the parents of the concept?",
            schema = @Schema(implementation = Boolean.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "hiddenLabel",
            description = "The hidden label to filter by",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE)
      })
  @CommonParameters.QParameter
  @Pageable.OffsetLimitParameters
  @Docs.DefaultSearchResponses
  @Docs.VocabularyNameInConceptPathParameter
  public @interface ListCommonDocs {}

  @Operation(
      operationId = "listConcepts",
      summary = "List all concepts of the vocabulary",
      description = "Lists all concepts of the vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0100")))
  @Parameters(
      @Parameter(
          name = "tags",
          description = "Tags of the concept",
          schema = @Schema(implementation = String.class),
          in = ParameterIn.QUERY,
          explode = Explode.TRUE))
  @Parameter(name = "params", hidden = true)
  @ListCommonDocs
  @GetMapping
  public PagingResponse<ConceptView> listConcepts(
      @PathVariable("vocabularyName") String vocabularyName, ConceptListParams params) {
    Vocabulary vocabulary = getVocabularyWithCheck(vocabularyName);

    PagingResponse<Concept> conceptsPage =
        conceptService.list(
            ConceptSearchParams.builder()
                .vocabularyKey(vocabulary.getKey())
                .query(params.getQ())
                .name(params.getName())
                .parentKey(params.getParentKey())
                .parent(params.getParent())
                .replacedByKey(params.getReplacedByKey())
                .deprecated(params.getDeprecated())
                .key(params.getKey())
                .hasParent(params.getHasParent())
                .hasReplacement(params.getHasReplacement())
                .tags(params.getTags())
                .hiddenLabel(params.getHiddenLabel())
                .build(),
            params.getPage());

    Stream<ConceptView> viewStream =
        createConceptViewStream(
            conceptsPage,
            params,
            conceptService::countChildren,
            conceptService::findParents,
            vocabularyName);

    // labels links
    viewStream =
        viewStream.map(v -> createLabelsLinks(v, vocabularyName, v.getConcept().getName()));

    return new PagingResponse<>(
        conceptsPage.getOffset(),
        conceptsPage.getLimit(),
        conceptsPage.getCount(),
        viewStream.collect(Collectors.toList()));
  }

  @Parameters(
      value = {
        @Parameter(
            name = "includeChildren",
            description = "Should the search results include the children of the concept?",
            schema = @Schema(implementation = Boolean.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "includeParents",
            description = "Should the search results include the parents of the concept?",
            schema = @Schema(implementation = Boolean.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE)
      })
  @Docs.ConceptPathParameters
  @ApiResponse(responseCode = "200", description = "Concept found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  public @interface GetCommonDocs {}

  @Operation(
      operationId = "getConcept",
      summary = "Get details of a single concept",
      description = "Details of a single concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0110")))
  @GetCommonDocs
  @GetMapping("{name}")
  public ConceptView get(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestParam(value = "includeParents", required = false) boolean includeParents,
      @RequestParam(value = "includeChildren", required = false) boolean includeChildren) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    ConceptView conceptView =
        createConceptView(includeParents, includeChildren, concept, vocabularyName);

    return createLabelsLinks(conceptView, vocabularyName, conceptName);
  }

  @Operation(
      operationId = "createConcept",
      summary = "Create a new concept",
      description =
          "Creates a new concept. Note definitions, labels, alternative labels, hidden labels and tags must be added "
              + "in subsequent requests.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0201")))
  @ApiResponse(responseCode = "201", description = "Concept created and returned")
  @Docs.VocabularyNameInConceptPathParameter
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping
  public ConceptView create(
      @PathVariable("vocabularyName") String vocabularyName, @RequestBody Concept concept) {
    Vocabulary vocabulary = getVocabularyWithCheck(vocabularyName);
    concept.setVocabularyKey(vocabulary.getKey());

    long key = conceptService.create(concept);
    return createLabelsLinks(
        new ConceptView(conceptService.get(key)), vocabularyName, concept.getName());
  }

  @Operation(
      operationId = "updateConcept",
      summary = "Update an existing concept",
      description =
          "Updates the existing concept. Note definitions, labels, alternative labels, hidden labels and tags are not changed with this method.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0202")))
  @Docs.ConceptPathParameters
  @ApiResponse(responseCode = "200", description = "Concept updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping("{name}")
  public ConceptView update(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Concept concept) {
    Vocabulary vocabulary = getVocabularyWithCheck(vocabularyName);
    checkArgument(
        vocabulary.getKey().equals(concept.getVocabularyKey()),
        "Concept vocabulary doesn't match with the resource vocabulary in the URL");
    checkArgument(
        conceptName.equals(concept.getName()),
        "Concept name doesn't match with the resource name in the URL");

    conceptService.update(concept);
    return createLabelsLinks(
        new ConceptView(conceptService.get(concept.getKey())), vocabularyName, concept.getName());
  }

  @Operation(
      operationId = "suggestConcepts",
      summary = "Suggest concepts.",
      description =
          "Search that returns up to 20 matching concepts. Results are ordered by relevance. "
              + "The response is smaller than a concept search.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0203")))
  @Docs.SuggestParameters
  @Docs.DefaultSearchResponses
  @Docs.VocabularyNameInConceptPathParameter
  @GetMapping("suggest")
  public List<SuggestResult> suggest(
      @PathVariable("vocabularyName") String vocabularyName,
      @RequestParam(value = "q", required = false) String query,
      LanguageRegion locale,
      LanguageRegion fallbackLocale,
      @RequestParam(value = "limit", required = false) Integer limit) {
    if (fallbackLocale != null && locale == null) {
      throw new IllegalArgumentException("Locale is required if the fallback locale is set");
    }
    return conceptService.suggest(
        query, getVocabularyWithCheck(vocabularyName).getKey(), locale, fallbackLocale, limit);
  }

  @Operation(
      operationId = "deprecateConcept",
      summary = "Deprecate an existing concept",
      description =
          "Deprecates the existing concept.\n\n Optionally, a replacement concept can be specified and the replacement "
              + "must belong to the same vocabulary. Note that if the concept has children they have to be either "
              + "deprecated or reassigned to the replacement if it's specified. If they should be deprecated it has to be "
              + "specified explicitly in the parameters.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0205")))
  @Docs.ConceptPathParameters
  @ApiResponse(responseCode = "204", description = "Concept deprecated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping("{name}/deprecate")
  public void deprecate(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody DeprecateConceptAction deprecateConceptAction) {
    conceptService.deprecate(
        getConceptWithCheck(conceptName, vocabularyName).getKey(),
        deprecateConceptAction.getDeprecatedBy(),
        deprecateConceptAction.getReplacementKey(),
        deprecateConceptAction.isDeprecateChildren());
  }

  @Operation(
      operationId = "restoreDeprecatedConcept",
      summary = "Restores a deprecated concept",
      description =
          "Restores the deprecated concept.\n\n Its vocabulary cannot be deprecated. If the concept used to have a parent "
              + "and now it's deprecated it will be replaced with its replacement if exists. Optionally, it can also be "
              + "specified to restore all the deprecated children of the concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0206")))
  @Docs.ConceptPathParameters
  @ApiResponse(responseCode = "204", description = "Concept restored")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{name}/deprecate")
  public void restoreDeprecated(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestParam(value = "restoreDeprecatedChildren", required = false)
          boolean restoreDeprecatedChildren) {
    conceptService.restoreDeprecated(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), restoreDeprecatedChildren);
  }

  @Operation(
      operationId = "listConceptDefinitions",
      summary = "List all the definitions of the concept",
      description = "Lists all definitions of the concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0300")))
  @Docs.LangParameter
  @Docs.ConceptPathParameters
  @Docs.DefaultSearchResponses
  @GetMapping("{name}/definition")
  public List<Definition> listDefinitions(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      List<LanguageRegion> lang) {
    return conceptService.listDefinitions(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), lang);
  }

  @Operation(
      operationId = "getConceptDefinition",
      summary = "Get the definition of a concept",
      description = "Gets the definition of the concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0310")))
  @Docs.ConceptPathParameters
  @Docs.DefinitionKeyPathParameter
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{name}/definition/{key}")
  public Definition getDefinition(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long definitionKey) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    return conceptService.getDefinition(concept.getKey(), definitionKey);
  }

  @Operation(
      operationId = "addConceptDefinition",
      summary = "Adds a definition to a concept",
      description = "Creates a definition and adds it to an existing concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0320")))
  @ApiResponse(responseCode = "201", description = "Concept created and returned")
  @Docs.ConceptPathParameters
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping("{name}/definition")
  public Definition addDefinition(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Definition definition) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    long key = conceptService.addDefinition(concept.getKey(), definition);
    return conceptService.getDefinition(concept.getKey(), key);
  }

  @Operation(
      operationId = "updateConceptDefinition",
      summary = "Updates a definition",
      description = "Updates a definition that belongs to an existing concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "030")))
  @ApiResponse(responseCode = "200", description = "Definition updated and returned")
  @Docs.ConceptPathParameters
  @Docs.DefinitionKeyPathParameter
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping("{name}/definition/{key}")
  public Definition updateDefinition(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long definitionKey,
      @RequestBody Definition definition) {
    checkArgument(
        definition.getKey() != null && definitionKey == definition.getKey(),
        "Definition key doesn't match with the path");
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    conceptService.updateDefinition(concept.getKey(), definition);
    return conceptService.getDefinition(concept.getKey(), definitionKey);
  }

  @Operation(
      operationId = "deleteConceptDefinition",
      summary = "Deletes a definition from a concept",
      description = "Deletes a definition from an existing concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0340")))
  @ApiResponse(responseCode = "204", description = "Definition deleted")
  @Docs.ConceptPathParameters
  @Docs.DefinitionKeyPathParameter
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{name}/definition/{key}")
  public void deleteDefinition(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long key) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    conceptService.deleteDefinition(concept.getKey(), key);
  }

  @Operation(
      operationId = "listConceptTags",
      summary = "List all the tags of the concept",
      description = "Lists all tags of the concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0400")))
  @Docs.ConceptPathParameters
  @Docs.DefaultSearchResponses
  @GetMapping("{name}/tags")
  public List<Tag> listTags(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName) {
    return conceptService.listTags(getConceptWithCheck(conceptName, vocabularyName).getKey());
  }

  @Operation(
      operationId = "addConceptTag",
      summary = "Links a tag to a concept",
      description = "Links a tag to an existing concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0420")))
  @ApiResponse(responseCode = "204", description = "Tag linked to the concept")
  @Docs.ConceptPathParameters
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping("{name}/tags")
  public void addTag(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody AddTagAction addTagAction) {
    checkArgument(!Strings.isNullOrEmpty(addTagAction.getTagName()), "Tag name is required");
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    Tag tag = tagService.getByName(addTagAction.getTagName());
    checkArgument(tag != null, "Tag not found for name " + addTagAction.getTagName());
    conceptService.addTag(concept.getKey(), tag.getKey());
  }

  @Operation(
      operationId = "removeConceptTag",
      summary = "Unlinks a tag from a concept",
      description = "Unlinks a tag from an existing concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0440")))
  @Parameters(
      @Parameter(name = "tagName", description = "The name of the tag.", in = ParameterIn.PATH))
  @ApiResponse(responseCode = "204", description = "Tag unlinked from the concept")
  @Docs.ConceptPathParameters
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{name}/tags/{tagName}")
  public void removeTag(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("tagName") String tagName) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    Tag tag = tagService.getByName(tagName);
    checkArgument(tag != null, "Tag not found for name " + tagName);
    conceptService.removeTag(concept.getKey(), tag.getKey());
  }

  @Operation(
      operationId = "listConceptLabels",
      summary = "List all the labels of the concept",
      description = "Lists all labels of the concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0500")))
  @Docs.LangParameter
  @Docs.ConceptPathParameters
  @Docs.DefaultSearchResponses
  @GetMapping("{name}/label")
  public List<Label> listLabels(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      List<LanguageRegion> lang) {
    return conceptService.listLabels(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), lang);
  }

  @Operation(
      operationId = "listConceptAlternativeLabels",
      summary = "List all the alternative labels of the concept",
      description = "Lists all alternative labels of the concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0600")))
  @Docs.LangParameter
  @Docs.ConceptPathParameters
  @Pageable.OffsetLimitParameters
  @Docs.DefaultSearchResponses
  @GetMapping("{name}/alternativeLabels")
  public PagingResponse<Label> listAlternativeLabels(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      List<LanguageRegion> lang,
      Pageable page) {
    return conceptService.listAlternativeLabels(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), lang, page);
  }

  @Operation(
      operationId = "listConceptHiddenLabels",
      summary = "List all the hidden labels of the concept",
      description = "Lists all hidden labels of the concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0700")))
  @Pageable.OffsetLimitParameters
  @Docs.ConceptPathParameters
  @Docs.DefaultSearchResponses
  @GetMapping("{name}/hiddenLabels")
  public PagingResponse<HiddenLabel> listHiddenLabels(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      Pageable page) {
    return conceptService.listHiddenLabels(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), page);
  }

  @Operation(
      operationId = "addConceptLabel",
      summary = "Adds a label to a concept",
      description = "Creates a label and adds it to an existing concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0510")))
  @ApiResponse(responseCode = "201", description = "Label created and returned")
  @Docs.ConceptPathParameters
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping("{name}/label")
  public Long addLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Label label) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    return conceptService.addLabel(concept.getKey(), label);
  }

  @Operation(
      operationId = "addConceptAlternativeLabel",
      summary = "Adds an alternative label to a concept",
      description = "Creates an alternative label and adds it to an existing concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0610")))
  @ApiResponse(responseCode = "201", description = "Alternative label created and returned")
  @Docs.ConceptPathParameters
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping("{name}/alternativeLabels")
  public Long addAlternativeLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Label label) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    return conceptService.addAlternativeLabel(concept.getKey(), label);
  }

  @Operation(
      operationId = "addConceptHiddenLabel",
      summary = "Adds a hidden label to a concept",
      description = "Creates a hidden label and adds it to an existing concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0710")))
  @ApiResponse(responseCode = "201", description = "Hidden label created and returned")
  @Docs.ConceptPathParameters
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping("{name}/hiddenLabels")
  public Long addHiddenLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody HiddenLabel label) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    return conceptService.addHiddenLabel(concept.getKey(), label);
  }

  @Operation(
      operationId = "deleteConceptLabel",
      summary = "Deletes a label from a concept",
      description = "Deletes a label from an existing concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0520")))
  @ApiResponse(responseCode = "204", description = "Label deleted")
  @Docs.ConceptPathParameters
  @Docs.LabelKeyPathParameter
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{name}/label/{key}")
  public void deleteLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long key) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    conceptService.deleteLabel(concept.getKey(), key);
  }

  @Operation(
      operationId = "deleteConceptAlternativeLabel",
      summary = "Deletes an alternative label from a concept",
      description = "Deletes an alternative label from an existing concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0620")))
  @ApiResponse(responseCode = "204", description = "Label deleted")
  @Docs.ConceptPathParameters
  @Docs.LabelKeyPathParameter
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{name}/alternativeLabels/{key}")
  public void deleteAlternativeLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long key) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    conceptService.deleteAlternativeLabel(concept.getKey(), key);
  }

  @Operation(
      operationId = "deleteConceptHiddenLabel",
      summary = "Deletes a hidden label from a concept",
      description = "Deletes a hidden label from an existing concept.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0720")))
  @ApiResponse(responseCode = "204", description = "Label deleted")
  @Docs.ConceptPathParameters
  @Docs.LabelKeyPathParameter
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{name}/hiddenLabels/{key}")
  public void deleteHiddenLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long key) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    conceptService.deleteHiddenLabel(concept.getKey(), key);
  }

  private ConceptView createLabelsLinks(
      ConceptView conceptView, String vocabularyName, String conceptName) {
    if (conceptView == null) {
      return null;
    }

    conceptView.setVocabularyName(vocabularyName);
    conceptView.setAlternativeLabelsLink(
        String.format(
            "%s/vocabularies/%s/concepts/%s/alternativeLabels",
            wsConfig.getApiUrl(), vocabularyName, conceptName));
    conceptView.setHiddenLabelsLink(
        String.format(
            "%s/vocabularies/%s/concepts/%s/hiddenLabels",
            wsConfig.getApiUrl(), vocabularyName, conceptName));
    return conceptView;
  }

  @Operation(
      operationId = "listConceptsFromLatestRelease",
      summary = "List all concepts of the vocabulary from its latest release",
      description = "Lists all concepts from the latest release of the vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0800")))
  @ListCommonDocs
  @GetMapping(LATEST_RELEASE_PATH)
  public PagingResponse<ConceptView> listConceptsLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName, ConceptListParams params) {
    PagingResponse<Concept> conceptsPage =
        conceptService.listLatestRelease(
            ConceptSearchParams.builder()
                .query(params.getQ())
                .name(params.getName())
                .parentKey(params.getParentKey())
                .parent(params.getParent())
                .replacedByKey(params.getReplacedByKey())
                .deprecated(params.getDeprecated())
                .key(params.getKey())
                .hasParent(params.getHasParent())
                .hasReplacement(params.getHasReplacement())
                .hiddenLabel(params.getHiddenLabel())
                .build(),
            params.getPage(),
            vocabularyName);

    Stream<ConceptView> viewStream =
        createConceptViewStream(
            conceptsPage,
            params,
            v -> conceptService.countChildrenLatestRelease(v, vocabularyName),
            v -> conceptService.findParentsLatestRelease(v, vocabularyName),
            vocabularyName);

    // labels links
    viewStream =
        viewStream.map(
            v ->
                createLabelsLinks(
                    v, vocabularyName, LATEST_RELEASE_PATH + "/" + v.getConcept().getName()));

    return new PagingResponse<>(
        conceptsPage.getOffset(),
        conceptsPage.getLimit(),
        conceptsPage.getCount(),
        viewStream.collect(Collectors.toList()));
  }

  @Operation(
      operationId = "getConceptFromLatestRelease",
      summary = "Get details of a single concept from the latest release of the vocabulary",
      description = "Details of a single concept from the latest release of the vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0810")))
  @GetCommonDocs
  @GetMapping(LATEST_RELEASE_PATH + "/{name}")
  public ConceptView getFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestParam(value = "includeParents", required = false) boolean includeParents,
      @RequestParam(value = "includeChildren", required = false) boolean includeChildren) {
    Concept concept = conceptService.getByNameLatestRelease(conceptName, vocabularyName);
    if (concept == null) {
      return null;
    }

    ConceptView conceptView =
        createConceptView(includeParents, includeChildren, concept, vocabularyName);

    return createLabelsLinks(conceptView, vocabularyName, LATEST_RELEASE_PATH + "/" + conceptName);
  }

  @Operation(
      operationId = "suggestConceptsFromLatestRelease",
      summary = "Suggest concepts from the latest release of the vocabulary.",
      description =
          "Search that returns up to 20 matching concepts from the latest release of the vocabulary. Results are ordered by relevance. "
              + "The response is smaller than a concept search.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0803")))
  @Docs.SuggestParameters
  @Docs.DefaultSearchResponses
  @Docs.VocabularyNameInConceptPathParameter
  @GetMapping(LATEST_RELEASE_PATH + "/suggest")
  public List<SuggestResult> suggestLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @RequestParam(value = "q", required = false) String query,
      LanguageRegion locale,
      LanguageRegion fallbackLocale,
      @RequestParam(value = "limit", required = false) Integer limit) {

    Supplier<String> cacheKey =
        () ->
            new StringJoiner(";")
                .add(query != null ? query : "null")
                .add(locale != null ? locale.getLocale() : "null")
                .add(fallbackLocale != null ? fallbackLocale.getLocale() : "null")
                .add(String.valueOf(limit))
                .toString();

    return conceptSuggestLatestReleaseCache
        .computeIfAbsent(vocabularyName, k -> new HashMap<>())
        .computeIfAbsent(
            cacheKey.get(),
            k ->
                conceptService.suggestLatestRelease(
                    query,
                    getVocabularyWithCheck(vocabularyName).getKey(),
                    locale,
                    fallbackLocale,
                    vocabularyName,
                    limit));
  }

  @Operation(
      operationId = "listConceptDefinitionsFromLatestRelease",
      summary = "List all the definitions of the concept from the latest release of the vocabulary",
      description =
          "Lists all definitions of the concept from the latest release of the vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0810")))
  @Docs.LangParameter
  @Docs.ConceptPathParameters
  @Docs.DefaultSearchResponses
  @GetMapping(LATEST_RELEASE_PATH + "/{name}/definition")
  public List<Definition> listDefinitionsFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      List<LanguageRegion> lang) {
    return conceptService.listDefinitionsLatestRelease(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), lang, vocabularyName);
  }

  @Operation(
      operationId = "listConceptLabelsFromLatestRelease",
      summary = "List all the labels of the concept from the latest release of the vocabulary",
      description = "Lists all labels of the concept from the latest release of the vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0820")))
  @Docs.LangParameter
  @Docs.ConceptPathParameters
  @Docs.DefaultSearchResponses
  @GetMapping(LATEST_RELEASE_PATH + "/{name}/label")
  public List<Label> listLabelsFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      List<LanguageRegion> lang) {
    return conceptService.listLabelsLatestRelease(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), lang, vocabularyName);
  }

  @Operation(
      operationId = "listConceptAlternativeLabelsFromLatestRelease",
      summary =
          "List all the alternative labels of the concept from the latest release of the vocabulary",
      description =
          "Lists all alternative labels of the concept from the latest release of the vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0830")))
  @Docs.LangParameter
  @Docs.ConceptPathParameters
  @Pageable.OffsetLimitParameters
  @Docs.DefaultSearchResponses
  @GetMapping(LATEST_RELEASE_PATH + "/{name}/alternativeLabels")
  public PagingResponse<Label> listAlternativeLabelsFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      List<LanguageRegion> lang,
      Pageable page) {
    return conceptService.listAlternativeLabelsLatestRelease(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), lang, page, vocabularyName);
  }

  @Operation(
      operationId = "listConceptHiddenLabelsFromLatestRelease",
      summary =
          "List all the hidden labels of the concept from the latest release of the vocabulary",
      description =
          "Lists all hidden labels of the concept from the latest release of the vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0840")))
  @Pageable.OffsetLimitParameters
  @Docs.ConceptPathParameters
  @Docs.DefaultSearchResponses
  @GetMapping(LATEST_RELEASE_PATH + "/{name}/hiddenLabels")
  public PagingResponse<HiddenLabel> listHiddenLabelsFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      Pageable page) {
    return conceptService.listHiddenLabelsLatestRelease(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), page, vocabularyName);
  }

  @Operation(
      operationId = "lookup",
      summary = "Concept lookup",
      description = "Lookup a concept given a specific value and an optional language.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0900")))
  @Parameters(
      value = {
        @Parameter(
            name = "q",
            description = "Value to do the lookup against to",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "lang",
            description = "Lang to discriminate the lookup",
            schema = @Schema(implementation = LanguageRegion.class),
            in = ParameterIn.QUERY)
      })
  @Docs.DefaultSearchResponses
  @GetMapping("lookup")
  public List<LookupResult> lookup(
      @PathVariable("vocabularyName") String vocabularyName,
      @RequestParam("q") String q,
      LanguageRegion lang) {
    getVocabularyWithCheck(vocabularyName);
    return conceptService.lookup(q, vocabularyName, lang);
  }

  @Operation(
      operationId = "lookupLatestRelease",
      summary = "Concept lookup in the latest release",
      description =
          "Lookup a concept in the latest release of the vocabulary given a specific value and an optional language.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0900")))
  @Parameters(
      value = {
        @Parameter(
            name = "q",
            description = "Value to do the lookup against to",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "lang",
            description = "Lang to discriminate the lookup",
            schema = @Schema(implementation = LanguageRegion.class),
            in = ParameterIn.QUERY)
      })
  @Docs.DefaultSearchResponses
  @GetMapping(LATEST_RELEASE_PATH + "/lookup")
  public List<LookupResult> lookupInLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @RequestParam("q") String q,
      LanguageRegion lang) {
    getVocabularyWithCheck(vocabularyName);
    return conceptService.lookupLatestRelease(q, vocabularyName, lang);
  }

  private ConceptView createConceptView(
      boolean includeParents, boolean includeChildren, Concept concept, String vocabularyName) {
    if (concept == null) {
      return null;
    }

    ConceptView conceptView = new ConceptView(concept);
    conceptView.setVocabularyName(vocabularyName);

    if (includeParents && concept.getParentKey() != null) {
      conceptView.setParents(conceptService.findParents(concept.getKey()));
    }

    if (includeChildren) {
      // get the children
      List<ChildrenResult> children =
          conceptService.countChildren(Collections.singletonList(concept.getKey()));

      // set it to the view
      conceptView.setChildren(
          children.stream().map(ChildrenResult::getChildName).collect(Collectors.toList()));
    }
    return conceptView;
  }

  public Vocabulary getVocabularyWithCheck(String vocabularyName) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    if (vocabulary == null) {
      throw new EntityNotFoundException(
          EntityType.VOCABULARY, "Vocabulary not found: " + vocabularyName);
    }
    return vocabulary;
  }

  private Concept getConceptWithCheck(String conceptName, String vocabularyName) {
    Concept concept = conceptService.getByNameAndVocabulary(conceptName, vocabularyName);
    if (concept == null) {
      throw new EntityNotFoundException(
          EntityType.CONCEPT,
          "Concept " + conceptName + " not found in vocabulary " + vocabularyName);
    }
    return concept;
  }

  private Stream<ConceptView> createConceptViewStream(
      PagingResponse<Concept> conceptsPage,
      ConceptListParams params,
      Function<List<Long>, List<ChildrenResult>> childrenFn,
      LongFunction<List<String>> parentsFn,
      String vocabularyName) {
    Stream<ConceptView> viewStream =
        conceptsPage.getResults().stream()
            .map(ConceptView::new)
            .map(v -> v.setVocabularyName(vocabularyName));

    // this could be simplified by keeping only 1 param but we leave the 2 of them for backwards
    // compatibility
    if (params.isIncludeChildrenCount() || params.isIncludeChildren()) {
      // get the keys of all the concepts
      List<Long> parentKeys =
          conceptsPage.getResults().stream()
              .map(AbstractVocabularyEntity::getKey)
              .collect(Collectors.toList());

      // get the children
      List<ChildrenResult> children = new ArrayList<>();
      if (!parentKeys.isEmpty()) {
        children = childrenFn.apply(parentKeys);
      }

      Map<Long, Set<ChildrenResult>> childrenByConcept =
          children.stream()
              .collect(Collectors.groupingBy(ChildrenResult::getParentKey, Collectors.toSet()));

      // set it to the view
      if (params.isIncludeChildrenCount()) {
        viewStream =
            viewStream.map(
                v ->
                    v.setChildrenCount(
                        childrenByConcept
                            .getOrDefault(v.getConcept().getKey(), Collections.emptySet())
                            .size()));
      }
      if (params.isIncludeChildren()) {
        viewStream =
            viewStream.map(
                v ->
                    v.setChildren(
                        childrenByConcept
                            .getOrDefault(v.getConcept().getKey(), Collections.emptySet())
                            .stream()
                            .map(ChildrenResult::getChildName)
                            .collect(Collectors.toList())));
      }
    }

    // parents
    if (params.isIncludeParents()) {
      viewStream =
          viewStream.map(
              v ->
                  v.getConcept().getParentKey() != null
                      ? v.setParents(parentsFn.apply(v.getConcept().getKey()))
                      : v);
    }

    return viewStream;
  }
}
