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
package org.gbif.vocabulary.restws.resources;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.ConceptApi;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.api.DeprecateConceptAction;
import org.gbif.vocabulary.model.AbstractVocabularyEntity;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.ChildrenResult;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.VocabularyService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.vocabulary.restws.utils.Constants.CONCEPTS_PATH;
import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;

@RestController
@RequestMapping(VOCABULARIES_PATH + "/{vocabularyName}/" + CONCEPTS_PATH)
public class ConceptResource implements ConceptApi {

  private final ConceptService conceptService;
  private final VocabularyService vocabularyService;

  ConceptResource(ConceptService conceptService, VocabularyService vocabularyService) {
    this.conceptService = conceptService;
    this.vocabularyService = vocabularyService;
  }

  @Override
  @GetMapping()
  public PagingResponse<ConceptView> listConcepts(
      @PathVariable("vocabularyName") String vocabularyName, ConceptListParams params) {

    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    checkArgument(vocabulary != null, "Vocabulary not found for name " + vocabularyName);

    PagingResponse<Concept> conceptsPage =
        conceptService.list(
            ConceptSearchParams.builder()
                .vocabularyKey(vocabulary.getKey())
                .query(params.getQ())
                .name(params.getName())
                .parentKey(params.getParentKey())
                .parent(params.getParent())
                .replacedByKey(params.getReplacedByKey())
                .deprecated(params.getDeprecated())
                .key(params.getKey())
                .hasParent(params.getHasParent())
                .hasReplacement(params.getHasReplacement())
                .build(),
            params.getPage());

    Stream<ConceptView> viewStream = conceptsPage.getResults().stream().map(ConceptView::new);

    // this could be simplified by keeping only 1 param but we leave the 2 of them for backwards
    // compatibility
    if (params.isIncludeChildrenCount() || params.isIncludeChildren()) {
      // get the keys of all the concepts
      List<Long> parentKeys =
          conceptsPage.getResults().stream()
              .map(AbstractVocabularyEntity::getKey)
              .collect(Collectors.toList());

      // get the children
      List<ChildrenResult> children = new ArrayList<>();
      if (!parentKeys.isEmpty()) {
        children = conceptService.countChildren(parentKeys);
      }

      Map<Long, Set<ChildrenResult>> childrenByConcept =
          children.stream()
              .collect(Collectors.groupingBy(ChildrenResult::getParentKey, Collectors.toSet()));

      // set it to the view
      if (params.isIncludeChildrenCount()) {
        viewStream =
            viewStream.map(
                v ->
                    v.setChildrenCount(
                        childrenByConcept
                            .getOrDefault(v.getConcept().getKey(), Collections.emptySet())
                            .size()));
      }
      if (params.isIncludeChildren()) {
        viewStream =
            viewStream.map(
                v ->
                    v.setChildren(
                        childrenByConcept
                            .getOrDefault(v.getConcept().getKey(), Collections.emptySet())
                            .stream()
                            .map(ChildrenResult::getChildName)
                            .collect(Collectors.toList())));
      }
    }

    // parents
    if (params.isIncludeParents()) {
      viewStream =
          viewStream.map(
              v ->
                  v.getConcept().getParentKey() != null
                      ? v.setParents(conceptService.findParents(v.getConcept().getKey()))
                      : v);
    }

    return new PagingResponse<>(
        conceptsPage.getOffset(),
        conceptsPage.getLimit(),
        conceptsPage.getCount(),
        viewStream.collect(Collectors.toList()));
  }

  @Override
  @GetMapping("{name}")
  public ConceptView get(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestParam(value = "includeParents", required = false) boolean includeParents,
      @RequestParam(value = "includeChildren", required = false) boolean includeChildren) {
    Concept concept = conceptService.getByNameAndVocabulary(conceptName, vocabularyName);

    if (concept == null) {
      return null;
    }

    ConceptView conceptView = new ConceptView(concept);
    if (includeParents && concept.getParentKey() != null) {
      conceptView.setParents(conceptService.findParents(concept.getKey()));
    }

    if (includeChildren) {
      // get the children
      List<ChildrenResult> children =
          conceptService.countChildren(Collections.singletonList(concept.getKey()));

      // set it to the view
      conceptView.setChildren(
          children.stream().map(ChildrenResult::getChildName).collect(Collectors.toList()));
    }

    return conceptView;
  }

  @Override
  @PostMapping
  public Concept create(
      @PathVariable("vocabularyName") String vocabularyName, @RequestBody Concept concept) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    checkArgument(vocabulary != null, "Vocabulary not found for name " + vocabularyName);
    concept.setVocabularyKey(vocabulary.getKey());

    long key = conceptService.create(concept);
    return conceptService.get(key);
  }

  @Override
  @PutMapping("{name}")
  public Concept update(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Concept concept) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    checkArgument(vocabulary != null, "Vocabulary not found for name " + vocabularyName);
    checkArgument(
        vocabulary.getKey().equals(concept.getVocabularyKey()),
        "Concept vocabulary doesn't match with the resource vocabulary in the URL");
    checkArgument(
        conceptName.equals(concept.getName()),
        "Concept name doesn't match with the resource name in the URL");

    conceptService.update(concept);
    return conceptService.get(concept.getKey());
  }

  @Override
  @GetMapping("suggest")
  public List<KeyNameResult> suggest(
      @PathVariable("vocabularyName") String vocabularyName, @RequestParam("q") String query) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    checkArgument(vocabulary != null, "Vocabulary not found for name " + vocabularyName);

    return conceptService.suggest(query, vocabulary.getKey());
  }

  @Override
  @PutMapping("{name}/deprecate")
  public void deprecate(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody DeprecateConceptAction deprecateConceptAction) {
    Concept concept = conceptService.getByNameAndVocabulary(conceptName, vocabularyName);
    checkArgument(
        concept != null,
        "Concept not found for name " + conceptName + " and vocabulary " + vocabularyName);

    conceptService.deprecate(
        concept.getKey(),
        deprecateConceptAction.getDeprecatedBy(),
        deprecateConceptAction.getReplacementKey(),
        deprecateConceptAction.isDeprecateChildren());
  }

  @Override
  @DeleteMapping("{name}/deprecate")
  public void restoreDeprecated(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestParam(value = "restoreDeprecatedChildren", required = false)
          boolean restoreDeprecatedChildren) {
    Concept concept = conceptService.getByNameAndVocabulary(conceptName, vocabularyName);
    checkArgument(
        concept != null,
        "Concept not found for name " + conceptName + " and vocabulary " + vocabularyName);

    conceptService.restoreDeprecated(concept.getKey(), restoreDeprecatedChildren);
  }
}
