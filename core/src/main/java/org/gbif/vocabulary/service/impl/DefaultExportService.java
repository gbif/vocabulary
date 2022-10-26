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
package org.gbif.vocabulary.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.UserRoles;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyRelease;
import org.gbif.vocabulary.model.export.ConceptExportView;
import org.gbif.vocabulary.model.export.Export;
import org.gbif.vocabulary.model.export.ExportMetadata;
import org.gbif.vocabulary.model.export.ExportParams;
import org.gbif.vocabulary.model.export.VocabularyExportView;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.persistence.mappers.VocabularyReleaseMapper;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.ExportService;
import org.gbif.vocabulary.service.VocabularyService;
import org.gbif.vocabulary.service.export.ReleasePersister;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/** Default implementation for {@link ExportService}. */
@Service
@Validated
@Slf4j
public class DefaultExportService implements ExportService {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

  private static final Pattern VERSION_PATTERN =
      Pattern.compile("([0-9]+\\.[0-9]+\\.[0-9]+)([-].*)*?");

  private final VocabularyService vocabularyService;
  private final ConceptService conceptService;
  private final VocabularyReleaseMapper vocabularyReleaseMapper;
  private final ReleasePersister releasePersister;

  @Autowired
  public DefaultExportService(
      VocabularyService vocabularyService,
      ConceptService conceptService,
      VocabularyReleaseMapper vocabularyReleaseMapper,
      ReleasePersister releasePersister) {
    this.vocabularyService = vocabularyService;
    this.conceptService = conceptService;
    this.vocabularyReleaseMapper = vocabularyReleaseMapper;
    this.releasePersister = releasePersister;
  }

  @Override
  public Path exportVocabulary(@NotBlank String vocabularyName) {
    return exportVocabulary(vocabularyName, null);
  }

  @Override
  public Path exportVocabulary(@NotBlank String vocabularyName, String version) {
    VocabularyExportView vocabularyExportView = getVocabularyExportView(vocabularyName);

    Path exportPath = createExportFile(vocabularyExportView.getVocabulary().getName());

    // write json to the file
    JsonFactory jsonFactory = OBJECT_MAPPER.getFactory();
    try (JsonGenerator jsonGen =
        jsonFactory.createGenerator(Files.newBufferedWriter(exportPath, StandardCharsets.UTF_8))) {

      // settings
      jsonGen.useDefaultPrettyPrinter();

      // root node
      jsonGen.writeStartObject();

      // write metadata
      ExportMetadata metadata = new ExportMetadata();
      metadata.setCreatedDate(LocalDateTime.now());
      if (!Strings.isNullOrEmpty(version)) {
        metadata.setVersion(version);
      }
      jsonGen.writeObjectField(Export.METADATA_PROP, metadata);

      // write vocabulary
      jsonGen.writeObjectField(Export.VOCABULARY_PROP, vocabularyExportView);

      // write concepts
      jsonGen.writeArrayFieldStart(Export.CONCEPTS_PROP);
      writeConcepts(vocabularyExportView, jsonGen);
      jsonGen.writeEndArray();

      // end of json
      jsonGen.writeEndObject();
    } catch (IOException e) {
      throw new IllegalStateException(
          "Could not create export for vocabulary " + vocabularyName, e);
    }

    return exportPath;
  }

  @Secured({UserRoles.VOCABULARY_ADMIN})
  @Override
  @SneakyThrows
  public VocabularyRelease releaseVocabulary(ExportParams exportParams) {
    checkVersionFormat(exportParams.getVersion());

    Vocabulary vocabulary = vocabularyService.getByName(exportParams.getVocabularyName());
    if (vocabulary == null) {
      throw new IllegalArgumentException(
          "vocabulary not found: " + exportParams.getVocabularyName());
    }

    // check that the version is greater than the latest
    checkVersionNumber(exportParams.getVersion(), vocabulary.getKey());

    // export the vocabulary first
    Path vocabularyExport =
        exportVocabulary(exportParams.getVocabularyName(), exportParams.getVersion());

    // upload to nexus
    String repositoryUrl = releasePersister.uploadToNexus(exportParams, vocabularyExport);

    // we store the release in the DB
    VocabularyRelease release = new VocabularyRelease();
    release.setVersion(exportParams.getVersion());
    release.setCreatedBy(exportParams.getUser());
    release.setExportUrl(repositoryUrl);
    release.setVocabularyKey(vocabulary.getKey());
    release.setComment(exportParams.getComment());
    vocabularyReleaseMapper.create(release);

    return vocabularyReleaseMapper.get(release.getKey());
  }

