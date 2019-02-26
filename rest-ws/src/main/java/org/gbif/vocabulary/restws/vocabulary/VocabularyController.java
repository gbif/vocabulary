package org.gbif.vocabulary.restws.vocabulary;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.VocabularyService;

import java.util.List;
import javax.websocket.server.PathParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.google.common.base.Preconditions.checkArgument;

/** Controller for {@link org.gbif.vocabulary.model.Vocabulary} entities. */
@RestController
@RequestMapping("vocabularies")
public class VocabularyController {

  private final VocabularyService vocabularyService;
  private final ConceptService conceptService;

  @Autowired
  VocabularyController(VocabularyService vocabularyService, ConceptService conceptService) {
    this.vocabularyService = vocabularyService;
    this.conceptService = conceptService;
  }

  @GetMapping
  PagingResponse<Vocabulary> listVocabularies(
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "namespace", required = false) String namespace,
      @RequestParam(value = "deprecated", required = false) Boolean deprecated,
      PagingRequest page) {

    // TODO: add LinkHeader??

    return vocabularyService.list(
        VocabularySearchParams.builder()
            .query(query)
            .name(name)
            .namespace(namespace)
            .deprecated(deprecated)
            .build(),
        page);
  }

  @GetMapping("{key}")
  Vocabulary get(@PathParam("key") int key) {
    return vocabularyService.get(key);
  }

  @PostMapping
  int create(@RequestBody Vocabulary vocabulary) {
    // TODO: set auditable fields
    // TODO: add location header
    // TODO: return the whole object instead of only the key??
    return vocabularyService.create(vocabulary);
  }

  @PutMapping("{key}")
  void update(@PathParam("key") int key, @RequestBody Vocabulary vocabulary) {
    checkArgument(
        key == vocabulary.getKey().intValue(),
        "Provided entity must have the same key as the resource in the URL");
    vocabularyService.update(vocabulary);
  }

  @GetMapping("suggest")
  List<KeyNameResult> suggest(@RequestParam("q") String query) {
    return vocabularyService.suggest(query);
  }

  @PutMapping("{key}/deprecate")
  void deprecate(@PathParam("key") int key, @RequestBody DeprecateVocabularyAction deprecateVocabularyAction) {
    // TODO: set deprecatedBy
    if (deprecateVocabularyAction.getReplacementKey() != null) {
      vocabularyService.deprecate(
        key,
        "TODO",
        deprecateVocabularyAction.getReplacementKey(),
        deprecateVocabularyAction.isDeprecateConcepts());
    } else {
      vocabularyService.deprecate(key, "TODO", deprecateVocabularyAction.isDeprecateConcepts());
    }
  }

  @DeleteMapping("{key}/deprecate")
  void restoreDeprecated(
      @PathParam("key") int key,
      @RequestParam(value = "restoreDeprecatedConcepts", required = false)
          boolean restoreDeprecatedConcepts) {
    vocabularyService.restoreDeprecated(key, restoreDeprecatedConcepts);
  }

  @GetMapping("{key}/concepts")
  PagingResponse<Concept> listConcepts(
      @PathParam("key") int vocabularyKey,
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "parentKet", required = false) int parentKey,
      @RequestParam(value = "replacedByKey", required = false) int replacedByKey,
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
}
