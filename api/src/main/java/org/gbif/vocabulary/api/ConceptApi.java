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

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.search.KeyNameResult;

import java.util.List;

public interface ConceptApi {

  PagingResponse<ConceptView> listConcepts(String vocabularyName, ConceptListParams params);

  ConceptView get(
      String vocabularyName, String conceptName, boolean includeParents, boolean includeChildren);

  Concept create(String vocabularyName, Concept concept);

  Concept update(String vocabularyName, String conceptName, Concept concept);

  List<KeyNameResult> suggest(String vocabularyName, String query);

  void deprecate(
      String vocabularyName, String conceptName, DeprecateConceptAction deprecateConceptAction);

  void restoreDeprecated(
      String vocabularyName, String conceptName, boolean restoreDeprecatedChildren);

  void addTag(String vocabularyName, String conceptName, AddTagAction addTagAction);

  void removeTag(String vocabularyName, String conceptName, String tagName);

  List<Tag> listTags(String vocabularyName, String conceptName);
}
