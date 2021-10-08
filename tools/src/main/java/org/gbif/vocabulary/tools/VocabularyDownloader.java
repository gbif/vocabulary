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
package org.gbif.vocabulary.tools;

import org.gbif.vocabulary.model.VocabularyRelease;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Utility class to download the latest version of a Vocabulary. It reuses the same http client for
 * all the calls.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class VocabularyDownloader {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  // http client
  private static final long DEFAULT_TIMEOUT_CLIENT = 60; // timeout in seconds
  private static final long DEFAULT_CACHE_SIZE = 200L * 1024 * 1024; // size in bytes
  private static final OkHttpClient HTTP_CLIENT =
      new OkHttpClient.Builder()
          .connectTimeout(DEFAULT_TIMEOUT_CLIENT, TimeUnit.SECONDS)
          .readTimeout(DEFAULT_TIMEOUT_CLIENT, TimeUnit.SECONDS)
          .cache(createCache())
          .build();

  private static final String ERROR_MSG =
      "Couldn't download the latest version of vocabulary %s from url %s";

  // paths
  private static final char SLASH = '/';
  private static final String VOCABULARIES_PATH = "vocabularies/";
  private static final String LATEST_RELEASE_PATH = SLASH + "releases/latest";

  static {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    HTTP_CLIENT.cache().delete();
                  } catch (IOException e) {
                    throw new IllegalStateException("Couldn't delete cache", e);
                  }
                }));
  }

  /**
   * Downloads the latest version of the requested vocabulary from the specified URL.
   *
   * @param apiUrl URL of the API where the vocabulary has to be downloaded from
   * @param vocabularyName name of the vocabulary to download
   * @return {@link InputStream} with the vocabulary downloaded
   */
  public static InputStream downloadLatestVocabularyVersion(String apiUrl, String vocabularyName) {
    if (apiUrl == null || apiUrl.isEmpty()) {
      throw new IllegalArgumentException("API URL is required");
    }
    if (vocabularyName == null || vocabularyName.isEmpty()) {
      throw new IllegalArgumentException("Vocabulary name is required");
    }

    if (apiUrl.charAt(apiUrl.length() - 1) != SLASH) {
      apiUrl += SLASH;
    }

    try {
      // request to get the latest release version of the vocabulary
      Request request =
          new Request.Builder()
              .url(apiUrl + VOCABULARIES_PATH + vocabularyName + LATEST_RELEASE_PATH)
              .build();

      Response response = HTTP_CLIENT.newCall(request).execute();

      if (!response.isSuccessful()) {
        throw new IllegalArgumentException(
            "Couldn't retrieve latest version of vocabulary "
                + vocabularyName
                + " from "
                + request.url());
      }

      VocabularyRelease release =
          OBJECT_MAPPER.readValue(response.body().bytes(), VocabularyRelease.class);

      if (release == null) {
        throw new IllegalArgumentException(
            "Couldn't find any release for the vocabulary: " + vocabularyName);
      }

      // inside the zip file there is a json file with the vocabulary export
      Path vocabularyJsonFile = downloadVocabularyFile(release.getExportUrl());

      return vocabularyJsonFile != null ? Files.newInputStream(vocabularyJsonFile) : null;
    } catch (IOException e) {
      throw new IllegalArgumentException(String.format(ERROR_MSG, vocabularyName, apiUrl), e);
    }
  }

  public static Path downloadVocabularyExport(String exportUrl) {
    Path vocabularyJsonFile = downloadVocabularyFile(exportUrl);

    if (vocabularyJsonFile == null || vocabularyJsonFile.toFile().length() == 0) {
      log.error("Empty vocabulary in url {} ", exportUrl);
      throw new IllegalArgumentException("Empty vocabulary");
    }

    return vocabularyJsonFile;
  }

  public static Path downloadVocabularyFile(String url) {
    try {
      // request to download the latest release that we've found before
      Request request = new Request.Builder().url(url).build();
      Response response = HTTP_CLIENT.newCall(request).execute();

      // the response returns a zip file
      Path downloadedFile =
          Files.createTempFile("download-" + Instant.now().toEpochMilli(), ".zip");
      Files.copy(response.body().byteStream(), downloadedFile, StandardCopyOption.REPLACE_EXISTING);

      // return the json file with the vocabulary export which is inside the zip file
      return unzipVocabularyRelease(downloadedFile);
    } catch (IOException e) {
      log.error("Couldn't copy vocabulary to hdfs", e);
      throw new IllegalArgumentException("Couldn't download vocabulary from " + url, e);
    }
  }

  /**
   * Creates a Cache using a maximum size.
   *
   * @return a new instance of file based cache
   */
  private static Cache createCache() {
    try {
      // use a new file cache for the current session
      String cacheName = System.currentTimeMillis() + "-downloadCache";
      File httpCacheDirectory = Files.createTempDirectory(cacheName).toFile();
      httpCacheDirectory.deleteOnExit();
      log.info("Cache file created - {}", httpCacheDirectory.getAbsolutePath());
      // create cache
      return new Cache(httpCacheDirectory, DEFAULT_CACHE_SIZE);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Cannot run without the ability to create temporary cache directory", e);
    }
  }

  private static Path unzipVocabularyRelease(Path zipFile) throws IOException {
    Path unzipFile = null;
    byte[] buffer = new byte[1024];
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
      ZipEntry zipEntry = zis.getNextEntry();
      if (zipEntry != null) {
        unzipFile = Files.createTempFile(zipEntry.getName(), ".json");
        try (FileOutputStream fos = new FileOutputStream(unzipFile.toFile())) {
          int len;
          while ((len = zis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
          }
        }
      }
      zis.closeEntry();
    }

    return unzipFile;
  }
}
