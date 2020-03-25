package org.gbif.vocabulary.restws.resources;

import org.gbif.vocabulary.model.vocabulary.LanguageRegion;
import org.gbif.vocabulary.restws.utils.Constants;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constants.VOCABULARY_LANGUAGE_PATH)
public class VocabularyLanguageResource {

  @GetMapping
  public LanguageRegion[] listLanguageRegions() {
    return LanguageRegion.values();
  }
}
