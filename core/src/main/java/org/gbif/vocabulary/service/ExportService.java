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
package org.gbif.vocabulary.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.VocabularyRelease;
import org.gbif.vocabulary.model.export.Export;
import org.gbif.vocabulary.model.export.ExportParams;

import java.nio.file.Path;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;

/** Service to create exports of the vocabularies. */
public interface ExportService {

  /**
   * Exports a vocabulary with all its concepts.
   *
   * @param vocabularyName name of the vocabulary to import
   * @return path of the file that contains the {@link
   *     Export} serialized in json
   */
  Path exportVocabulary(@NotBlank String vocabularyName);

  /**
   * Exports a vocabulary with all its concepts for an specific version.
   *
   * @param vocabularyName name of the vocabulary to import
   * @param version version to set in the metadata
   * @return path of the file that contains the {@link
   *     Export} serialized in json
   */
  Path exportVocabulary(@NotBlank String vocabularyName, String version);

  /**
   * Releases a vocabulary for an specific version.
   *
   * @param exportParams params for the release
   * @return the created {@link VocabularyRelease}
   */
  VocabularyRelease releaseVocabulary(ExportParams exportParams);

  /**
   * Lists the vocabulary releases for a vocabulary and optionally a specific version. If the
   * version is "latest" it will return the latest released version.
   *
   * @param vocabularyName name of the vocabulary
   * @param version version to retrieve. "Latest" is accepted and it will return the latest release.
   * @param page paging parameters
   * @return list of {@link VocabularyRelease}
   */
  PagingResponse<VocabularyRelease> listReleases(
      @NotBlank String vocabularyName, @Nullable String version, @Nullable Pageable page);
}
