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

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.DeprecateVocabularyAction;
import org.gbif.vocabulary.api.VocabularyListParams;
import org.gbif.vocabulary.api.VocabularyReleaseParams;
import org.gbif.vocabulary.api.VocabularyView;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyRelease;
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

@RequestMapping("vocabularies")
public interface VocabularyClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  PagingResponse<VocabularyView> listVocabularies(@SpringQueryMap VocabularyListParams params);

  @GetMapping(value = "{name}", produces = MediaType.APPLICATION_JSON_VALUE)
  VocabularyView get(@PathVariable("name") String name);

  @PostMapping(
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  VocabularyView create(@RequestBody Vocabulary vocabulary);

  @PutMapping(
      value = "{name}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  VocabularyView update(
      @PathVariable("name") String vocabularyName, @RequestBody Vocabulary vocabulary);

  default VocabularyView update(Vocabulary vocabulary) {
    return update(vocabulary.getName(), vocabulary);
  }

  @GetMapping(value = "suggest", produces = MediaType.APPLICATION_JSON_VALUE)
  List<KeyNameResult> suggest(@RequestParam("q") String query);

  @PutMapping(
      value = "{name}/deprecate",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void deprecate(
      @PathVariable("name") String vocabularyName,
      @RequestBody DeprecateVocabularyAction deprecateVocabularyAction);

  @DeleteMapping(
      value = "{name}/deprecate",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void restoreDeprecated(
      @PathVariable("name") String vocabularyName,
      @RequestParam(value = "restoreDeprecatedConcepts", required = false)
          boolean restoreDeprecatedConcepts);

  @GetMapping(value = "{name}/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  byte[] exportVocabulary(@PathVariable("name") String vocabularyName);

  @PostMapping(
      value = "{name}/releases",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  VocabularyRelease releaseVocabularyVersion(
      @PathVariable("name") String vocabularyName, @RequestBody VocabularyReleaseParams params);

  @GetMapping(value = "{name}/releases", produces = MediaType.APPLICATION_JSON_VALUE)
  PagingResponse<VocabularyRelease> listReleases(
      @PathVariable("name") String vocabularyName,
      @RequestParam(value = "version", required = false) String version,
      PagingRequest page);

  @GetMapping(value = "{name}/releases/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
  VocabularyRelease getRelease(
      @PathVariable("name") String vocabularyName, @PathVariable("version") String version);

  @GetMapping(
      value = "{name}/releases/{version}/export",
      produces = MediaType.APPLICATION_JSON_VALUE)
  byte[] getReleaseExport(
      @PathVariable("name") String vocabularyName, @PathVariable("version") String version);

  @DeleteMapping(value = "{name}")
  void deleteVocabulary(@PathVariable("name") String vocabularyName);

  @GetMapping(value = "{name}/labels", produces = MediaType.APPLICATION_JSON_VALUE)
  List<Label> listLabels(@PathVariable("name") String vocabularyName);

  @GetMapping(value = "{name}/labels/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
  Label getLabel(@PathVariable("name") String vocabularyName, @PathVariable("key") long labelKey);

  @PostMapping(
      value = "{name}/labels",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Label addLabel(@PathVariable("name") String vocabularyName, @RequestBody Label label);

  @PutMapping(
      value = "{name}/labels/{key}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Label updateLabel(
      @PathVariable("name") String vocabularyName,
      @PathVariable("key") long labelKey,
      @RequestBody Label label);

  default Label updateLabel(String vocabularyName, Label label) {
    return updateLabel(vocabularyName, label.getKey(), label);
  }

  @DeleteMapping("{name}/labels/{key}")
  void deleteLabel(@PathVariable("name") String vocabularyName, @PathVariable("key") long key);
}
