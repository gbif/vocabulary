package org.gbif.vocabulary.api;

import java.io.IOException;
import java.util.List;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyRelease;
import org.gbif.vocabulary.model.export.VocabularyExport;
import org.gbif.vocabulary.model.search.KeyNameResult;

public interface VocabularyApi {

  PagingResponse<Vocabulary> listVocabularies(VocabularyListParams params);

  Vocabulary get(String vocabularyName);

  Vocabulary create(Vocabulary vocabulary);

  Vocabulary update(String vocabularyName, Vocabulary vocabulary);

  List<KeyNameResult> suggest(String query);

  void deprecate(String vocabularyName, DeprecateVocabularyAction deprecateVocabularyAction);

  void restoreDeprecated(String vocabularyName, boolean restoreDeprecatedConcepts);

  byte[] exportVocabulary(String vocabularyName) throws IOException;

  VocabularyRelease releaseVocabularyVersion(String vocabularyName, VocabularyReleaseParams params)
      throws IOException;

  PagingResponse<VocabularyRelease> listReleases(
      String vocabularyName, String version, PagingRequest page);

  VocabularyRelease getRelease(String vocabularyName, String version);

  VocabularyExport getReleaseExport(String vocabularyName, String version);
}
