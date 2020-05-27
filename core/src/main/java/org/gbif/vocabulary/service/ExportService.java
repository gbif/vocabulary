package org.gbif.vocabulary.service;

import java.nio.file.Path;
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

  // TODO:
  boolean deployExportToNexus(String vocabularyName, String version, Path vocabulary);
}
