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
  Vocabulary get(@PathVariable("key") int key) {
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
  void update(@PathVariable("key") int key, @RequestBody Vocabulary vocabulary) {
    checkArgument(
        key == vocabulary.getKey(),
        "Provided entity must have the same key as the resource in the URL");
    vocabularyService.update(vocabulary);
  }

  @GetMapping("suggest")
  List<KeyNameResult> suggest(@RequestParam("q") String query) {
    return vocabularyService.suggest(query);
  }

  @PutMapping("{key}/deprecate")
  void deprecate(
      @PathVariable("key") int key,
      @RequestBody DeprecateVocabularyAction deprecateVocabularyAction) {
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
      @PathVariable("key") int key,
      @RequestParam(value = "restoreDeprecatedConcepts", required = false)
          boolean restoreDeprecatedConcepts) {
    vocabularyService.restoreDeprecated(key, restoreDeprecatedConcepts);
  }
}
