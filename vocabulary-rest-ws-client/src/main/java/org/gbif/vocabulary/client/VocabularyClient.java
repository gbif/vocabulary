package org.gbif.vocabulary.client;

import java.util.List;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.api.DeprecateVocabularyAction;
import org.gbif.vocabulary.api.VocabularyApi;
import org.gbif.vocabulary.api.VocabularyListParams;
import org.gbif.vocabulary.api.VocabularyReleaseParams;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyRelease;
import org.gbif.vocabulary.model.export.VocabularyExport;
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
public interface VocabularyClient extends VocabularyApi {

  @Override
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  PagingResponse<Vocabulary> listVocabularies(@SpringQueryMap VocabularyListParams params);

  @Override
  @GetMapping(value = "{name}", produces = MediaType.APPLICATION_JSON_VALUE)
  Vocabulary get(@PathVariable("name") String name);

  @Override
  @PostMapping(
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Vocabulary create(@RequestBody Vocabulary vocabulary);

  @Override
  @PutMapping(
      value = "{name}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Vocabulary update(
      @PathVariable("name") String vocabularyName, @RequestBody Vocabulary vocabulary);

  @Override
  @GetMapping(value = "suggest", produces = MediaType.APPLICATION_JSON_VALUE)
  List<KeyNameResult> suggest(@RequestParam("q") String query);

  @Override
  @PutMapping(
      value = "{name}/deprecate",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void deprecate(
      @PathVariable("name") String vocabularyName,
      @RequestBody DeprecateVocabularyAction deprecateVocabularyAction);

  @Override
  @DeleteMapping(
      value = "{name}/deprecate",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void restoreDeprecated(
      @PathVariable("name") String vocabularyName,
      @RequestParam(value = "restoreDeprecatedConcepts", required = false)
          boolean restoreDeprecatedConcepts);

  @Override
  @GetMapping(value = "{name}/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  byte[] exportVocabulary(@PathVariable("name") String vocabularyName);

  @Override
  @PostMapping(
      value = "{name}/releases",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  VocabularyRelease releaseVocabularyVersion(
      @PathVariable("name") String vocabularyName, @RequestBody VocabularyReleaseParams params);

  @Override
  @GetMapping(value = "{name}/releases", produces = MediaType.APPLICATION_JSON_VALUE)
  PagingResponse<VocabularyRelease> listReleases(
      @PathVariable("name") String vocabularyName,
      @RequestParam(value = "version", required = false) String version,
      PagingRequest page);

  @Override
  @GetMapping(value = "{name}/releases/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
  VocabularyRelease getRelease(
      @PathVariable("name") String vocabularyName, @PathVariable("version") String version);

  @Override
  @GetMapping(
      value = "{name}/releases/{version}/export",
      produces = MediaType.APPLICATION_JSON_VALUE)
  VocabularyExport getReleaseExport(
      @PathVariable("name") String vocabularyName, @PathVariable("version") String version);
}
