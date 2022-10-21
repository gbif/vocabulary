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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.AddTagAction;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.api.DeprecateConceptAction;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.search.KeyNameResult;

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
      @PathVariable("vocabularyName") String vocabularyName, @RequestParam("q") String query);

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

  @GetMapping(value = "{name}/labels", produces = MediaType.APPLICATION_JSON_VALUE)
  List<Label> listLabels(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName);

  @GetMapping(value = "{name}/alternativeLabels", produces = MediaType.APPLICATION_JSON_VALUE)
  PagingResponse<Label> listAlternativeLabels(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @SpringQueryMap Pageable page);

  @GetMapping(value = "{name}/hiddenLabels", produces = MediaType.APPLICATION_JSON_VALUE)
  PagingResponse<HiddenLabel> listHiddenLabels(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @SpringQueryMap Pageable page);

  @GetMapping(value = "{name}/labels/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
  Label getLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long labelKey);

  @GetMapping(value = "{name}/alternativeLabels/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
  Label getAlternativeLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long labelKey);

  @GetMapping(value = "{name}/hiddenLabels/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
  HiddenLabel getHiddenLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long labelKey);

  @PostMapping(
      value = "{name}/labels",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Label addLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Label label);

  @PostMapping(
      value = "{name}/alternativeLabels",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Label addAlternativeLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody Label label);

  @PostMapping(
      value = "{name}/hiddenLabels",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  HiddenLabel addHiddenLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @RequestBody HiddenLabel label);

  @PutMapping(
      value = "{name}/labels/{key}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Label updateLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long labelKey,
      @RequestBody Label label);

  default Label updateLabel(String vocabularyName, String conceptName, Label label) {
    return updateLabel(vocabularyName, conceptName, label.getKey(), label);
  }

  @PutMapping(
      value = "{name}/alternativeLabels/{key}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Label updateAlternativeLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long labelKey,
      @RequestBody Label label);

  default Label updateAlternativeLabel(String vocabularyName, String conceptName, Label label) {
    return updateAlternativeLabel(vocabularyName, conceptName, label.getKey(), label);
  }

  @PutMapping(
      value = "{name}/hiddenLabels/{key}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  HiddenLabel updateHiddenLabel(
      @PathVariable("vocabularyName") String vocabularyName,
      @PathVariable("name") String conceptName,
      @PathVariable("key") long labelKey,
      @RequestBody HiddenLabel label);

  default HiddenLabel updateHiddenLabel(
      String vocabularyName, String conceptName, HiddenLabel label) {
    return updateHiddenLabel(vocabularyName, conceptName, label.getKey(), label);
  }

  @DeleteMapping("{name}/labels/{key}")
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
}
