package org.gbif.vocabulary.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class HiddenLabel implements LabelEntity {

  private Long key;
  private Long entityKey;
  private String value;
  private String createdBy;
  private LocalDateTime created;
  private String modifiedBy;
  private LocalDateTime modified;
}
