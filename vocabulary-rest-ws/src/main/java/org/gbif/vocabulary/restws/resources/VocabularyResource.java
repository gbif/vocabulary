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
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.VocabularyReleasedMessage;
import org.gbif.vocabulary.api.DeprecateVocabularyAction;
import org.gbif.vocabulary.api.VocabularyListParams;
import org.gbif.vocabulary.api.VocabularyReleaseParams;
import org.gbif.vocabulary.model.*;
import org.gbif.vocabulary.model.exception.EntityNotFoundException;
import org.gbif.vocabulary.model.export.ExportParams;
import org.gbif.vocabulary.model.search.SuggestResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.restws.config.ExportConfig;
import org.gbif.vocabulary.restws.documentation.Docs;
import org.gbif.vocabulary.service.ExportService;
import org.gbif.vocabulary.service.VocabularyService;
import org.gbif.vocabulary.tools.VocabularyDownloader;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.vocabulary.model.utils.PathUtils.VOCABULARIES_PATH;
import static org.gbif.vocabulary.model.utils.PathUtils.VOCABULARY_RELEASES_PATH;

/** Controller for {@link org.gbif.vocabulary.model.Vocabulary} entities. */
@OpenAPIDefinition(
    info =
        @Info(
            title = "Vocabulary API",
            version = "v1",
            description =
                "This API works against the GBIF Vocabulary server, which acts as a registry of controlled vocabularies that are or will be used during the data interpretation."
                    + "Internally we use a Java web service client for the consumption of these HTTP-based, RESTful web services. "
                    + "It may be of interest to those coding against the API, and can be found in the "
                    + "[vocabulary-rest-ws-client](https://github.com/gbif/vocabulary/tree/master/vocabulary-rest-ws-client) project.",
            termsOfService = "https://www.gbif.org/terms"),
    servers = {
      @Server(url = "https://api.gbif.org/v1/", description = "Production"),
      @Server(url = "https://api.gbif-uat.org/v1/", description = "User testing")
    })
@Tag(
    name = "Vocabularies",
    description =
        "A vocabulary is the entity that holds data about a controlled vocabulary and its concepts.\n\n"
            + "The vocabulary API provides CRUD and discovery services for vocabularies. "
            + "It also allows to deprecate vocabularies that are being replaced by other vocabularies.",
    extensions =
        @io.swagger.v3.oas.annotations.extensions.Extension(
            name = "Order",
            properties = @ExtensionProperty(name = "Order", value = "0100")))
