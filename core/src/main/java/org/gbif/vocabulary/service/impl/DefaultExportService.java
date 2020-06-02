package org.gbif.vocabulary.service.impl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyRelease;
import org.gbif.vocabulary.model.export.ExportMetadata;
import org.gbif.vocabulary.model.export.VocabularyExport;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.persistence.mappers.VocabularyReleaseMapper;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.ExportService;
import org.gbif.vocabulary.service.VocabularyService;
import org.gbif.vocabulary.service.config.ExportConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Default implementation for {@link ExportService}. */
@Service
@Validated
@Slf4j
public class DefaultExportService implements ExportService {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

  // http client
  private static final long DEFAULT_TIMEOUT_CLIENT = 60; // timeout in seconds
  private static final OkHttpClient HTTP_CLIENT =
      new OkHttpClient.Builder()
          .connectTimeout(DEFAULT_TIMEOUT_CLIENT, TimeUnit.SECONDS)
          .readTimeout(DEFAULT_TIMEOUT_CLIENT, TimeUnit.SECONDS)
          .build();
  private static final String REPOSITORY_PATH_TEMPLATE =
      "/org/gbif/vocabulary/export/%s/%s/%s?upload-file=%s";

  private final VocabularyService vocabularyService;
  private final ConceptService conceptService;
  private final VocabularyReleaseMapper vocabularyReleaseMapper;
  private final ExportConfig exportConfig;

  @Autowired
  public DefaultExportService(
      VocabularyService vocabularyService,
      ConceptService conceptService,
      VocabularyReleaseMapper vocabularyReleaseMapper,
      ExportConfig exportConfig) {
    this.vocabularyService = vocabularyService;
    this.conceptService = conceptService;
    this.vocabularyReleaseMapper = vocabularyReleaseMapper;
    this.exportConfig = exportConfig;
  }

  @Override
  public Path exportVocabulary(@NotBlank String vocabularyName) {
    Vocabulary vocabulary =
        Optional.ofNullable(vocabularyService.getByName(vocabularyName))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Couldn't find vocabulary with name " + vocabularyName));

    Path exportPath = createExportFile(vocabulary.getName());

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
      jsonGen.writeObjectField(VocabularyExport.METADATA_PROP, metadata);

      // write vocabulary
      jsonGen.writeObjectField(VocabularyExport.VOCABULARY_PROP, vocabulary);

      // write concepts
      jsonGen.writeArrayFieldStart(VocabularyExport.CONCEPTS_PROP);
      writeConcepts(vocabulary, jsonGen);
      jsonGen.writeEndArray();

      // end of json
      jsonGen.writeEndObject();
    } catch (IOException e) {
      throw new IllegalStateException(
          "Could not create export for vocabulary " + vocabularyName, e);
    }

    return exportPath;
  }

  @Override
  @SneakyThrows
  public VocabularyRelease releaseVocabulary(
      @NotBlank String vocabularyName,
      @NotBlank String version,
      @NotNull Path vocabularyExport,
      @NotBlank String user) {

    if (!exportConfig.isReleaseEnabled()) {
      throw new UnsupportedOperationException("Vocabulary releases are not enabled");
    }

    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    if (vocabulary == null) {
      throw new IllegalArgumentException("vocabulary not found: " + vocabularyName);
    }

    Path zipFile = Files.createFile(Paths.get(vocabularyName + "-" + version + ".zip"));
    toZipFile(vocabularyExport, zipFile);

    String repositoryUrl =
        exportConfig.getDeployRepository()
            + String.format(
                REPOSITORY_PATH_TEMPLATE,
                vocabularyName,
                version,
                zipFile.toFile().getName(),
                zipFile.toFile().getAbsolutePath());

    // upload it to nexus
    Request request =
        new Request.Builder()
            .url(repositoryUrl)
            .method("PUT", RequestBody.create("", MediaType.parse("text/plain")))
            .addHeader(
                "Authorization",
                Credentials.basic(exportConfig.getDeployUser(), exportConfig.getDeployPassword()))
            .build();

    Response response = HTTP_CLIENT.newCall(request).execute();

    if (!response.isSuccessful()) {
      throw new IllegalStateException("Couldn't upload to nexus: " + repositoryUrl);
    }

    boolean deleted = zipFile.toFile().delete();
    if (!deleted) {
      log.warn("Couldn't delete export file: {}", zipFile.getFileName().toString());
    }

    // we store the release in the DB
    VocabularyRelease release = new VocabularyRelease();
    release.setVersion(version);
    release.setCreatedBy(user);
    release.setExportUrl(repositoryUrl);
    release.setVocabularyKey(vocabulary.getKey());
    vocabularyReleaseMapper.create(release);

    return vocabularyReleaseMapper.get(release.getKey());
  }

  @Override
  public List<VocabularyRelease> listReleases(
      @NotBlank String vocabularyName, @Nullable String version, @Nullable Pageable page) {
    page = page != null ? page : new PagingRequest();

    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    Preconditions.checkArgument(vocabulary != null, "Vocabulary can't be null");

    if ("latest".equalsIgnoreCase(version)) {
      page = new PagingRequest(0, 1);
      return vocabularyReleaseMapper.list(vocabulary.getKey(), null, page);
    } else {
      return vocabularyReleaseMapper.list(vocabulary.getKey(), version, page);
    }
  }

  private Path createExportFile(String vocabularyName) {
    try {
      return Files.createTempFile(vocabularyName + "-" + Instant.now().toEpochMilli(), ".json");
    } catch (IOException e) {
      throw new IllegalStateException(
          "Couldn't create export file for vocabulary " + vocabularyName);
    }
  }

  private void writeConcepts(Vocabulary vocabulary, JsonGenerator jsonGen) throws IOException {
    final int limit = 1000;
    int offset = 0;
    ConceptSearchParams conceptSearchParams =
        ConceptSearchParams.builder().vocabularyKey(vocabulary.getKey()).build();
    PagingResponse<Concept> concepts;
    jsonGen.flush();
    do {
      concepts = conceptService.list(conceptSearchParams, new PagingRequest(offset, limit));

      for (Concept c : concepts.getResults()) {
        jsonGen.writeObject(c);
      }

      jsonGen.flush();
      offset += limit;
    } while (!concepts.isEndOfRecords());
  }

  @SneakyThrows
  static void toZipFile(Path fileToZip, Path targetFile) {
    try (FileOutputStream fos = new FileOutputStream(targetFile.toFile().getName());
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        FileInputStream fis = new FileInputStream(fileToZip.toFile())) {
      ZipEntry zipEntry = new ZipEntry(fileToZip.toFile().getName());
      zipOut.putNextEntry(zipEntry);
      byte[] bytes = new byte[1024];
      int length;
      while ((length = fis.read(bytes)) >= 0) {
        zipOut.write(bytes, 0, length);
      }
    }
  }
}
