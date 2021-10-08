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
import org.gbif.vocabulary.client.VocabularyClient;
import org.gbif.ws.client.ClientBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Strings;

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

    log.info("Creating the importer");
    VocabularyImporter vocabularyImporter = new VocabularyImporter(vocabularyClient, conceptClient);

    Path conceptsPath = Paths.get(cliArgs.getConceptsPath());

    if (!Files.exists(conceptsPath)) {
      throw new IllegalArgumentException("Concepts path " + conceptsPath + " doesn't exist");
    }

    Path hiddenLabelsPath = Paths.get(cliArgs.getHiddenLabelsPath());

    if (!Files.exists(hiddenLabelsPath)) {
      throw new IllegalArgumentException(
          "Hidden labels path " + hiddenLabelsPath + " doesn't exist");
    }

    if (Strings.isNullOrEmpty(cliArgs.getCsvDelimiter())) {
      throw new IllegalArgumentException("CSV delimiter is required");
    }

    log.info("Calling the importer");
    if (cliArgs.isReimport()) {
      vocabularyImporter.reimportVocabulary(
          cliArgs.getCsvDelimiter(),
          cliArgs.getListDelimiter(),
          cliArgs.getVocabularyName(),
          cliArgs.getVocabularyLabelEN(),
          cliArgs.getVocabularyDefinitionEN(),
          conceptsPath,
          hiddenLabelsPath);
    } else {
      vocabularyImporter.importVocabulary(
          cliArgs.getCsvDelimiter(),
          cliArgs.getListDelimiter(),
          cliArgs.getVocabularyName(),
          cliArgs.getVocabularyLabelEN(),
          cliArgs.getVocabularyDefinitionEN(),
          conceptsPath,
          hiddenLabelsPath);
    }
    log.info("Import done");
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

    @Parameter(
        names = {"--apiUser", "-au"},
        required = true)
    private String apiUser;

    @Parameter(
        names = {"--apiPassword", "-ap"},
        required = true,
        password = true)
    private String apiPassword;

    @Parameter(
        names = {"--vocabularyName", "-vn"},
        required = true)
    private String vocabularyName;

    @Parameter(
        names = {"--vocabularyLabelEN", "-vlen"},
        required = true)
    private String vocabularyLabelEN;

    @Parameter(names = {"--vocabularyDefinitionEN", "-vden"})
    private String vocabularyDefinitionEN;

    @Parameter(
        names = {"--conceptsPath", "-cp"},
        required = true)
    private String conceptsPath;

    @Parameter(
        names = {"--hiddenLabelsPath", "-hp"},
        required = true)
    private String hiddenLabelsPath;

    @Parameter(names = {"--reimport", "-re"})
    private boolean reimport;
  }
}
