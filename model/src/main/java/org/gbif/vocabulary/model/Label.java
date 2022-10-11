package org.gbif.vocabulary.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Label {

  private Long key;
  private Long entityKey;
  private LanguageRegion language;
  private String label;
}
