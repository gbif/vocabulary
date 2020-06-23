package org.gbif.vocabulary.lookup;

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

import org.gbif.vocabulary.model.VocabularyRelease;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Utility class to download the latest version of a Vocabulary. It reuses the same http client for
 * all the calls.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class VocabularyDownloader {

  private static final Logger LOG = LoggerFactory.getLogger(VocabularyDownloader.class);
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
  static InputStream downloadLatestVocabularyVersion(String apiUrl, String vocabularyName) {
    checkArgument(!Strings.isNullOrEmpty(apiUrl));
    checkArgument(!Strings.isNullOrEmpty(vocabularyName));

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

      // request to download the latest release that we've found before
      request = new Request.Builder().url(release.getExportUrl()).build();
      response = HTTP_CLIENT.newCall(request).execute();

      // the response returns a zip file
      Path downloadedFile =
          Files.createTempFile(vocabularyName + Instant.now().toEpochMilli(), ".zip");
      Files.copy(response.body().byteStream(), downloadedFile, StandardCopyOption.REPLACE_EXISTING);

      // inside the zip file there is a json file with the vocabulary export
      Path vocabularyJsonFile = unzipVocabularyRelease(downloadedFile);

      return vocabularyJsonFile != null ? Files.newInputStream(vocabularyJsonFile) : null;
    } catch (IOException e) {
      throw new IllegalArgumentException(String.format(ERROR_MSG, vocabularyName, apiUrl), e);
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
      LOG.info("Cache file created - {}", httpCacheDirectory.getAbsolutePath());
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
        FileOutputStream fos = new FileOutputStream(unzipFile.toFile());
        int len;
        while ((len = zis.read(buffer)) > 0) {
          fos.write(buffer, 0, len);
        }
        fos.close();
      }
      zis.closeEntry();
    }

    return unzipFile;
  }
}
