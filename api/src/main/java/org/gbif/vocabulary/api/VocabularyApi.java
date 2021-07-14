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
package org.gbif.vocabulary.api;

import java.io.IOException;
import java.util.List;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyRelease;
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

  byte[] getReleaseExport(String vocabularyName, String version);
}
