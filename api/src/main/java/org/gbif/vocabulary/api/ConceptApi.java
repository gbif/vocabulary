package org.gbif.vocabulary.api;

import java.util.List;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.search.KeyNameResult;

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
}
