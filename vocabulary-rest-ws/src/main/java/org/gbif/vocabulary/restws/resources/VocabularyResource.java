package org.gbif.vocabulary.restws.resources;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.restws.model.DeprecateVocabularyAction;
import org.gbif.vocabulary.service.ExportService;
import org.gbif.vocabulary.service.VocabularyService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;

import static com.google.common.base.Preconditions.checkArgument;

/** Controller for {@link org.gbif.vocabulary.model.Vocabulary} entities. */
@RestController
@RequestMapping(VOCABULARIES_PATH)
public class VocabularyResource {

  private final VocabularyService vocabularyService;
  private final ExportService exportService;

  @Autowired
  VocabularyResource(VocabularyService vocabularyService, ExportService exportService) {
    this.vocabularyService = vocabularyService;
    this.exportService = exportService;
  }

  @GetMapping
  public PagingResponse<Vocabulary> listVocabularies(
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "namespace", required = false) String namespace,
      @RequestParam(value = "deprecated", required = false) Boolean deprecated,
      PagingRequest page) {

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
  public Vocabulary get(@PathVariable("name") String vocabularyName) {
    return vocabularyService.getByName(vocabularyName);
  }

  @PostMapping
  public Vocabulary create(@RequestBody Vocabulary vocabulary) {
    int key = vocabularyService.create(vocabulary);
    return vocabularyService.get(key);
  }

  @PutMapping("{name}")
  public Vocabulary update(
      @PathVariable("name") String vocabularyName, @RequestBody Vocabulary vocabulary) {
    checkArgument(
        vocabularyName.equals(vocabulary.getName()),
        "Provided entity must have the same name as the resource in the URL");
    vocabularyService.update(vocabulary);
    return vocabularyService.get(vocabulary.getKey());
  }

  @GetMapping("suggest")
  public List<KeyNameResult> suggest(@RequestParam("q") String query) {
    return vocabularyService.suggest(query);
  }

  @PutMapping("{name}/deprecate")
  public void deprecate(
      @PathVariable("name") String vocabularyName,
      @RequestBody DeprecateVocabularyAction deprecateVocabularyAction) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    Objects.requireNonNull(vocabulary, "Vocabulary not found for name " + vocabularyName);

    vocabularyService.deprecate(
        vocabulary.getKey(),
        deprecateVocabularyAction.getDeprecatedBy(),
        deprecateVocabularyAction.getReplacementKey(),
        deprecateVocabularyAction.isDeprecateConcepts());
  }

  @DeleteMapping("{name}/deprecate")
  public void restoreDeprecated(
      @PathVariable("name") String vocabularyName,
      @RequestParam(value = "restoreDeprecatedConcepts", required = false)
          boolean restoreDeprecatedConcepts) {
    Vocabulary vocabulary = vocabularyService.getByName(vocabularyName);
    Objects.requireNonNull(vocabulary, "Vocabulary not found for name " + vocabularyName);

    vocabularyService.restoreDeprecated(vocabulary.getKey(), restoreDeprecatedConcepts);
  }

  @GetMapping(value = "{name}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Resource> downloadVocabulary(@PathVariable("name") String vocabularyName)
      throws IOException {
    Path exportPath = exportService.exportVocabulary(vocabularyName);
    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(exportPath));

    return ResponseEntity.ok()
        .header(
            "Content-Disposition", "attachment; filename=" + exportPath.getFileName().toString())
        .body(resource);
  }
}
