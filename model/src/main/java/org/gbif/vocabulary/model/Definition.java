package org.gbif.vocabulary.model;

import java.time.LocalDateTime;
import java.util.Objects;

import org.gbif.vocabulary.model.utils.LenientEquals;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class Definition implements ValueEntity, Auditable, LenientEquals<Definition> {

  private Long key;
  private LanguageRegion language;
  private String value;
  private String createdBy;
  private LocalDateTime created;
  private String modifiedBy;
  private LocalDateTime modified;

  @Override
  public boolean lenientEquals(Definition other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    return Objects.equals(key, other.key)
        && Objects.equals(language, other.language)
        && Objects.equals(value, other.value);
  }
}
