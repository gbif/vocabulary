package org.gbif.vocabulary.model;

import java.time.LocalDateTime;

public interface LabelEntity extends ValueEntity {

  LocalDateTime getCreated();

  void setCreated(LocalDateTime created);

  String getCreatedBy();

  void setCreatedBy(String createdBy);

}
