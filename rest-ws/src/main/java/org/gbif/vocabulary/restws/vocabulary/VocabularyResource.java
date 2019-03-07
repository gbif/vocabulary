package org.gbif.vocabulary.restws.vocabulary;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
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

/** Controller for {@link org.gbif.vocabulary.model.Vocabulary} entities. */
@RestController
@RequestMapping(VOCABULARIES_PATH)
public class VocabularyResource {

  public static final String VOCABULARIES_PATH = "vocabularies";

  private final VocabularyService vocabularyService;

  @Autowired
  VocabularyResource(VocabularyService vocabularyService) {
    this.vocabularyService = vocabularyService;
  }

  @GetMapping
  PagingResponse<Vocabulary> listVocabularies(
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "namespace", required = false) String namespace,
      @RequestParam(value = "deprecated", required = false) Boolean deprecated,
      PagingRequest page) {

    // TODO: add Link Header??

    return vocabularyService.list(
        VocabularySearchParams.builder()
            .query(query)
            .name(name)
            .namespace(namespace)
            .deprecated(deprecated)
            .build(),
        page);
  }

  @GetMapping("{name}")
  Vocabulary get(@PathVariable("name") String vocabularyName) {
    return vocabularyService.getByName(vocabularyName);
  }

  @PostMapping
  Vocabulary create(@RequestBody Vocabulary vocabulary) {
    int key = vocabularyService.create(vocabulary);
    return vocabularyService.get(key);
  }

  @PutMapping("{name}")
  Vocabulary update(
      @PathVariable("name") String vocabularyName, @RequestBody Vocabulary vocabulary) {
    checkArgument(
        vocabularyName.equals(vocabulary.getName()),
        "Provided entity must have the same name as the resource in the URL");
    vocabularyService.update(vocabulary);
    return vocabularyService.get(vocabulary.getKey());
  }

  @GetMapping("suggest")
  List<KeyNameResult> suggest(@RequestParam("q") String query) {
    return vocabularyService.suggest(query);
  }

  @PutMapping("{name}/deprecate")
  void deprecate(
      @PathVariable("name") String vocabularyName,
      @RequestBody DeprecateVocabularyAction deprecateVocabularyAction) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);

    vocabularyService.deprecate(
        vocabulary.getKey(),
        deprecateVocabularyAction.getDeprecatedBy(),
        deprecateVocabularyAction.getReplacementKey(),
        deprecateVocabularyAction.isDeprecateConcepts());
  }

  @DeleteMapping("{name}/deprecate")
  void restoreDeprecated(
      @PathVariable("name") String vocabularyName,
      @RequestParam(value = "restoreDeprecatedConcepts", required = false)
          boolean restoreDeprecatedConcepts) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    vocabularyService.restoreDeprecated(vocabulary.getKey(), restoreDeprecatedConcepts);
  }
}
