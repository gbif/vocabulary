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
package org.gbif.vocabulary.cli.release;

import org.gbif.common.messaging.AbstractMessageCallback;
import org.gbif.common.messaging.api.messages.VocabularyReleasedMessage;
import org.gbif.vocabulary.tools.VocabularyDownloader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import com.google.common.base.Strings;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VocabularyReleaseToHdfsCallback
    extends AbstractMessageCallback<VocabularyReleasedMessage> {

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

    if (!config.enabledSnapshotsCopy
        && vocabularyReleasedMessage.getVersion().endsWith("SNAPSHOT")) {
      log.warn(
          "Copy of snapshost versions disabled. The copy to HDFS of the vocabulary {} with version {} was skipped",
          vocabularyReleasedMessage.getVocabularyName(),
          vocabularyReleasedMessage.getVersion());
      return;
    }

    try {
      // download vocabulary file from the given url
      Path vocabularyJsonFile =
          VocabularyDownloader.downloadVocabularyFile(
              vocabularyReleasedMessage.getReleaseDownloadUrl().toString());

      if (vocabularyJsonFile == null || vocabularyJsonFile.toFile().length() == 0) {
        log.error(
            "Empty vocabulary {} for version {}",
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
}
