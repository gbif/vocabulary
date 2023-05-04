/*
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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.AddTagAction;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.api.DeprecateConceptAction;
import org.gbif.vocabulary.model.AbstractVocabularyEntity;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Definition;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.exception.EntityNotFoundException;
import org.gbif.vocabulary.model.exception.EntityNotFoundException.EntityType;
import org.gbif.vocabulary.model.search.ChildrenResult;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.restws.config.WsConfig;
import org.gbif.vocabulary.service.ConceptService;
import org.gbif.vocabulary.service.TagService;
import org.gbif.vocabulary.service.VocabularyService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.util.Strings;
import org.springframework.http.MediaType;
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
import static org.gbif.vocabulary.restws.utils.Constants.LATEST_RELEASE_PATH;
import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;

@RestController
@RequestMapping(
    value = VOCABULARIES_PATH + "/{vocabularyName}/" + CONCEPTS_PATH,
    produces = MediaType.APPLICATION_JSON_VALUE)
public class ConceptResource {

  private final ConceptService conceptService;
  private final VocabularyService vocabularyService;
  private final TagService tagService;
  private final WsConfig wsConfig;

  ConceptResource(
      ConceptService conceptService,
      VocabularyService vocabularyService,
      TagService tagService,
      WsConfig wsConfig) {
    this.conceptService = conceptService;
    this.vocabularyService = vocabularyService;
    this.tagService = tagService;
    this.wsConfig = wsConfig;
  }

  @GetMapping()
  public PagingResponse<ConceptView> listConcepts(
      @PathVariable("vocabularyName") String vocabularyName, ConceptListParams params) {
    Vocabulary vocabulary = getVocabularyWithCheck(vocabularyName);

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
                .tags(params.getTags())
                .build(),
            params.getPage());

    Stream<ConceptView> viewStream =
        createConceptViewStream(
            conceptsPage, params, conceptService::countChildren, conceptService::findParents);

    // labels links
    viewStream =
        viewStream.map(v -> createLabelsLinks(v, vocabularyName, v.getConcept().getName()));

    return new PagingResponse<>(
        conceptsPage.getOffset(),
        conceptsPage.getLimit(),
        conceptsPage.getCount(),
        viewStream.collect(Collectors.toList()));
  }

  @GetMapping("{name}")
  public ConceptView get(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestParam(value = "includeParents", required = false) boolean includeParents,
      @RequestParam(value = "includeChildren", required = false) boolean includeChildren) {
    ConceptView conceptView =
        createConceptView(
            includeParents, includeChildren, getConceptWithCheck(conceptName, vocabularyName));

    return createLabelsLinks(conceptView, vocabularyName, conceptName);
  }

  @PostMapping
  public ConceptView create(
      @PathVariable("vocabularyName") String vocabularyName, @RequestBody Concept concept) {
    Vocabulary vocabulary = getVocabularyWithCheck(vocabularyName);
    concept.setVocabularyKey(vocabulary.getKey());

    long key = conceptService.create(concept);
    return createLabelsLinks(
        new ConceptView(conceptService.get(key)), vocabularyName, concept.getName());
  }

  @PutMapping("{name}")
  public ConceptView update(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Concept concept) {
    Vocabulary vocabulary = getVocabularyWithCheck(vocabularyName);
    checkArgument(
        vocabulary.getKey().equals(concept.getVocabularyKey()),
        "Concept vocabulary doesn't match with the resource vocabulary in the URL");
    checkArgument(
        conceptName.equals(concept.getName()),
        "Concept name doesn't match with the resource name in the URL");

    conceptService.update(concept);
    return createLabelsLinks(
        new ConceptView(conceptService.get(concept.getKey())), vocabularyName, concept.getName());
  }

  @GetMapping("suggest")
  public List<KeyNameResult> suggest(
      @PathVariable("vocabularyName") String vocabularyName,
      @RequestParam(value = "q", required = false) String query,
      LanguageRegion locale) {
    return conceptService.suggest(query, getVocabularyWithCheck(vocabularyName).getKey(), locale);
  }

  @PutMapping("{name}/deprecate")
  public void deprecate(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody DeprecateConceptAction deprecateConceptAction) {
    conceptService.deprecate(
        getConceptWithCheck(conceptName, vocabularyName).getKey(),
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
    conceptService.restoreDeprecated(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), restoreDeprecatedChildren);
  }

  @GetMapping("{name}/definition")
  public List<Definition> listDefinitions(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      List<LanguageRegion> lang) {
    return conceptService.listDefinitions(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), lang);
  }

  @GetMapping("{name}/definition/{key}")
  public Definition getDefinition(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long definitionKey) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    return conceptService.getDefinition(concept.getKey(), definitionKey);
  }

  @PostMapping("{name}/definition")
  public Definition addDefinition(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Definition definition) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    long key = conceptService.addDefinition(concept.getKey(), definition);
    return conceptService.getDefinition(concept.getKey(), key);
  }

  @PutMapping("{name}/definition/{key}")
  public Definition updateDefinition(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long definitionKey,
      @RequestBody Definition definition) {
    checkArgument(
        definitionKey == definition.getKey(), "Definition key doesn't match with the path");
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    conceptService.updateDefinition(concept.getKey(), definition);
    return conceptService.getDefinition(concept.getKey(), definitionKey);
  }

  @DeleteMapping("{name}/definition/{key}")
  public void deleteDefinition(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long key) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    conceptService.deleteDefinition(concept.getKey(), key);
  }

  @GetMapping("{name}/tags")
  public List<Tag> listTags(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName) {
    return conceptService.listTags(getConceptWithCheck(conceptName, vocabularyName).getKey());
  }

  @PutMapping("{name}/tags")
  public void addTag(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody AddTagAction addTagAction) {
    checkArgument(!Strings.isNullOrEmpty(addTagAction.getTagName()), "Tag name is required");
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    Tag tag = tagService.getByName(addTagAction.getTagName());
    checkArgument(tag != null, "Tag not found for name " + addTagAction.getTagName());
    conceptService.addTag(concept.getKey(), tag.getKey());
  }

  @DeleteMapping("{name}/tags/{tagName}")
  public void removeTag(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("tagName") String tagName) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    Tag tag = tagService.getByName(tagName);
    checkArgument(tag != null, "Tag not found for name " + tagName);
    conceptService.removeTag(concept.getKey(), tag.getKey());
  }

  @GetMapping("{name}/label")
  public List<Label> listLabels(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      List<LanguageRegion> lang) {
    return conceptService.listLabels(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), lang);
  }

  @GetMapping("{name}/alternativeLabels")
  public PagingResponse<Label> listAlternativeLabels(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      List<LanguageRegion> lang,
      Pageable page) {
    return conceptService.listAlternativeLabels(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), lang, page);
  }

  @GetMapping("{name}/hiddenLabels")
  public PagingResponse<HiddenLabel> listHiddenLabels(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      Pageable page) {
    return conceptService.listHiddenLabels(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), page);
  }

  @PostMapping("{name}/label")
  public Long addLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Label label) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    return conceptService.addLabel(concept.getKey(), label);
  }

  @PostMapping("{name}/alternativeLabels")
  public Long addAlternativeLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Label label) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    return conceptService.addAlternativeLabel(concept.getKey(), label);
  }

  @PostMapping("{name}/hiddenLabels")
  public Long addHiddenLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody HiddenLabel label) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    return conceptService.addHiddenLabel(concept.getKey(), label);
  }

  @DeleteMapping("{name}/label/{key}")
  public void deleteLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long key) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    conceptService.deleteLabel(concept.getKey(), key);
  }

  @DeleteMapping("{name}/alternativeLabels/{key}")
  public void deleteAlternativeLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long key) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    conceptService.deleteAlternativeLabel(concept.getKey(), key);
  }

  @DeleteMapping("{name}/hiddenLabels/{key}")
  public void deleteHiddenLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long key) {
    Concept concept = getConceptWithCheck(conceptName, vocabularyName);
    conceptService.deleteHiddenLabel(concept.getKey(), key);
  }

  private ConceptView createLabelsLinks(
      ConceptView conceptView, String vocabularyName, String conceptName) {
    conceptView.setAlternativeLabelsLink(
        String.format(
            "%s/vocabularies/%s/concepts/%s/alternativeLabels",
            wsConfig.getApiUrl(), vocabularyName, conceptName));
    conceptView.setHiddenLabelsLink(
        String.format(
            "%s/vocabularies/%s/concepts/%s/hiddenLabels",
            wsConfig.getApiUrl(), vocabularyName, conceptName));
    return conceptView;
  }

  @GetMapping(LATEST_RELEASE_PATH)
  public PagingResponse<ConceptView> listConceptsLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName, ConceptListParams params) {
    if (!conceptService.existsLatestReleaseView(vocabularyName)) {
      throw new EntityNotFoundException(
          EntityType.RELEASE,
          "No release view found for "
              + vocabularyName
              + ". Please make sure the vocabulary has been released.");
    }

    PagingResponse<Concept> conceptsPage =
        conceptService.listLatestRelease(
            ConceptSearchParams.builder()
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
            params.getPage(),
            vocabularyName);

    Stream<ConceptView> viewStream =
        createConceptViewStream(
            conceptsPage,
            params,
            v -> conceptService.countChildrenLatestRelease(v, vocabularyName),
            v -> conceptService.findParentsLatestRelease(v, vocabularyName));

    // labels links
    viewStream =
        viewStream.map(
            v ->
                createLabelsLinks(
                    v, vocabularyName, LATEST_RELEASE_PATH + "/" + v.getConcept().getName()));

    return new PagingResponse<>(
        conceptsPage.getOffset(),
        conceptsPage.getLimit(),
        conceptsPage.getCount(),
        viewStream.collect(Collectors.toList()));
  }

  @GetMapping(LATEST_RELEASE_PATH + "/{name}")
  public ConceptView getFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestParam(value = "includeParents", required = false) boolean includeParents,
      @RequestParam(value = "includeChildren", required = false) boolean includeChildren) {
    if (!conceptService.existsLatestReleaseView(vocabularyName)) {
      throw new EntityNotFoundException(
          EntityType.RELEASE,
          "No release view found for "
              + vocabularyName
              + ". Please make sure the vocabulary has been released.");
    }

    ConceptView conceptView =
        createConceptView(
            includeParents,
            includeChildren,
            conceptService.getByNameLatestRelease(conceptName, vocabularyName));

    return createLabelsLinks(conceptView, vocabularyName, LATEST_RELEASE_PATH + "/" + conceptName);
  }

  @GetMapping(LATEST_RELEASE_PATH + "/{name}/definition")
  public List<Definition> listDefinitionsFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      List<LanguageRegion> lang) {
    return conceptService.listDefinitionsLatestRelease(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), lang, vocabularyName);
  }

  @GetMapping(LATEST_RELEASE_PATH + "/{name}/label")
  public List<Label> listLabelsFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      List<LanguageRegion> lang) {
    return conceptService.listLabelsLatestRelease(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), lang, vocabularyName);
  }

  @GetMapping(LATEST_RELEASE_PATH + "/{name}/alternativeLabels")
  public PagingResponse<Label> listAlternativeLabelsFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      List<LanguageRegion> lang,
      Pageable page) {
    return conceptService.listAlternativeLabelsLatestRelease(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), lang, page, vocabularyName);
  }

  @GetMapping(LATEST_RELEASE_PATH + "/{name}/hiddenLabels")
  public PagingResponse<HiddenLabel> listHiddenLabelsFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      Pageable page) {
    return conceptService.listHiddenLabelsLatestRelease(
        getConceptWithCheck(conceptName, vocabularyName).getKey(), page, vocabularyName);
  }

  @GetMapping(LATEST_RELEASE_PATH + "/suggest")
  public List<KeyNameResult> suggestLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @RequestParam(value = "q", required = false) String query,
      LanguageRegion locale) {
    return conceptService.suggestLatestRelease(
        query, getVocabularyWithCheck(vocabularyName).getKey(), locale, vocabularyName);
  }

  private ConceptView createConceptView(
      boolean includeParents, boolean includeChildren, Concept concept) {
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

  public Vocabulary getVocabularyWithCheck(String vocabularyName) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    if (vocabulary == null) {
      throw new EntityNotFoundException(
          EntityType.VOCABULARY, "Vocabulary not found: " + vocabularyName);
    }
    return vocabulary;
  }

  private Concept getConceptWithCheck(String conceptName, String vocabularyName) {
    Concept concept = conceptService.getByNameAndVocabulary(conceptName, vocabularyName);
    if (concept == null) {
      throw new EntityNotFoundException(
          EntityType.CONCEPT,
          "Concept " + conceptName + " not found in vocabulary " + vocabularyName);
    }
    return concept;
  }

  private Stream<ConceptView> createConceptViewStream(
      PagingResponse<Concept> conceptsPage,
      ConceptListParams params,
      Function<List<Long>, List<ChildrenResult>> childrenFn,
      LongFunction<List<String>> parentsFn) {
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
        children = childrenFn.apply(parentKeys);
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
                      ? v.setParents(parentsFn.apply(v.getConcept().getKey()))
                      : v);
    }

    return viewStream;
  }
}
