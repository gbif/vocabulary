package org.gbif.vocabulary.client;

import java.util.List;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.ConceptApi;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.api.DeprecateConceptAction;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.search.KeyNameResult;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("vocabularies/{vocabularyName}/concepts")
public interface ConceptClient extends ConceptApi {

  @Override
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  PagingResponse<ConceptView> listConcepts(
      @PathVariable("vocabularyName") String vocabularyName,
      @SpringQueryMap ConceptListParams params);

  @Override
  @GetMapping(value = "{name}", produces = MediaType.APPLICATION_JSON_VALUE)
  ConceptView get(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestParam(value = "includeParents", required = false) boolean includeParents,
      @RequestParam(value = "includeChildren", required = false) boolean includeChildren);

  @Override
  @PostMapping(
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Concept create(
      @PathVariable("vocabularyName") String vocabularyName, @RequestBody Concept concept);

  @Override
  @PutMapping(
      value = "{name}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Concept update(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Concept concept);

  @Override
  @GetMapping(value = "suggest", produces = MediaType.APPLICATION_JSON_VALUE)
  List<KeyNameResult> suggest(
      @PathVariable("vocabularyName") String vocabularyName, @RequestParam("q") String query);

  @Override
  @PutMapping(value = "{name}/deprecate", consumes = MediaType.APPLICATION_JSON_VALUE)
  void deprecate(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody DeprecateConceptAction deprecateConceptAction);

  @Override
  @DeleteMapping(value = "{name}/deprecate", consumes = MediaType.APPLICATION_JSON_VALUE)
  void restoreDeprecated(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestParam(value = "restoreDeprecatedChildren", required = false)
          boolean restoreDeprecatedChildren);
}