  @Override
  public PagingResponse<VocabularyRelease> listReleases(
      @NotBlank String vocabularyName, @Nullable String version, @Nullable Pageable page) {
    page = page != null ? page : new PagingRequest();

    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    Preconditions.checkArgument(vocabulary != null, "Vocabulary can't be null");

    if ("latest".equalsIgnoreCase(version)) {
      page = new PagingRequest(0, 1);
      return new PagingResponse<>(
          page, 1L, vocabularyReleaseMapper.list(vocabulary.getKey(), null, page));
    } else {
      return new PagingResponse<>(
          page,
          vocabularyReleaseMapper.count(vocabulary.getKey(), version),
          vocabularyReleaseMapper.list(vocabulary.getKey(), version, page));
    }
  }

  private Path createExportFile(String vocabularyName) {
    try {
      return Files.createTempFile(vocabularyName, ".json");
    } catch (IOException e) {
      throw new IllegalStateException(
          "Couldn't create export file for vocabulary " + vocabularyName);
    }
  }

  private void writeConcepts(VocabularyExportView vocabularyExportView, JsonGenerator jsonGen)
      throws IOException {
    final int limit = 1000;
    int offset = 0;
    ConceptSearchParams conceptSearchParams =
        ConceptSearchParams.builder()
            .vocabularyKey(vocabularyExportView.getVocabulary().getKey())
            .build();
    PagingResponse<Concept> concepts;
    jsonGen.flush();
    do {
      concepts = conceptService.list(conceptSearchParams, new PagingRequest(offset, limit));

      for (Concept c : concepts.getResults()) {
        jsonGen.writeObject(getConceptExportView(c));
      }

      jsonGen.flush();
      offset += limit;
    } while (!concepts.isEndOfRecords());
  }

  private void checkVersionNumber(@NotBlank String version, long vocabularyKey) {
    List<VocabularyRelease> latestRelease =
        vocabularyReleaseMapper.list(vocabularyKey, null, new PagingRequest(0, 1));
    if (latestRelease != null && !latestRelease.isEmpty()) {
      VocabularyRelease latest = latestRelease.get(0);
      long latestVersion = getVersionNumber(latest.getVersion());
      long versionParam = getVersionNumber(version);

      if (latestVersion >= versionParam) {
        throw new IllegalArgumentException(
            "Version has to be greater than the latest version for this vocabulary: "
                + latest.getVersion());
      }
    }
  }

  @VisibleForTesting
  static void checkVersionFormat(String version) {
    if (!VERSION_PATTERN.matcher(version).find()) {
      throw new IllegalArgumentException(
          "Version doesn't comply with pattern " + VERSION_PATTERN.toString());
    }
  }

  @VisibleForTesting
  static long getVersionNumber(String version) {
    Matcher matcher = VERSION_PATTERN.matcher(version);
    if (matcher.find()) {
      return Long.parseLong(matcher.group(0).replace(".", ""));
    }
    return 0;
  }

  private VocabularyExportView getVocabularyExportView(String vocabularyName) {
    Vocabulary vocabulary =
        Optional.ofNullable(vocabularyService.getByName(vocabularyName))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Couldn't find vocabulary with name " + vocabularyName));

    VocabularyExportView vocabularyExportView = new VocabularyExportView();
    vocabularyExportView.setVocabulary(vocabulary);

    List<Label> labels = vocabularyService.listLabels(vocabulary.getKey(), null);
    labels.forEach(l -> vocabularyExportView.getLabel().put(l.getLanguage(), l.getValue()));

    return vocabularyExportView;
  }

  private ConceptExportView getConceptExportView(Concept concept) {
    ConceptExportView conceptExportView = new ConceptExportView();
    conceptExportView.setConcept(concept);

    // labels
    List<Label> labels = conceptService.listLabels(concept.getKey(), null);
    labels.forEach(l -> conceptExportView.getLabel().put(l.getLanguage(), l.getValue()));

    // alternative labels
    int offset = 0;
    int limit = 1000;
    PagingResponse<Label> responseAltLabels = null;
    do {
      responseAltLabels =
          conceptService.listAlternativeLabels(
              concept.getKey(), null, new PagingRequest(offset, limit));

      responseAltLabels
          .getResults()
          .forEach(
              r ->
                  conceptExportView
                      .getAlternativeLabels()
                      .computeIfAbsent(r.getLanguage(), k -> new HashSet<>())
                      .add(r.getValue()));

      offset += limit;
    } while (!responseAltLabels.isEndOfRecords());

    // hidden labels
    offset = 0;
    PagingResponse<HiddenLabel> responseHidden = null;
    do {
      responseHidden =
          conceptService.listHiddenLabels(concept.getKey(), new PagingRequest(offset, limit));

      responseHidden
          .getResults()
          .forEach(r -> conceptExportView.getHiddenLabels().add(r.getValue()));

      offset += limit;
    } while (!responseHidden.isEndOfRecords());

    return conceptExportView;
  }
}
