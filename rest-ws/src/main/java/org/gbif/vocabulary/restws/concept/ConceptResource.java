package org.gbif.vocabulary.restws.concept;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.service.ConceptService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.gbif.vocabulary.restws.vocabulary.VocabularyResource.VOCABULARIES_PATH;

import static com.google.common.base.Preconditions.checkArgument;

@RestController
@RequestMapping(VOCABULARIES_PATH + "/{vocabularyKey}/concepts")
public class ConceptResource {

  private final ConceptService conceptService;

  @Autowired
  ConceptResource(ConceptService conceptService) {
    this.conceptService = conceptService;
  }

  @GetMapping()
  PagingResponse<Concept> listConcepts(
      @PathVariable("vocabularyKey") int vocabularyKey,
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "parentKey", required = false) Integer parentKey,
      @RequestParam(value = "replacedByKey", required = false) Integer replacedByKey,
      @RequestParam(value = "deprecated", required = false) Boolean deprecated,
      PagingRequest page) {
    // TODO: add LinkHeader??

    return conceptService.list(
        ConceptSearchParams.builder()
            .vocabularyKey(vocabularyKey)
            .query(query)
            .name(name)
            .parentKey(parentKey)
            .replacedByKey(replacedByKey)
            .deprecated(deprecated)
            .build(),
        page);
  }

  @GetMapping("{key}")
  Concept get(@PathVariable("vocabularyKey") int vocabularyKey, @PathVariable("key") int key) {
    Concept concept = conceptService.get(key);
    checkArgument(
        vocabularyKey == concept.getVocabularyKey(),
        "Concept vocabulary doesn't match with the resource vocabulary in the URL");
    return concept;
  }

  // TODO: validate request body is not null or is it checked already??
  @PostMapping
  int create(@PathVariable("vocabularyKey") int vocabularyKey, @RequestBody Concept concept) {
    checkArgument(
        vocabularyKey == concept.getVocabularyKey(),
        "Concept vocabulary doesn't match with the resource vocabulary in the URL");
    // TODO: set auditable fields
    // TODO: add location header
    // TODO: return the whole object instead of only the key??
    return conceptService.create(concept);
  }

  @PutMapping("{key}")
  void update(
      @PathVariable("vocabularyKey") int vocabularyKey,
      @PathVariable("key") int key,
      @RequestBody Concept concept) {
    checkArgument(
        vocabularyKey == concept.getVocabularyKey(),
        "Concept vocabulary doesn't match with the resource vocabulary in the URL");
    checkArgument(
        key == concept.getKey(), "Concept key doesn't match with the resource key in the URL");
    conceptService.update(concept);
  }

  @GetMapping("suggest")
  List<KeyNameResult> suggest(
      @PathVariable("vocabularyKey") int vocabularyKey, @RequestParam("q") String query) {
    return conceptService.suggest(query, vocabularyKey);
  }

  @PutMapping("{key}/deprecate")
  void deprecate(
      @PathVariable("key") int key, @RequestBody DeprecateConceptAction deprecateConceptAction) {
    // TODO: set deprecatedBy
    if (deprecateConceptAction.getReplacementKey() != null) {
      conceptService.deprecate(
          key,
          "TODO",
          deprecateConceptAction.getReplacementKey(),
          deprecateConceptAction.isDeprecateChildren());
    } else {
      conceptService.deprecate(key, "TODO", deprecateConceptAction.isDeprecateChildren());
    }
  }

  @DeleteMapping("{key}/deprecate")
  void restoreDeprecated(
      @PathVariable("key") int key,
      @RequestParam(value = "restoreDeprecatedChildren", required = false)
          boolean restoreDeprecatedChildren) {
    conceptService.restoreDeprecated(key, restoreDeprecatedChildren);
  }
}
