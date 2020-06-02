package org.gbif.vocabulary.lookup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Utility class to download the latest version of a Vocabulary. It reuses the same http client for
 * all the calls.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class VocabularyDownloader {

  // TODO: I will have to change this to download the latest version

  private static final Logger LOG = LoggerFactory.getLogger(VocabularyDownloader.class);

  // http client
  private static final long DEFAULT_TIMEOUT_CLIENT = 60; // timeout in seconds
  private static final long DEFAULT_CACHE_SIZE = 200L * 1024 * 1024; // size in bytes
  private static final OkHttpClient HTTP_CLIENT =
      new OkHttpClient.Builder()
          .connectTimeout(DEFAULT_TIMEOUT_CLIENT, TimeUnit.SECONDS)
          .readTimeout(DEFAULT_TIMEOUT_CLIENT, TimeUnit.SECONDS)
          .cache(createCache())
          .build();

  // paths
  private static final char SLASH = '/';
  private static final String VOCABULARIES_PATH = "vocabularies/";
  private static final String DOWNLOAD_PATH = SLASH + "download";

  private static final String ERROR_MSG = "Couldn't download vocabulary %s from url %s";

  static {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    HTTP_CLIENT.cache().delete();
                  } catch (IOException e) {
                    throw new IllegalStateException("Couldn't not delete cache", e);
                  }
                }));
  }

  /**
   * Downloads the requested vocabulary from the specified URL.
   *
   * @param apiUrl URL of the API where the vocabulary has to be downloaded from
   * @param vocabularyName name of the vocabulary to download
   * @return {@link InputStream} with the vocabulary downloaded
   */
  static InputStream downloadVocabulary(String apiUrl, String vocabularyName) {
    checkArgument(!Strings.isNullOrEmpty(apiUrl));
    checkArgument(!Strings.isNullOrEmpty(vocabularyName));

    if (apiUrl.charAt(apiUrl.length() - 1) != SLASH) {
      apiUrl += SLASH;
    }

    Request request =
        new Request.Builder()
            .url(apiUrl + VOCABULARIES_PATH + vocabularyName + DOWNLOAD_PATH)
            .build();

    try {
      Response response = HTTP_CLIENT.newCall(request).execute();

      if (!response.isSuccessful()) {
        throw new IllegalArgumentException(String.format(ERROR_MSG, vocabularyName, apiUrl));
      }

      return response.body() != null ? response.body().byteStream() : null;
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
}
