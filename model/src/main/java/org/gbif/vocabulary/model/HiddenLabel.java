package org.gbif.vocabulary.model;

import java.time.LocalDateTime;
import java.util.Objects;

import org.gbif.vocabulary.model.utils.LenientEquals;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class HiddenLabel implements LabelEntity, LenientEquals<HiddenLabel> {

  private Long key;
  private String value;
  private String createdBy;
  private LocalDateTime created;

  @Override
  public boolean lenientEquals(HiddenLabel other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    return Objects.equals(key, other.key) && Objects.equals(value, other.value);
  }
}
