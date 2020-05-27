package org.gbif.vocabulary.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.export.ExportMetadata;
import org.gbif.vocabulary.model.export.VocabularyExport;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.config.ExportConfig;
import org.gbif.vocabulary.service.ExportService;
import org.gbif.vocabulary.service.VocabularyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

  private static final String DEPLOY_CMD_TEMPLATE =
      "mvn deploy:deploy-file "
          + "-DgroupId=org.gbif.vocabulary.export "
          + "-DartifactId=%s "
          + "-Dversion=%s "
          + "-Dpackaging=zip "
          + "-Dfile=%s "
          + "-DgeneratePom=false "
          + "-DupdateReleaseInfo=true "
          + "-Durl=\"https://%s:%s@repository.gbif.org/repository/%s/\"";

  private VocabularyService vocabularyService;
  private ConceptService conceptService;
  private ExportConfig exportConfig;

  @Autowired
  public DefaultExportService(
      VocabularyService vocabularyService,
      ConceptService conceptService,
      ExportConfig exportConfig) {
    this.vocabularyService = vocabularyService;
    this.conceptService = conceptService;
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
  public boolean deployExportToNexus(String vocabularyName, String version, Path vocabularyExport) {
    if (!exportConfig.isDeployEnabled()) {
      log.info("Version deploy disabled");
      return false;
    }

    String deployCommand =
        String.format(
            DEPLOY_CMD_TEMPLATE,
            Objects.requireNonNull(vocabularyName),
            version,
            Objects.requireNonNull(vocabularyExport).toFile().getAbsolutePath(),
            exportConfig.getDeployUser(),
            exportConfig.getDeployPassword(),
            exportConfig.getDeployRepository());

    int exitCode = Runtime.getRuntime().exec(deployCommand).waitFor();
    if (exitCode != 0) {
      log.error("Couldn't deploy vocabulary {} with version {}", vocabularyName, version);
    }

    return exitCode == 0;
  }

  public void getLastExportVersion(String vocabularyName) {
    // TODO: call to http://repository.gbif.org/content/repositories/snapshots/org/gbif/vocabulary/export/TypeStatus/maven-metadata.xml

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
}
