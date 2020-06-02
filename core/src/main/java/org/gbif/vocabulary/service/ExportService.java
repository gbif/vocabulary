package org.gbif.vocabulary.service;

import java.nio.file.Path;
import java.util.List;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.VocabularyRelease;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
   * Releases a vocabulary for an specific version.
   *
   * @param vocabularyName name of the vocabulary to release
   * @param version version to release
   * @param vocabularyExport export file that contains the vocabulary
   * @param user user that created the release
   * @return the created {@link VocabularyRelease}
   */
  VocabularyRelease releaseVocabulary(
      @NotBlank String vocabularyName,
      @NotBlank String version,
      @NotNull Path vocabularyExport,
      @NotBlank String user);

  /**
   * Lists the vocabulary releases for a vocabulary and optionally a specific version. If the
   * version is "latest" it will return the latest released version.
   *
   * @param vocabularyName name of the vocabulary
   * @param version version to retrieve. "Latest" is accepted and it will return the latest release.
   * @param page paging parameters
   * @return list of {@link VocabularyRelease}
   */
  List<VocabularyRelease> listReleases(
      @NotBlank String vocabularyName, @Nullable String version, @Nullable Pageable page);
}
