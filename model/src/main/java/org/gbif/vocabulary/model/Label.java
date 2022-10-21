package org.gbif.vocabulary.model;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class Label implements LabelEntity {

  private Long key;
  private Long entityKey;
  private LanguageRegion language;
  private String value;
}
