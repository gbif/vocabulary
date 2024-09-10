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
package org.gbif.vocabulary.client;

import java.util.List;
import lombok.AllArgsConstructor;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.AddTagAction;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.api.DeprecateConceptAction;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Definition;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.LookupResult;
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
public interface ConceptClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  PagingResponse<ConceptView> listConcepts(
      @PathVariable("vocabularyName") String vocabularyName,
      @SpringQueryMap ConceptListParams params);

  @GetMapping(value = "{name}", produces = MediaType.APPLICATION_JSON_VALUE)
  ConceptView get(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestParam(value = "includeParents", required = false) boolean includeParents,
      @RequestParam(value = "includeChildren", required = false) boolean includeChildren);

  @PostMapping(
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  ConceptView create(
      @PathVariable("vocabularyName") String vocabularyName, @RequestBody Concept concept);

  @PutMapping(
      value = "{name}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  ConceptView update(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Concept concept);

  default ConceptView update(String vocabularyName, Concept concept) {
    return update(vocabularyName, concept.getName(), concept);
  }

  @GetMapping(value = "suggest", produces = MediaType.APPLICATION_JSON_VALUE)
  List<KeyNameResult> suggest(
      @PathVariable("vocabularyName") String vocabularyName,
      @SpringQueryMap SuggestParams suggestParams);

  @PutMapping(value = "{name}/deprecate", consumes = MediaType.APPLICATION_JSON_VALUE)
  void deprecate(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody DeprecateConceptAction deprecateConceptAction);

  @DeleteMapping(value = "{name}/deprecate", consumes = MediaType.APPLICATION_JSON_VALUE)
  void restoreDeprecated(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestParam(value = "restoreDeprecatedChildren", required = false)
          boolean restoreDeprecatedChildren);

  @GetMapping(value = "{name}/definition", produces = MediaType.APPLICATION_JSON_VALUE)
  List<Definition> listDefinitions(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @SpringQueryMap ListParams listParams);

  default List<Definition> listDefinitions(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      List<LanguageRegion> lang) {
    return listDefinitions(vocabularyName, conceptName, ListParams.of(lang, null, null));
  }

  @GetMapping(value = "{name}/definition/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
  Definition getDefinition(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long definitionKey);

  @PostMapping(
      value = "{name}/definition",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Definition addDefinition(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Definition definition);

  @PutMapping(
      value = "{name}/definition/{key}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Definition updateDefinition(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long definitionKey,
      @RequestBody Definition definition);

  default Definition updateDefinition(
      String vocabularyName, String conceptName, Definition definition) {
    return updateDefinition(vocabularyName, conceptName, definition.getKey(), definition);
  }

  @DeleteMapping("{name}/definition/{key}")
  void deleteDefinition(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long key);

  @GetMapping(value = "{name}/tags", produces = MediaType.APPLICATION_JSON_VALUE)
  List<Tag> listTags(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName);

  @PutMapping(value = "{name}/tags", consumes = MediaType.APPLICATION_JSON_VALUE)
  void addTag(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody AddTagAction addTagAction);

  @DeleteMapping("{name}/tags/{tagName}")
  void removeTag(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("tagName") String tagName);

  @GetMapping(value = "{name}/label", produces = MediaType.APPLICATION_JSON_VALUE)
  List<Label> listLabels(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @SpringQueryMap ListParams params);

  default List<Label> listLabels(
      String vocabularyName, String conceptName, List<LanguageRegion> languageRegion) {
    return listLabels(vocabularyName, conceptName, ListParams.of(languageRegion, null, null));
  }

  @GetMapping(value = "{name}/alternativeLabels", produces = MediaType.APPLICATION_JSON_VALUE)
  PagingResponse<Label> listAlternativeLabels(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @SpringQueryMap ListParams params);

  default PagingResponse<Label> listAlternativeLabels(
      String vocabularyName, String conceptName, List<LanguageRegion> lang, Pageable page) {
    return listAlternativeLabels(
        vocabularyName, conceptName, ListParams.of(lang, page.getOffset(), page.getLimit()));
  }

  @GetMapping(value = "{name}/hiddenLabels", produces = MediaType.APPLICATION_JSON_VALUE)
  PagingResponse<HiddenLabel> listHiddenLabels(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @SpringQueryMap PagingRequest page);

  @PostMapping(
      value = "{name}/label",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Long addLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Label label);

  @PostMapping(
      value = "{name}/alternativeLabels",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Long addAlternativeLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Label label);

  @PostMapping(
      value = "{name}/hiddenLabels",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Long addHiddenLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody HiddenLabel label);

  @DeleteMapping("{name}/label/{key}")
  void deleteLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long key);

  @DeleteMapping("{name}/alternativeLabels/{key}")
  void deleteAlternativeLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long key);

  @DeleteMapping("{name}/hiddenLabels/{key}")
  void deleteHiddenLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long key);

  @GetMapping("latestRelease")
  PagingResponse<ConceptView> listConceptsLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @SpringQueryMap ConceptListParams params);

  @GetMapping("latestRelease/{name}")
  ConceptView getFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestParam(value = "includeParents", required = false) boolean includeParents,
      @RequestParam(value = "includeChildren", required = false) boolean includeChildren);

  @GetMapping("latestRelease/{name}/definition")
  List<Definition> listDefinitionsFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @SpringQueryMap ListParams listParams);

  default List<Definition> listDefinitionsFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      List<LanguageRegion> lang) {
    return listDefinitionsFromLatestRelease(
        vocabularyName, conceptName, ListParams.of(lang, null, null));
  }

  @GetMapping("latestRelease/{name}/label")
  List<Label> listLabelsFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @SpringQueryMap ListParams params);

  default List<Label> listLabelsFromLatestRelease(
      String vocabularyName, String conceptName, List<LanguageRegion> languageRegion) {
    return listLabelsFromLatestRelease(
        vocabularyName, conceptName, ListParams.of(languageRegion, null, null));
  }

  @GetMapping("latestRelease/{name}/alternativeLabels")
  PagingResponse<Label> listAlternativeLabelsFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @SpringQueryMap ListParams params);

  default PagingResponse<Label> listAlternativeLabelsFromLatestRelease(
      String vocabularyName, String conceptName, List<LanguageRegion> lang, Pageable page) {
    return listAlternativeLabelsFromLatestRelease(
        vocabularyName, conceptName, ListParams.of(lang, page.getOffset(), page.getLimit()));
  }

  @GetMapping("latestRelease/{name}/hiddenLabels")
  PagingResponse<HiddenLabel> listHiddenLabelsFromLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @SpringQueryMap ListParams params);

  default PagingResponse<HiddenLabel> listHiddenLabelsFromLatestRelease(
      String vocabularyName, String conceptName, Pageable page) {
    return listHiddenLabelsFromLatestRelease(
        vocabularyName, conceptName, ListParams.of(null, page.getOffset(), page.getLimit()));
  }

  @GetMapping("latestRelease/suggest")
  List<KeyNameResult> suggestLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @SpringQueryMap SuggestParams suggestParams);

  @GetMapping(value = "lookup", produces = MediaType.APPLICATION_JSON_VALUE)
  List<LookupResult> lookup(
      @PathVariable("vocabularyName") String vocabularyName,
      @RequestParam("q") String q,
      @SpringQueryMap LanguageRegion lang);

  @GetMapping(value = "latestRelease/lookup", produces = MediaType.APPLICATION_JSON_VALUE)
  List<LookupResult> lookupInLatestRelease(
      @PathVariable("vocabularyName") String vocabularyName,
      @RequestParam("q") String q,
      @SpringQueryMap LanguageRegion lang);

  @AllArgsConstructor(staticName = "of")
  class ListParams {
    List<LanguageRegion> lang;
    Long offset;
    Integer limit;
  }

  @AllArgsConstructor(staticName = "of")
  class SuggestParams {
    LanguageRegion locale;
    LanguageRegion fallbackLocale;
    String q;
    Integer limit;
  }
}
