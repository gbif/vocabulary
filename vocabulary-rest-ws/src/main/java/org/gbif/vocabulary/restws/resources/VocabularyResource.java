package org.gbif.vocabulary.restws.resources;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.VocabularyRelease;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.restws.model.DeprecateVocabularyAction;
import org.gbif.vocabulary.restws.model.VocabularyReleaseParams;
import org.gbif.vocabulary.service.ExportService;
import org.gbif.vocabulary.service.VocabularyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARIES_PATH;
import static org.gbif.vocabulary.restws.utils.Constants.VOCABULARY_RELEASES_PATH;

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
      @RequestParam(value = "key", required = false) Long key,
      PagingRequest page) {

    return vocabularyService.list(
        VocabularySearchParams.builder()
            .query(query)
            .name(name)
            .namespace(namespace)
            .deprecated(deprecated)
            .key(key)
            .build(),
        page);
  }

  @GetMapping("{name}")
  public Vocabulary get(@PathVariable("name") String vocabularyName) {
    return vocabularyService.getByName(vocabularyName);
  }

  @PostMapping
  public Vocabulary create(@RequestBody Vocabulary vocabulary) {
    long key = vocabularyService.create(vocabulary);
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
    checkArgument(vocabulary != null, "Vocabulary not found for name " + vocabularyName);

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
    checkArgument(vocabulary != null, "Vocabulary not found for name " + vocabularyName);

    vocabularyService.restoreDeprecated(vocabulary.getKey(), restoreDeprecatedConcepts);
  }

  @GetMapping(value = "{name}/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Resource> downloadVocabulary(@PathVariable("name") String vocabularyName)
      throws IOException {
    Path exportPath = exportService.exportVocabulary(vocabularyName);
    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(exportPath));

    return ResponseEntity.ok()
        .header(
            "Content-Disposition", "attachment; filename=" + exportPath.getFileName().toString())
        .body(resource);
  }

  @PostMapping(value = "{name}/" + VOCABULARY_RELEASES_PATH)
  public ResponseEntity<VocabularyRelease> releaseVocabularyVersion(
      @PathVariable("name") String vocabularyName,
      @RequestBody() VocabularyReleaseParams params,
      HttpServletRequest httpServletRequest) {

    // export the vocabulary first
    Path vocabularyExport = exportService.exportVocabulary(vocabularyName);

    // release it and return
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    VocabularyRelease release =
        exportService.releaseVocabulary(
            vocabularyName, params.getVersion(), vocabularyExport, authentication.getName());

    return ResponseEntity.created(
            URI.create(httpServletRequest.getRequestURL() + "/" + release.getVersion()))
        .body(release);
  }

  @GetMapping(value = "{name}/" + VOCABULARY_RELEASES_PATH)
  public PagingResponse<VocabularyRelease> listReleases(
      @PathVariable("name") String vocabularyName,
      @RequestParam(value = "version", required = false) String version,
      PagingRequest page) {
    return exportService.listReleases(vocabularyName, version, page);
  }

  @GetMapping(value = "{name}/" + VOCABULARY_RELEASES_PATH + "/{version}")
  public VocabularyRelease getRelease(
      @PathVariable("name") String vocabularyName, @PathVariable("version") String version) {
    PagingRequest page = new PagingRequest(0, 1);
    PagingResponse<VocabularyRelease> releases =
        exportService.listReleases(vocabularyName, version, page);
    return releases.getResults().isEmpty() ? null : releases.getResults().get(0);
  }
}
