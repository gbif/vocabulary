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
package org.gbif.vocabulary.importer;

import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.vocabulary.client.TagClient;
import org.gbif.vocabulary.client.VocabularyClient;
import org.gbif.ws.client.ClientBuilder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CliApp {

  public static void main(String[] args) {
    // parse args
    CliApp.CliArgs cliArgs = new CliApp.CliArgs();
    JCommander jCommander = new JCommander(cliArgs);
    jCommander.parse(args);

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    ClientBuilder clientBuilder = new ClientBuilder();
    VocabularyClient vocabularyClient =
        clientBuilder
            .withUrl(cliArgs.getApiUrl())
            .withExponentialBackoffRetry(Duration.ofMillis(1000), 2, 4)
            .withCredentials(cliArgs.getApiUser(), cliArgs.getApiPassword())
            .withObjectMapper(objectMapper)
            .build(VocabularyClient.class);
    ConceptClient conceptClient =
        clientBuilder
            .withUrl(cliArgs.getApiUrl())
            .withExponentialBackoffRetry(Duration.ofMillis(1000), 2, 4)
            .withCredentials(cliArgs.getApiUser(), cliArgs.getApiPassword())
            .withObjectMapper(objectMapper)
            .build(ConceptClient.class);
    TagClient tagClient =
        clientBuilder
            .withUrl(cliArgs.getApiUrl())
            .withExponentialBackoffRetry(Duration.ofMillis(1000), 2, 4)
            .withCredentials(cliArgs.getApiUser(), cliArgs.getApiPassword())
            .withObjectMapper(objectMapper)
            .build(TagClient.class);

    log.info("Creating the importer");
    VocabularyImporter vocabularyImporter =
        new VocabularyImporter(vocabularyClient, conceptClient, tagClient);

    if (cliArgs.isMigration()) {
      VocabularyClient targetVocabularyClient =
          clientBuilder
              .withUrl(cliArgs.getTargetApiUrl())
              .withExponentialBackoffRetry(Duration.ofMillis(1000), 2, 4)
              .withCredentials(cliArgs.getTargetApiUser(), cliArgs.getTargetApiPassword())
              .withObjectMapper(objectMapper)
              .build(VocabularyClient.class);
      ConceptClient targetConceptClient =
          clientBuilder
              .withUrl(cliArgs.getTargetApiUrl())
              .withExponentialBackoffRetry(Duration.ofMillis(1000), 2, 4)
              .withCredentials(cliArgs.getTargetApiUser(), cliArgs.getTargetApiPassword())
              .withObjectMapper(objectMapper)
              .build(ConceptClient.class);
      TagClient targetTagClient =
          clientBuilder
              .withUrl(cliArgs.getTargetApiUrl())
              .withExponentialBackoffRetry(Duration.ofMillis(1000), 2, 4)
              .withCredentials(cliArgs.getTargetApiUser(), cliArgs.getTargetApiPassword())
              .withObjectMapper(objectMapper)
              .build(TagClient.class);

      log.info("Calling the importer to migrate the vocabulary");
      vocabularyImporter.migrateVocabulary(
          cliArgs.getVocabularyName(),
          targetVocabularyClient,
          targetConceptClient,
          targetTagClient);

      log.info("Migration done");
    } else {
      Path hiddenLabelsPath = null;
      if (cliArgs.getHiddenLabelsPath() != null) {
        hiddenLabelsPath = Paths.get(cliArgs.getHiddenLabelsPath());

        if (!Files.exists(hiddenLabelsPath)) {
          throw new IllegalArgumentException(
              "Hidden labels path " + hiddenLabelsPath + " doesn't exist");
        }
      }

      if (cliArgs.getCsvDelimiter() == null) {
        throw new IllegalArgumentException("CSV delimiter is required");
      }

      if (cliArgs.importHiddenLabelsOnly) {
        log.info("Calling the hidden labels importer");
        vocabularyImporter.importHiddenLabels(
            parseDelimiter(cliArgs.getCsvDelimiter()),
            cliArgs.getVocabularyName(),
            hiddenLabelsPath,
            parseEncoding(cliArgs.encoding));
        return;
      }

      Path conceptsPath = Paths.get(cliArgs.getConceptsPath());

      if (!Files.exists(conceptsPath)) {
        throw new IllegalArgumentException("Concepts path " + conceptsPath + " doesn't exist");
      }

      if (cliArgs.importLabelsAndDefinitionsOnly) {
        log.info("Calling the labels and definitions importer");
        vocabularyImporter.importLabelsAndDefinitions(
            parseDelimiter(cliArgs.getCsvDelimiter()),
            cliArgs.getListDelimiter(),
            cliArgs.getVocabularyName(),
            conceptsPath,
            parseEncoding(cliArgs.encoding));
      } else {
        log.info("Calling the importer");
        vocabularyImporter.importVocabulary(
            parseDelimiter(cliArgs.getCsvDelimiter()),
            cliArgs.getListDelimiter(),
            cliArgs.getVocabularyName(),
            cliArgs.getVocabularyLabelEN(),
            cliArgs.getVocabularyDefinitionEN(),
            conceptsPath,
            hiddenLabelsPath,
            parseEncoding(cliArgs.encoding));
      }
      log.info("Import done");
    }
  }

  private static char parseDelimiter(String delimiter) {
    if ("\\t".equals(delimiter)) {
      return '\t';
    } else {
      return delimiter.charAt(0);
    }
  }

  private static Charset parseEncoding(String encoding) {
    try {
      return Charset.forName(encoding);
    } catch (UnsupportedCharsetException e) {
      log.warn("Couldn't parse encoding. Using UTF-8.", e);
      return StandardCharsets.UTF_8;
    }
  }

  @Getter
  @Setter
  public static class CliArgs {

    @Parameter(names = {"--csvDelimiter", "-d"})
    private String csvDelimiter = ",";

    @Parameter(names = {"--listDelimiter", "-ld"})
    private String listDelimiter = "\\|";

    @Parameter(
        names = {"--apiUrl", "-a"},
        required = true)
    private String apiUrl;

    @Parameter(names = {"--targetApiUrl", "-ta"})
    private String targetApiUrl;

    @Parameter(
        names = {"--apiUser", "-au"},
        required = true)
    private String apiUser;

    @Parameter(names = {"--targetApiUser", "-tau"})
    private String targetApiUser;

    @Parameter(
        names = {"--apiPassword", "-ap"},
        required = true,
        password = true)
    private String apiPassword;

    @Parameter(
        names = {"--targetApiPassword", "-tap"},
        password = true)
    private String targetApiPassword;

    @Parameter(
        names = {"--vocabularyName", "-vn"},
        required = true)
    private String vocabularyName;

    @Parameter(names = {"--vocabularyLabelEN", "-vlen"})
    private String vocabularyLabelEN;

    @Parameter(names = {"--vocabularyDefinitionEN", "-vden"})
    private String vocabularyDefinitionEN;

    @Parameter(names = {"--conceptsPath", "-cp"})
    private String conceptsPath;

    @Parameter(names = {"--hiddenLabelsPath", "-hp"})
    private String hiddenLabelsPath;

    @Parameter(names = {"--encoding", "-enc"})
    private String encoding = "UTF-8";

    @Parameter(names = {"--importHiddenLabelsOnly", "-hlo"})
    private boolean importHiddenLabelsOnly;

    @Parameter(names = {"--importLabelsAndDefinitionsOnly", "-ldo"})
    private boolean importLabelsAndDefinitionsOnly;

    @Parameter(names = {"--migration", "-mi"})
    private boolean migration;
  }
}
