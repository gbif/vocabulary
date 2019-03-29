package org.gbif.vocabulary.model;

import java.time.LocalDateTime;
import javax.validation.constraints.NotNull;

/** Defines the minimum fields that an entity must have in order to be auditable. */
public interface Auditable {

  LocalDateTime getCreated();

  void setCreated(LocalDateTime created);

  String getCreatedBy();

  void setCreatedBy(String createdBy);

  LocalDateTime getModified();

  void setModified(LocalDateTime modified);

  String getModifiedBy();

  void setModifiedBy(String modifiedBy);

  LocalDateTime getDeleted();

  void setDeleted(LocalDateTime deleted);
}
