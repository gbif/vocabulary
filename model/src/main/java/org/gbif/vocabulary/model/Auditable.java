package org.gbif.vocabulary.model;

import java.time.LocalDateTime;

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
