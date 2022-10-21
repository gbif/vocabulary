package org.gbif.vocabulary.model.export;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.LanguageRegion;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;

@Data
public class ConceptExportView {

  @JsonUnwrapped private Concept concept;

  private Map<LanguageRegion, String> label = new HashMap<>();
  private Map<LanguageRegion, Set<String>> alternativeLabels = new HashMap<>();
  private Set<String> hiddenLabels = new HashSet<>();
}
