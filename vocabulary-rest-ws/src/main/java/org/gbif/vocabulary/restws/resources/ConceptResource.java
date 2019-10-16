package org.gbif.vocabulary.restws.resources;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.restws.model.DeprecateConceptAction;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.VocabularyService;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static org.gbif.vocabulary.restws.utils.Constants.CONCEPTS_PATH;
import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;

import static com.google.common.base.Preconditions.checkArgument;

@RestController
@RequestMapping(VOCABULARIES_PATH + "/{vocabularyName}/" + CONCEPTS_PATH)
public class ConceptResource {

  private final ConceptService conceptService;
  private final VocabularyService vocabularyService;

  @Autowired
  ConceptResource(ConceptService conceptService, VocabularyService vocabularyService) {
    this.conceptService = conceptService;
    this.vocabularyService = vocabularyService;
  }

  @GetMapping()
  public PagingResponse<Concept> listConcepts(
      @PathVariable("vocabularyName") String vocabularyName,
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "parentKey", required = false) Integer parentKey,
      @RequestParam(value = "replacedByKey", required = false) Integer replacedByKey,
      @RequestParam(value = "deprecated", required = false) Boolean deprecated,
      PagingRequest page) {

    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    Objects.requireNonNull(vocabulary, "Vocabulary not found for name " + vocabularyName);

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
  public Concept get(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName) {
    return conceptService.getByNameAndVocabulary(conceptName, vocabularyName);
  }

  @PostMapping
  public Concept create(
      @PathVariable("vocabularyName") String vocabularyName, @RequestBody Concept concept) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    Objects.requireNonNull(vocabulary, "Vocabulary not found for name " + vocabularyName);
    checkArgument(
        vocabulary.getKey().equals(concept.getVocabularyKey()),
        "Concept vocabulary doesn't match with the resource vocabulary in the URL");

    int key = conceptService.create(concept);
    return conceptService.get(key);
  }

  @PutMapping("{name}")
  public Concept update(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Concept concept) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    Objects.requireNonNull(vocabulary, "Vocabulary not found for name " + vocabularyName);
    checkArgument(
        vocabulary.getKey().equals(concept.getVocabularyKey()),
        "Concept vocabulary doesn't match with the resource vocabulary in the URL");
    checkArgument(
        conceptName.equals(concept.getName()),
        "Concept name doesn't match with the resource name in the URL");

    conceptService.update(concept);
    return conceptService.get(concept.getKey());
  }

  @GetMapping("suggest")
  public List<KeyNameResult> suggest(
      @PathVariable("vocabularyName") String vocabularyName, @RequestParam("q") String query) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    Objects.requireNonNull(vocabulary, "Vocabulary not found for name " + vocabularyName);

    return conceptService.suggest(query, vocabulary.getKey());
  }

  @PutMapping("{name}/deprecate")
  public void deprecate(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody DeprecateConceptAction deprecateConceptAction) {
    Concept concept = conceptService.getByNameAndVocabulary(conceptName, vocabularyName);
    Objects.requireNonNull(
        concept, "Concept not found for name " + conceptName + " and vocabulary " + vocabularyName);

    conceptService.deprecate(
        concept.getKey(),
        deprecateConceptAction.getDeprecatedBy(),
        deprecateConceptAction.getReplacementKey(),
        deprecateConceptAction.isDeprecateChildren());
  }

  @DeleteMapping("{name}/deprecate")
  public void restoreDeprecated(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestParam(value = "restoreDeprecatedChildren", required = false)
          boolean restoreDeprecatedChildren) {
    Concept concept = conceptService.getByNameAndVocabulary(conceptName, vocabularyName);
    Objects.requireNonNull(
        concept, "Concept not found for name " + conceptName + " and vocabulary " + vocabularyName);

    conceptService.restoreDeprecated(concept.getKey(), restoreDeprecatedChildren);
  }
}
