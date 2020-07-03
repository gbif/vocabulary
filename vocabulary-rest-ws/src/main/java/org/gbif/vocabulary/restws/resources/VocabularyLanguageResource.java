package org.gbif.vocabulary.restws.resources;

import org.gbif.vocabulary.model.enums.LanguageRegion;
import org.gbif.vocabulary.model.enums.LanguageRegion.LanguageRegionAllFieldsSerializer;
import org.gbif.vocabulary.restws.utils.Constants;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@RestController
@RequestMapping(Constants.VOCABULARY_LANGUAGE_PATH)
public class VocabularyLanguageResource {

  private static final SimpleModule LANGUAGE_SERIALIZER_MODULE =
      new SimpleModule()
          .addSerializer(LanguageRegion.class, new LanguageRegionAllFieldsSerializer());
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .disable(MapperFeature.USE_ANNOTATIONS)
          .registerModule(LANGUAGE_SERIALIZER_MODULE);

  @GetMapping
  public String listLanguageRegions() throws JsonProcessingException {
    return OBJECT_MAPPER.writeValueAsString(LanguageRegion.values());
  }
}
