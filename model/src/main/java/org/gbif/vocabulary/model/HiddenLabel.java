package org.gbif.vocabulary.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HiddenLabel {

  private Long key;
  private Long entityKey;
  private String label;

}
