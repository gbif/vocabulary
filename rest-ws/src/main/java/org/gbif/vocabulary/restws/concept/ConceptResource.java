package org.gbif.vocabulary.restws.concept;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.VocabularyService;

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
@RequestMapping(VOCABULARIES_PATH + "/{vocabularyName}/concepts")
public class ConceptResource {

  private final ConceptService conceptService;
  private final VocabularyService vocabularyService;

  @Autowired
  ConceptResource(ConceptService conceptService, VocabularyService vocabularyService) {
    this.conceptService = conceptService;
    this.vocabularyService = vocabularyService;
  }

  @GetMapping()
  PagingResponse<Concept> listConcepts(
      @PathVariable("vocabularyName") String vocabularyName,
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "parentKey", required = false) Integer parentKey,
      @RequestParam(value = "replacedByKey", required = false) Integer replacedByKey,
      @RequestParam(value = "deprecated", required = false) Boolean deprecated,
      PagingRequest page) {
    // TODO: add LinkHeader??

    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);

    return conceptService.list(
        ConceptSearchParams.builder()
            .vocabularyKey(vocabulary.getKey())
            .query(query)
            .name(name)
            .parentKey(parentKey)
            .replacedByKey(replacedByKey)
            .deprecated(deprecated)
            .build(),
        page);
  }

  @GetMapping("{name}")
  Concept get(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName) {
    return conceptService.getByNameAndVocabulary(conceptName, vocabularyName);
  }

  @PostMapping
  int create(@PathVariable("vocabularyName") String vocabularyName, @RequestBody Concept concept) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    checkArgument(
        vocabulary.getKey().equals(concept.getVocabularyKey()),
        "Concept vocabulary doesn't match with the resource vocabulary in the URL");
    // TODO: set auditable fields
    // TODO: add location header
    // TODO: return the whole object instead of only the key??
    return conceptService.create(concept);
  }

  @PutMapping("{name}")
  void update(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Concept concept) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    checkArgument(
        vocabulary.getKey().equals(concept.getVocabularyKey()),
        "Concept vocabulary doesn't match with the resource vocabulary in the URL");
    checkArgument(
        conceptName.equals(concept.getName()),
        "Concept name doesn't match with the resource name in the URL");
    conceptService.update(concept);
  }

  @GetMapping("suggest")
  List<KeyNameResult> suggest(
      @PathVariable("vocabularyName") String vocabularyName, @RequestParam("q") String query) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    return conceptService.suggest(query, vocabulary.getKey());
  }

  @PutMapping("{name}/deprecate")
  void deprecate(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody DeprecateConceptAction deprecateConceptAction) {
    Concept concept = conceptService.getByNameAndVocabulary(conceptName, vocabularyName);

    // TODO: set deprecatedBy
    conceptService.deprecate(
        concept.getKey(),
        "TODO",
        deprecateConceptAction.getReplacementKey(),
        deprecateConceptAction.isDeprecateChildren());
  }

  @DeleteMapping("{name}/deprecate")
  void restoreDeprecated(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestParam(value = "restoreDeprecatedChildren", required = false)
          boolean restoreDeprecatedChildren) {
    Concept concept = conceptService.getByNameAndVocabulary(conceptName, vocabularyName);
    conceptService.restoreDeprecated(concept.getKey(), restoreDeprecatedChildren);
  }
}
