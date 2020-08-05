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
package org.gbif.vocabulary.service.export;

import org.gbif.vocabulary.model.export.ExportParams;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Utility class that allows us to decouple the upload of a vocabulary release to an external system
 * like Nexus Repository. This makes testing easier.
 */
@Component
@Slf4j
public class ReleasePersister {

  private static final String REPOSITORY_PATH_TEMPLATE = "/org/gbif/vocabulary/export/%s/%s/%s";

  // http client
  private static final long DEFAULT_TIMEOUT_CLIENT = 60; // timeout in seconds
  private static final OkHttpClient HTTP_CLIENT =
      new OkHttpClient.Builder()
          .connectTimeout(DEFAULT_TIMEOUT_CLIENT, TimeUnit.SECONDS)
          .readTimeout(DEFAULT_TIMEOUT_CLIENT, TimeUnit.SECONDS)
          .build();

  public String uploadToNexus(ExportParams exportParams, Path vocabularyExport) throws IOException {
    Path zipFile =
        Files.createFile(
            Paths.get(exportParams.getVocabularyName() + "-" + exportParams.getVersion() + ".zip"));
    toZipFile(vocabularyExport, zipFile);

    String repositoryUrl =
        exportParams.getDeployRepository()
            + String.format(
                REPOSITORY_PATH_TEMPLATE,
                exportParams.getVocabularyName(),
                exportParams.getVersion(),
                zipFile.toFile().getName());

    // upload it to nexus
    RequestBody body = RequestBody.create(zipFile.toFile(), MediaType.parse("multipart/form-data"));
    Request request =
        new Request.Builder()
            .url(repositoryUrl)
            .method("PUT", body)
            .addHeader("enctype", "multipart/form-data")
            .addHeader("Content-Type", "multipart/form-data")
            .addHeader(
                "Authorization",
                Credentials.basic(exportParams.getDeployUser(), exportParams.getDeployPassword()))
            .build();

    Response response = HTTP_CLIENT.newCall(request).execute();

    boolean deleted = zipFile.toFile().delete();
    if (!deleted) {
      log.warn("Couldn't delete export file: {}", zipFile.getFileName().toString());
    }

    if (!response.isSuccessful()) {
      throw new IllegalStateException("Couldn't upload to nexus: " + repositoryUrl);
    }

    return repositoryUrl;
  }

  @SneakyThrows
  private static void toZipFile(Path fileToZip, Path targetFile) {
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