@Slf4j
@RestController
@RequestMapping(value = VOCABULARIES_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class VocabularyResource {

  private final VocabularyService vocabularyService;
  private final ExportService exportService;
  private final ExportConfig exportConfig;
  private final MessagePublisher messagePublisher;

  VocabularyResource(
      VocabularyService vocabularyService,
      ExportService exportService,
      ExportConfig exportConfig,
      @Autowired(required = false) MessagePublisher messagePublisher) {
    this.vocabularyService = vocabularyService;
    this.exportService = exportService;
    this.exportConfig = exportConfig;
    this.messagePublisher = messagePublisher;
  }

  @Operation(
      operationId = "listVocabularies",
      summary = "List all vocabularies",
      description = "Lists all current vocabularies.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0100")))
  @Parameters(
      value = {
        @Parameter(
            name = "name",
            description = "The name of the vocabulary.",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "namespace",
            description = "The namespace of the vocabulary.",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "deprecated",
            description = "Is the vocabulary deprecated?",
            schema = @Schema(implementation = Boolean.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "key",
            description = "The key of the vocabulary.",
            schema = @Schema(implementation = Long.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE),
        @Parameter(
            name = "hasUnreleasedChanges",
            description = "Has the vocabulary changes that haven't been released yet?",
            schema = @Schema(implementation = Boolean.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE)
      })
  @CommonParameters.QParameter
  @Pageable.OffsetLimitParameters
  @Parameter(name = "params", hidden = true)
  @Docs.DefaultSearchResponses
  @GetMapping
  public PagingResponse<Vocabulary> listVocabularies(VocabularyListParams params) {
    return vocabularyService.list(
        VocabularySearchParams.builder()
            .namespace(params.getNamespace())
            .name(params.getName())
            .deprecated(params.getDeprecated())
            .key(params.getKey())
            .query(params.getQ())
            .hasUnreleasedChanges(params.getHasUnreleasedChanges())
            .build(),
        params.getPage());
  }

  @Operation(
      operationId = "getVocabulary",
      summary = "Get details of a single vocabulary",
      description = "Details of a single vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0110")))
  @Docs.VocabularyNamePathParameter
  @ApiResponse(responseCode = "200", description = "Vocabulary found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{name}")
  public Vocabulary get(@PathVariable("name") String vocabularyName) {
    return vocabularyService.getByName(vocabularyName);
  }

  @Operation(
      operationId = "createVocabulary",
      summary = "Create a new vocabulary",
      description =
          "Creates a new vocabulary. Note definitions and labels must be added in subsequent requests.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0201")))
  @ApiResponse(responseCode = "201", description = "Vocabulary created and returned")
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping
  public Vocabulary create(@RequestBody Vocabulary vocabulary) {
    long key = vocabularyService.create(vocabulary);
    return vocabularyService.get(key);
  }

  @Operation(
      operationId = "updateVocabulary",
      summary = "Update an existing vocabulary",
      description =
          "Updates the existing vocabulary. Note definitions and labels are not changed with this method.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0202")))
  @Docs.VocabularyNamePathParameter
  @ApiResponse(responseCode = "200", description = "Vocabulary updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping("{name}")
  public Vocabulary update(
      @PathVariable("name") String vocabularyName, @RequestBody Vocabulary vocabulary) {
    checkArgument(
        vocabularyName.equals(vocabulary.getName()),
        "Provided entity must have the same name as the resource in the URL");
    vocabularyService.update(vocabulary);
    return vocabularyService.get(vocabulary.getKey());
  }

  @Operation(
      operationId = "suggestVocabularies",
      summary = "Suggest vocabularies.",
      description =
          "Search that returns up to 20 matching vocabularies. Results are ordered by relevance. "
              + "The response is smaller than a vocabulary search.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0203")))
  @Docs.SuggestParameters
  @Docs.DefaultSearchResponses
  @GetMapping("suggest")
  public List<SuggestResult> suggest(
      @RequestParam(value = "q", required = false) String query,
      LanguageRegion locale,
      LanguageRegion fallbackLocale,
      @RequestParam(value = "limit", required = false) Integer limit) {
    return vocabularyService.suggest(query, locale, fallbackLocale, limit);
  }

  @Operation(
      operationId = "deprecateVocabulary",
      summary = "Deprecate an existing vocabulary",
      description =
          "Deprecates the existing vocabulary.\n\n Optionally, a replacement vocabulary can be specified. "
              + "Note that if the vocabulary has concepts they will be deprecated too and that has to be specified "
              + "explicitly in the parameters.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0205")))
  @Docs.VocabularyNamePathParameter
  @ApiResponse(responseCode = "204", description = "Vocabulary deprecated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping("{name}/deprecate")
  public void deprecate(
      @PathVariable("name") String vocabularyName,
      @RequestBody DeprecateVocabularyAction deprecateVocabularyAction) {
    vocabularyService.deprecate(
        getVocabularyByName(vocabularyName).getKey(),
        deprecateVocabularyAction.getDeprecatedBy(),
        deprecateVocabularyAction.getReplacementKey(),
        deprecateVocabularyAction.isDeprecateConcepts());
  }

  @Operation(
      operationId = "restoreDeprecatedVocabulary",
      summary = "Restores a deprecated vocabulary",
      description =
          "Restores the deprecated vocabulary.\n\n Optionally, its deprecated concepts can be restored too if it is "
              + "specified in the parameters.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0206")))
  @Docs.VocabularyNamePathParameter
  @ApiResponse(responseCode = "204", description = "Vocabulary restored")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{name}/deprecate")
  public void restoreDeprecated(
      @PathVariable("name") String vocabularyName,
      @RequestParam(value = "restoreDeprecatedConcepts", required = false)
          boolean restoreDeprecatedConcepts) {
    Vocabulary vocabulary = getVocabularyByName(vocabularyName);
    vocabularyService.restoreDeprecated(vocabulary.getKey(), restoreDeprecatedConcepts);
  }

  @Operation(
      operationId = "exportVocabulary",
      summary = "Exports a vocabulary",
      description = "Exports a vocabulary in JSON format.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0300")))
  @Docs.VocabularyNamePathParameter
  @ApiResponse(responseCode = "200", description = "Vocabulary exported")
  @GetMapping(value = "{name}/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Resource> exportVocabularyResource(
      @PathVariable("name") String vocabularyName) throws IOException {
    Path exportPath = exportService.exportVocabulary(vocabularyName);
    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(exportPath));

    return ResponseEntity.ok()
        .header(
            "Content-Disposition", "attachment; filename=" + exportPath.getFileName().toString())
        .body(resource);
  }

  public VocabularyRelease releaseVocabularyVersion(
      String vocabularyName, VocabularyReleaseParams params) throws IOException {

    if (!exportConfig.isReleaseEnabled()) {
      throw new UnsupportedOperationException("Vocabulary releases are not enabled");
    }

    // release the vocabulary and return
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    VocabularyRelease release =
        exportService.releaseVocabulary(
            ExportParams.builder()
                .vocabularyName(vocabularyName)
                .version(params.getVersion())
                .user(authentication.getName())
                .comment(params.getComment())
                .deployRepository(exportConfig.getDeployRepository())
                .deployUser(exportConfig.getDeployUser())
                .deployPassword(exportConfig.getDeployPassword())
                .skipUpload(exportConfig.isSkipUpload())
                .build());

    if (messagePublisher != null) {
      messagePublisher.send(
          new VocabularyReleasedMessage(
              vocabularyName, release.getVersion(), URI.create(release.getExportUrl())));
    } else {
      log.warn("Message publisher not instantiated");
    }

    return release;
  }

  @Hidden
  @PostMapping(value = "{name}/" + VOCABULARY_RELEASES_PATH)
  public ResponseEntity<VocabularyRelease> releaseVocabularyVersion(
      @PathVariable("name") String vocabularyName,
      @RequestBody VocabularyReleaseParams params,
      HttpServletRequest httpServletRequest)
      throws IOException {

    VocabularyRelease release = releaseVocabularyVersion(vocabularyName, params);

    // clear cache
    LatestReleaseCache.conceptSuggestLatestReleaseCache.remove(vocabularyName);

    return ResponseEntity.created(
            URI.create(httpServletRequest.getRequestURL() + "/" + release.getVersion()))
        .body(release);
  }

  @Operation(
      operationId = "listVocabularyReleases",
      summary = "List all the releases of a vocabulary",
      description = "Lists all releases of a vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0310")))
  @Parameters(
      value = {
        @Parameter(
            name = "version",
            description =
                "The version to filter by. To get the latest one you can specify 'latest'.",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY,
            explode = Explode.FALSE)
      })
  @Docs.VocabularyNamePathParameter
  @Pageable.OffsetLimitParameters
  @Docs.DefaultSearchResponses
  @GetMapping(value = "{name}/" + VOCABULARY_RELEASES_PATH)
  public PagingResponse<VocabularyRelease> listReleases(
      @PathVariable("name") String vocabularyName,
      @RequestParam(value = "version", required = false) String version,
      PagingRequest page) {
    return exportService.listReleases(vocabularyName, version, page);
  }

  @Operation(
      operationId = "getRelease",
      summary = "Get details of a single vocabulary release",
      description = "Details of a single vocabulary release.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0320")))
  @Parameters(
      value = {
        @Parameter(
            name = "version",
            description =
                "The version of the release. To get the latest one you can specify 'latest'.",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.PATH,
            explode = Explode.FALSE)
      })
  @Docs.VocabularyNamePathParameter
  @ApiResponse(responseCode = "200", description = "Release found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping(value = "{name}/" + VOCABULARY_RELEASES_PATH + "/{version}")
  public VocabularyRelease getRelease(
      @PathVariable("name") String vocabularyName, @PathVariable("version") String version) {
    PagingRequest page = new PagingRequest(0, 1);
    PagingResponse<VocabularyRelease> releases =
        exportService.listReleases(vocabularyName, version, page);
    return releases.getResults().isEmpty() ? null : releases.getResults().get(0);
  }

  @SneakyThrows
  public byte[] getReleaseExport(String vocabularyName, String version) {
    PagingRequest page = new PagingRequest(0, 1);
    PagingResponse<VocabularyRelease> releases =
        exportService.listReleases(vocabularyName, version, page);
    VocabularyRelease release =
        releases.getResults().isEmpty() ? null : releases.getResults().get(0);

    if (release == null) {
      return new byte[0];
    }

    Path exportPath = VocabularyDownloader.downloadVocabularyExport(release.getExportUrl());
    return Files.readAllBytes(exportPath);
  }

  @Operation(
      operationId = "getReleaseExport",
      summary = "Get the exported release",
      description = "Details of the exported release to see its content.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0330")))
  @Parameters(
      value = {
        @Parameter(
            name = "version",
            description =
                "The version of the release. To get the latest one you can specify 'latest'.",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.PATH,
            explode = Explode.FALSE)
      })
  @Docs.VocabularyNamePathParameter
  @ApiResponse(responseCode = "200", description = "Release export found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping(value = "{name}/" + VOCABULARY_RELEASES_PATH + "/{version}/export")
  @SneakyThrows
  public ResponseEntity<Resource> getReleasedExport(
      @PathVariable("name") String vocabularyName, @PathVariable("version") String version) {
    ByteArrayResource resource = new ByteArrayResource(getReleaseExport(vocabularyName, version));
    return ResponseEntity.ok().header("Content-Disposition", "inline").body(resource);
  }

  @Hidden
  @DeleteMapping("{name}")
  public void deleteVocabulary(@PathVariable("name") String vocabularyName) {
    vocabularyService.deleteVocabulary(getVocabularyByName(vocabularyName).getKey());
  }

  @Operation(
      operationId = "listVocabularyDefinitions",
      summary = "List all the definitions of the vocabulary",
      description = "Lists all definitions of the vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0400")))
  @Docs.LangParameter
  @Docs.VocabularyNamePathParameter
  @Docs.DefaultSearchResponses
  @GetMapping("{name}/definition")
  public List<Definition> listDefinitions(
      @PathVariable("name") String vocabularyName, 
      @RequestParam(value = "lang", required = false) List<LanguageRegion> lang) {
    return vocabularyService.listDefinitions(getVocabularyByName(vocabularyName).getKey(), lang);
  }

  @Operation(
      operationId = "getVocabularyDefinition",
      summary = "Get the definition of a vocabulary",
      description = "Gets the definition of the vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0410")))
  @Docs.VocabularyNamePathParameter
  @Docs.DefinitionKeyPathParameter
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{name}/definition/{key}")
  public Definition getDefinition(
      @PathVariable("name") String vocabularyName, @PathVariable("key") long definitionKey) {
    Vocabulary vocabulary = getVocabularyByName(vocabularyName);
    return vocabularyService.getDefinition(vocabulary.getKey(), definitionKey);
  }

  @Operation(
      operationId = "addVocabularyDefinition",
      summary = "Adds a definition to a vocabulary",
      description = "Creates a definition and adds it to an existing vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0420")))
  @ApiResponse(responseCode = "201", description = "Definition created and returned")
  @Docs.VocabularyNamePathParameter
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping("{name}/definition")
  public Definition addDefinition(
      @PathVariable("name") String vocabularyName, @RequestBody Definition definition) {
    Vocabulary vocabulary = getVocabularyByName(vocabularyName);
    long key = vocabularyService.addDefinition(vocabulary.getKey(), definition);
    return vocabularyService.getDefinition(vocabulary.getKey(), key);
  }

  @Operation(
      operationId = "updateVocabularyDefinition",
      summary = "Updates a definition",
      description = "Updates a definition that belongs to an existing vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0430")))
  @ApiResponse(responseCode = "200", description = "Definition updated and returned")
  @Docs.VocabularyNamePathParameter
  @Docs.DefinitionKeyPathParameter
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping("{name}/definition/{key}")
  public Definition updateDefinition(
      @PathVariable("name") String vocabularyName,
      @PathVariable("key") long definitionKey,
      @RequestBody Definition definition) {
    checkArgument(
        definition.getKey() != null && definitionKey == definition.getKey(),
        "Definition key doesn't match with the path");
    Vocabulary vocabulary = getVocabularyByName(vocabularyName);
    vocabularyService.updateDefinition(vocabulary.getKey(), definition);
    return vocabularyService.getDefinition(vocabulary.getKey(), definitionKey);
  }

  @Operation(
      operationId = "deleteVocabularyDefinition",
      summary = "Deletes a definition from a vocabulary",
      description = "Deletes a definition from an existing vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0440")))
  @ApiResponse(responseCode = "204", description = "Definition deleted")
  @Docs.VocabularyNamePathParameter
  @Docs.DefinitionKeyPathParameter
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{name}/definition/{key}")
  public void deleteDefinition(
      @PathVariable("name") String vocabularyName, @PathVariable("key") long key) {
    Vocabulary vocabulary = getVocabularyByName(vocabularyName);
    vocabularyService.deleteDefinition(vocabulary.getKey(), key);
  }

  @Operation(
      operationId = "listVocabularyLabels",
      summary = "List all the labels of the vocabulary",
      description = "Lists all labels of the vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0500")))
  @Docs.LangParameter
  @Docs.VocabularyNamePathParameter
  @Docs.DefaultSearchResponses
  @GetMapping("{name}/label")
  public List<Label> listLabels(
      @PathVariable("name") String vocabularyName, 
      @RequestParam(value = "lang", required = false) List<LanguageRegion> lang) {
    return vocabularyService.listLabels(getVocabularyByName(vocabularyName).getKey(), lang);
  }

  @Operation(
      operationId = "addVocabularyLabel",
      summary = "Adds a label to a vocabulary",
      description = "Creates a label and adds it to an existing vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0510")))
  @ApiResponse(responseCode = "201", description = "Label created and returned")
  @Docs.VocabularyNamePathParameter
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping("{name}/label")
  public Long addLabel(@PathVariable("name") String vocabularyName, @RequestBody Label label) {
    return vocabularyService.addLabel(getVocabularyByName(vocabularyName).getKey(), label);
  }

  @Operation(
      operationId = "deleteVocabularyLabel",
      summary = "Deletes a label from a vocabulary",
      description = "Deletes a label from an existing vocabulary.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0520")))
  @ApiResponse(responseCode = "204", description = "Label deleted")
  @Docs.VocabularyNamePathParameter
  @Docs.LabelKeyPathParameter
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{name}/label/{key}")
  public void deleteLabel(
      @PathVariable("name") String vocabularyName, @PathVariable("key") long key) {
    vocabularyService.deleteLabel(getVocabularyByName(vocabularyName).getKey(), key);
  }

  @NotNull
  private Vocabulary getVocabularyByName(String vocabularyName) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    if (vocabulary == null) {
      throw new EntityNotFoundException(
          EntityNotFoundException.EntityType.VOCABULARY,
          "Vocabulary " + vocabularyName + " not found");
    }
    return vocabulary;
  }
}
