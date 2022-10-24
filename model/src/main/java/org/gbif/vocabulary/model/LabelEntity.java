package org.gbif.vocabulary.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.gbif.vocabulary.model.utils.PostPersist;
import org.gbif.vocabulary.model.utils.PrePersist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

public interface LabelEntity extends Auditable, Serializable {

  @Null(groups = {PrePersist.class})
  @NotNull(groups = {PostPersist.class})
  Long getKey();

  void setKey(Long key);

  @NotNull
  Long getEntityKey();

  void setEntityKey(Long key);

  @NotBlank
  String getValue();

  void setValue(String value);

  @JsonIgnore
  default LocalDateTime getDeleted() {
    // not implemented
    return null;
  }

  default void setDeleted(LocalDateTime deleted) {
    // do nothing
  }
}
