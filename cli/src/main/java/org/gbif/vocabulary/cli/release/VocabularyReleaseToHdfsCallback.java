package org.gbif.vocabulary.cli.release;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.gbif.common.messaging.AbstractMessageCallback;
import org.gbif.common.messaging.api.messages.VocabularyReleasedMessage;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import com.google.common.base.Strings;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
public class VocabularyReleaseToHdfsCallback
    extends AbstractMessageCallback<VocabularyReleasedMessage> {

  // http client
  private static final long DEFAULT_TIMEOUT_CLIENT = 60; // timeout in seconds
  private static final long DEFAULT_CACHE_SIZE = 200L * 1024 * 1024; // size in bytes
  private static final OkHttpClient HTTP_CLIENT =
      new OkHttpClient.Builder()
          .connectTimeout(DEFAULT_TIMEOUT_CLIENT, TimeUnit.SECONDS)
          .readTimeout(DEFAULT_TIMEOUT_CLIENT, TimeUnit.SECONDS)
          .cache(createCache())
          .build();

  private final VocabularyReleaseToHdfsConfiguration config;

  public VocabularyReleaseToHdfsCallback(VocabularyReleaseToHdfsConfiguration config) {
    this.config = config;
  }

  @Override
  @SneakyThrows
  public void handleMessage(VocabularyReleasedMessage vocabularyReleasedMessage) {
    if (!vocabularyReleasedMessage
        .getReleaseDownloadUrl()
        .toString()
        .contains(
            vocabularyReleasedMessage.getVocabularyName()
                + "/"
                + vocabularyReleasedMessage.getVersion())) {
      log.error("Invalid message for a vocabulary release: {}", vocabularyReleasedMessage);
      return;
    }

    try {
      // request to download the released vocabulary
      Request request =
          new Request.Builder()
              .url(vocabularyReleasedMessage.getReleaseDownloadUrl().toString())
              .build();

      Response response = HTTP_CLIENT.newCall(request).execute();

      // the response returns a zip file
      Path downloadedFile =
          Files.createTempFile(
              vocabularyReleasedMessage.getVocabularyName() + Instant.now().toEpochMilli(), ".zip");
      Files.copy(response.body().byteStream(), downloadedFile, StandardCopyOption.REPLACE_EXISTING);

      // inside the zip file there is a json file with the vocabulary export
      Path vocabularyJsonFile = unzipVocabularyRelease(downloadedFile);

      if (vocabularyJsonFile == null || vocabularyJsonFile.toFile().length() == 0) {
        log.error(
            "Empty vocabulary {} for version",
            vocabularyReleasedMessage.getVocabularyName(),
            vocabularyReleasedMessage.getVersion());
        throw new IllegalArgumentException("Empty vocabulary");
      }

      FileSystem fs =
          FileSystem.get(
              URI.create(config.hdfsPrefix), getHdfsConfiguration(config.hdfsSiteConfig));

      org.apache.hadoop.fs.Path sourcePath =
          new org.apache.hadoop.fs.Path(vocabularyJsonFile.toFile().getAbsolutePath());
      org.apache.hadoop.fs.Path targetPath =
          new org.apache.hadoop.fs.Path(
              config.targetDirectory, vocabularyReleasedMessage.getVocabularyName() + ".json");
      fs.copyFromLocalFile(true, true, sourcePath, targetPath);
      log.info(
          "Copied vocabulary {} with version {} to dir {}",
          vocabularyReleasedMessage.getVocabularyName(),
          vocabularyReleasedMessage.getVersion(),
          targetPath);

    } catch (IOException e) {
      log.error("Couldn't copy vocabulary to hdfs", e);
      throw new IllegalArgumentException(
          "Couldn't copy to hdfs vocabulary "
              + vocabularyReleasedMessage.getVocabularyName()
              + " and version "
              + vocabularyReleasedMessage.getVersion(),
          e);
    }
  }

  @SneakyThrows
  private static Configuration getHdfsConfiguration(String hdfsSiteConfig) {
    Configuration config = new Configuration();

    // check if the hdfs-site.xml is provided
    if (!Strings.isNullOrEmpty(hdfsSiteConfig)) {
      File hdfsSite = new File(hdfsSiteConfig);
      if (hdfsSite.exists() && hdfsSite.isFile()) {
        log.info("using hdfs-site.xml");
        config.addResource(hdfsSite.toURI().toURL());
      } else {
        log.warn("hdfs-site.xml does not exist");
      }
    }
    return config;
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
