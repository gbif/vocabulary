package org.gbif.vocabulary.model;

import java.time.LocalDateTime;
import java.util.Objects;

import org.gbif.api.model.registry.LenientEquals;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VocabularyRelease implements LenientEquals<VocabularyRelease> {

  private Long key;
  private long vocabularyKey;
  @NotBlank private String version;
  private String exportUrl;
  LocalDateTime created;
  String createdBy;

  @Override
  public boolean lenientEquals(VocabularyRelease other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    return Objects.equals(key, other.key)
        && Objects.equals(vocabularyKey, other.vocabularyKey)
        && Objects.equals(version, other.version)
        && Objects.equals(exportUrl, other.exportUrl);
  }
}
