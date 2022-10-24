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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.VocabularyReleasedMessage;
import org.gbif.vocabulary.api.DeprecateVocabularyAction;
import org.gbif.vocabulary.api.VocabularyListParams;
import org.gbif.vocabulary.api.VocabularyReleaseParams;
import org.gbif.vocabulary.api.VocabularyView;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyRelease;
import org.gbif.vocabulary.model.export.ExportParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.restws.config.ExportConfig;
import org.gbif.vocabulary.restws.config.WsConfig;
import org.gbif.vocabulary.service.ExportService;
import org.gbif.vocabulary.service.VocabularyService;
import org.gbif.vocabulary.tools.VocabularyDownloader;

import org.jetbrains.annotations.NotNull;
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

import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;
import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARY_RELEASES_PATH;

import static com.google.common.base.Preconditions.checkArgument;

/** Controller for {@link org.gbif.vocabulary.model.Vocabulary} entities. */
@Slf4j
@RestController
@RequestMapping(value = VOCABULARIES_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class VocabularyResource {

  private final VocabularyService vocabularyService;
  private final ExportService exportService;
  private final ExportConfig exportConfig;
  private final MessagePublisher messagePublisher;
  private final WsConfig wsConfig;

  VocabularyResource(
      VocabularyService vocabularyService,
      ExportService exportService,
      ExportConfig exportConfig,
      WsConfig wsConfig,
      @Autowired(required = false) MessagePublisher messagePublisher) {
    this.vocabularyService = vocabularyService;
    this.exportService = exportService;
    this.exportConfig = exportConfig;
    this.wsConfig = wsConfig;
    this.messagePublisher = messagePublisher;
  }

  @GetMapping
  public PagingResponse<VocabularyView> listVocabularies(VocabularyListParams params) {
    PagingResponse<Vocabulary> vocabulariesPage =
        vocabularyService.list(
            VocabularySearchParams.builder()
                .namespace(params.getNamespace())
                .name(params.getName())
                .deprecated(params.getDeprecated())
                .key(params.getKey())
                .query(params.getQ())
                .hasUnreleasedChanges(params.getHasUnreleasedChanges())
                .build(),
            params.getPage());

    List<VocabularyView> views =
        vocabulariesPage.getResults().stream()
            .map(v -> new VocabularyView(v, createLabelsLink(v)))
            .collect(Collectors.toList());

    return new PagingResponse<>(
        vocabulariesPage.getOffset(),
        vocabulariesPage.getLimit(),
        vocabulariesPage.getCount(),
        views);
  }

  @GetMapping("{name}")
  public VocabularyView get(@PathVariable("name") String vocabularyName) {
    return getVocabularyView(vocabularyService.getByName(vocabularyName));
  }

  @PostMapping
  public VocabularyView create(@RequestBody Vocabulary vocabulary) {
    long key = vocabularyService.create(vocabulary);
    return getVocabularyView(vocabularyService.get(key));
  }

  @PutMapping("{name}")
  public VocabularyView update(
      @PathVariable("name") String vocabularyName, @RequestBody Vocabulary vocabulary) {
    checkArgument(
        vocabularyName.equals(vocabulary.getName()),
        "Provided entity must have the same name as the resource in the URL");
    vocabularyService.update(vocabulary);
    return getVocabularyView(vocabularyService.get(vocabulary.getKey()));
  }

  @GetMapping("suggest")
  public List<KeyNameResult> suggest(
      @RequestParam(value = "q", required = false) String query, LanguageRegion locale) {
    return vocabularyService.suggest(query, locale);
  }

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

  @DeleteMapping("{name}/deprecate")
  public void restoreDeprecated(
      @PathVariable("name") String vocabularyName,
      @RequestParam(value = "restoreDeprecatedConcepts", required = false)
          boolean restoreDeprecatedConcepts) {
    Vocabulary vocabulary = getVocabularyByName(vocabularyName);

    vocabularyService.restoreDeprecated(vocabulary.getKey(), restoreDeprecatedConcepts);
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

  @GetMapping(value = "{name}/" + VOCABULARY_RELEASES_PATH)
  public PagingResponse<VocabularyRelease> listReleases(
      @PathVariable("name") String vocabularyName,
      @RequestParam(value = "version", required = false) String version,
      PagingRequest page) {
    return exportService.listReleases(vocabularyName, version, page);
  }

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

  @GetMapping(value = "{name}/" + VOCABULARY_RELEASES_PATH + "/{version}/export")
  @SneakyThrows
  public ResponseEntity<Resource> getReleasedExport(
      @PathVariable("name") String vocabularyName, @PathVariable("version") String version) {
    ByteArrayResource resource = new ByteArrayResource(getReleaseExport(vocabularyName, version));
    return ResponseEntity.ok().header("Content-Disposition", "inline").body(resource);
  }

  @DeleteMapping("{name}")
  public void deleteVocabulary(@PathVariable("name") String vocabularyName) {
    vocabularyService.deleteVocabulary(getVocabularyByName(vocabularyName).getKey());
  }

  @GetMapping("{name}/labels")
  public List<Label> listLabels(
      @PathVariable("name") String vocabularyName,
      @RequestParam(required = false, value = "lang") LanguageRegion languageRegion) {
    return vocabularyService.listLabels(
        getVocabularyByName(vocabularyName).getKey(), languageRegion);
  }

  @GetMapping("{name}/labels/{key}")
  public Label getLabel(
      @PathVariable("name") String vocabularyName, @PathVariable("key") long labelKey) {
    Vocabulary vocabulary = getVocabularyByName(vocabularyName);
    Label label = vocabularyService.getLabel(labelKey);
    checkArgument(
        vocabulary.getKey().equals(label.getEntityKey()), "Label doesn't belong to the vocabulary");
    return label;
  }

  @PostMapping("{name}/labels")
  public Label addLabel(@PathVariable("name") String vocabularyName, @RequestBody Label label) {
    Vocabulary vocabulary = getVocabularyByName(vocabularyName);
    label.setEntityKey(vocabulary.getKey());
    long key = vocabularyService.addLabel(label);
    return vocabularyService.getLabel(key);
  }

  @PutMapping("{name}/labels/{key}")
  public Label updateLabel(
      @PathVariable("name") String vocabularyName,
      @PathVariable("key") long labelKey,
      @RequestBody Label label) {
    checkArgument(labelKey == label.getKey(), "Label key doesn't match with the path");
    Vocabulary vocabulary = getVocabularyByName(vocabularyName);
    checkArgument(
        vocabulary.getKey().equals(label.getEntityKey()), "Label doesn't belong to the vocabulary");
    vocabularyService.updateLabel(label);
    return vocabularyService.getLabel(labelKey);
  }

  @DeleteMapping("{name}/labels/{key}")
  public void deleteLabel(
      @PathVariable("name") String vocabularyName, @PathVariable("key") long key) {
    Vocabulary vocabulary = getVocabularyByName(vocabularyName);
    Label labelToDelete = vocabularyService.getLabel(key);
    checkArgument(
        vocabulary.getKey().equals(labelToDelete.getEntityKey()),
        "Label doesn't belong to the vocabulary");
    vocabularyService.deleteLabel(key);
  }

  private VocabularyView getVocabularyView(Vocabulary vocabulary) {
    return vocabulary != null ? new VocabularyView(vocabulary, createLabelsLink(vocabulary)) : null;
  }

  private String createLabelsLink(Vocabulary v) {
    return String.format("%s/vocabularies/%s/labels", wsConfig.getApiUrl(), v.getName());
  }

  @NotNull
  private Vocabulary getVocabularyByName(String vocabularyName) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    checkArgument(vocabulary != null, "Vocabulary not found for name " + vocabularyName);
    return vocabulary;
  }
}
