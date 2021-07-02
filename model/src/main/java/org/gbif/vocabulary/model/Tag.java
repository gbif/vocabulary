package org.gbif.vocabulary.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import org.gbif.vocabulary.model.utils.LenientEquals;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Data;

@Data
public class Tag implements Serializable, LenientEquals<Tag> {

  private Integer key;
  @NotBlank private String name;
  private String description;

  @Pattern(regexp = "^#[A-Z0-9]{6}$")
  private String color;

  private LocalDateTime created;
  private String createdBy;
  private LocalDateTime modified;
  private String modifiedBy;

  @Override
  public boolean lenientEquals(Tag other) {
    if (this == other) {
      return true;
    } else {
      return Objects.equals(this.name, other.name)
          && Objects.equals(this.color, other.color)
          && Objects.equals(this.description, other.description);
    }
  }
}
