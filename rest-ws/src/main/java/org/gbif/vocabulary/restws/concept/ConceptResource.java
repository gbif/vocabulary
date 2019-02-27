package org.gbif.vocabulary.restws.concept;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.service.ConceptService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.gbif.vocabulary.restws.vocabulary.VocabularyResource.VOCABULARIES_PATH;

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

}
