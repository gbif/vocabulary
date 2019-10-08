package org.gbif.vocabulary.lookup;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Utility class to download the latest version of a Vocabulary. It reuses the same http client for
 * all the calls.
 */
class VocabularyDownloader {

  private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
  private static final char SLASH = '/';
  private static final String VOCABULARIES_PATH = "vocabularies/";
  private static final String DOWNLOAD_PATH = SLASH + "download";

  private static final String ERROR_MSG = "Couldn't download vocabulary %s from url %s";

  private VocabularyDownloader() {}

  /**
   * Downloads the requested vocabulary from the specified URL.
   *
   * @param apiUrl URL of the API where the vocabulary has to be downloaded from
   * @param vocabularyName name of the vocabulary to download
   * @return {@link InputStream} with the vocabulary downloaded
   */
  static InputStream downloadVocabulary(String apiUrl, String vocabularyName) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(apiUrl));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vocabularyName));

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

      return response.body().byteStream();
    } catch (IOException e) {
      throw new IllegalArgumentException(String.format(ERROR_MSG, vocabularyName, apiUrl), e);
    }
  }
}
