package org.gbif.vocabulary.service;

import java.nio.file.Path;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.VocabularyRelease;
import org.gbif.vocabulary.model.export.ExportParams;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;

/** Service to create exports of the vocabularies. */
public interface ExportService {

  /**
   * Exports a vocabulary with all its concepts.
   *
   * @param vocabularyName name of the vocabulary to import
   * @return path of the file that contains the {@link
   *     org.gbif.vocabulary.model.export.VocabularyExport} serialized in json
   */
  Path exportVocabulary(@NotBlank String vocabularyName);

  /**
   * Exports a vocabulary with all its concepts for an specific version.
   *
   * @param vocabularyName name of the vocabulary to import
   * @param version version to set in the metadata
   * @return path of the file that contains the {@link
   *     org.gbif.vocabulary.model.export.VocabularyExport} serialized in json
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
