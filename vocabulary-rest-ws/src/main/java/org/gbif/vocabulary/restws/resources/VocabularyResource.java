/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.VocabularyReleasedMessage;
import org.gbif.vocabulary.api.DeprecateVocabularyAction;
import org.gbif.vocabulary.api.VocabularyApi;
import org.gbif.vocabulary.api.VocabularyListParams;
import org.gbif.vocabulary.api.VocabularyReleaseParams;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyRelease;
import org.gbif.vocabulary.model.export.ExportParams;
import org.gbif.vocabulary.model.export.VocabularyExport;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.restws.config.ExportConfig;
import org.gbif.vocabulary.service.ExportService;
import org.gbif.vocabulary.service.VocabularyService;
import org.gbif.vocabulary.tools.VocabularyDownloader;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;
import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARY_RELEASES_PATH;

/** Controller for {@link org.gbif.vocabulary.model.Vocabulary} entities. */
@Slf4j
@RestController
@RequestMapping(value = VOCABULARIES_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class VocabularyResource implements VocabularyApi {

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

  @Override
  @GetMapping
  public PagingResponse<Vocabulary> listVocabularies(VocabularyListParams params) {
    return vocabularyService.list(
        VocabularySearchParams.builder()
            .namespace(params.getNamespace())
            .name(params.getName())
            .deprecated(params.getDeprecated())
            .key(params.getKey())
            .query(params.getQ())
            .build(),
        params.getPage());
  }

  @Override
  @GetMapping("{name}")
  public Vocabulary get(@PathVariable("name") String vocabularyName) {
    return vocabularyService.getByName(vocabularyName);
  }

  @Override
  @PostMapping
  public Vocabulary create(@RequestBody Vocabulary vocabulary) {
    long key = vocabularyService.create(vocabulary);
    return vocabularyService.get(key);
  }

  @Override
  @PutMapping("{name}")
  public Vocabulary update(
      @PathVariable("name") String vocabularyName, @RequestBody Vocabulary vocabulary) {
    checkArgument(
        vocabularyName.equals(vocabulary.getName()),
        "Provided entity must have the same name as the resource in the URL");
    vocabularyService.update(vocabulary);
    return vocabularyService.get(vocabulary.getKey());
  }

  @Override
  @GetMapping("suggest")
  public List<KeyNameResult> suggest(@RequestParam("q") String query) {
    return vocabularyService.suggest(query);
  }

  @Override
  @PutMapping("{name}/deprecate")
  public void deprecate(
      @PathVariable("name") String vocabularyName,
      @RequestBody DeprecateVocabularyAction deprecateVocabularyAction) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    checkArgument(vocabulary != null, "Vocabulary not found for name " + vocabularyName);

    vocabularyService.deprecate(
        vocabulary.getKey(),
        deprecateVocabularyAction.getDeprecatedBy(),
        deprecateVocabularyAction.getReplacementKey(),
        deprecateVocabularyAction.isDeprecateConcepts());
  }

  @Override
  @DeleteMapping("{name}/deprecate")
  public void restoreDeprecated(
      @PathVariable("name") String vocabularyName,
      @RequestParam(value = "restoreDeprecatedConcepts", required = false)
          boolean restoreDeprecatedConcepts) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    checkArgument(vocabulary != null, "Vocabulary not found for name " + vocabularyName);

    vocabularyService.restoreDeprecated(vocabulary.getKey(), restoreDeprecatedConcepts);
  }

  @Override
  public byte[] exportVocabulary(String vocabularyName) throws IOException {
    Path exportPath = exportService.exportVocabulary(vocabularyName);
    return Files.readAllBytes(exportPath);
  }

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

  @Override
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

  @PostMapping(value = "{name}/" + VOCABULARY_RELEASES_PATH)
  public ResponseEntity<VocabularyRelease> releaseVocabularyVersion(
      @PathVariable("name") String vocabularyName,
      @RequestBody VocabularyReleaseParams params,
      HttpServletRequest httpServletRequest)
      throws IOException {

    VocabularyRelease release = releaseVocabularyVersion(vocabularyName, params);

    return ResponseEntity.created(
            URI.create(httpServletRequest.getRequestURL() + "/" + release.getVersion()))
        .body(release);
  }

  @Override
  @GetMapping(value = "{name}/" + VOCABULARY_RELEASES_PATH)
  public PagingResponse<VocabularyRelease> listReleases(
      @PathVariable("name") String vocabularyName,
      @RequestParam(value = "version", required = false) String version,
      PagingRequest page) {
    return exportService.listReleases(vocabularyName, version, page);
  }

  @Override
  @GetMapping(value = "{name}/" + VOCABULARY_RELEASES_PATH + "/{version}")
  public VocabularyRelease getRelease(
      @PathVariable("name") String vocabularyName, @PathVariable("version") String version) {
    PagingRequest page = new PagingRequest(0, 1);
    PagingResponse<VocabularyRelease> releases =
        exportService.listReleases(vocabularyName, version, page);
    return releases.getResults().isEmpty() ? null : releases.getResults().get(0);
  }

  @Override
  @GetMapping(value = "{name}/" + VOCABULARY_RELEASES_PATH + "/{version}/export")
  public VocabularyExport getReleaseExport(
      @PathVariable("name") String vocabularyName, @PathVariable("version") String version) {
    PagingRequest page = new PagingRequest(0, 1);
    PagingResponse<VocabularyRelease> releases =
        exportService.listReleases(vocabularyName, version, page);
    VocabularyRelease release =
        releases.getResults().isEmpty() ? null : releases.getResults().get(0);

    if (release == null) {
      return null;
    }

    return VocabularyDownloader.downloadVocabularyExport(release.getExportUrl());
  }
}
