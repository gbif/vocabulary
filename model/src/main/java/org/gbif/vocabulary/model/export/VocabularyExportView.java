package org.gbif.vocabulary.model.export;

import java.util.EnumMap;
import java.util.Map;

import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Vocabulary;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;

@Data
public class VocabularyExportView {

  @JsonUnwrapped private Vocabulary vocabulary;
  private Map<LanguageRegion, String> definition = new EnumMap<>(LanguageRegion.class);
  private Map<LanguageRegion, String> label = new EnumMap<>(LanguageRegion.class);
}
