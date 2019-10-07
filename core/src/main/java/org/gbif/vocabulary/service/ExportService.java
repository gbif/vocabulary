package org.gbif.vocabulary.service;

import java.io.File;
import javax.validation.constraints.NotBlank;

/** Service to create exports of the vocabularies. */
public interface ExportService {

  /**
   * Exports a vocabulary with all its concepts.
   *
   * @param vocabularyName name of the vocabulary to import
   * @return {@link org.gbif.vocabulary.model.export.VocabularyExport} serialized in a json file
   */
  File exportVocabulary(@NotBlank String vocabularyName);
}
