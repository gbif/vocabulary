package org.gbif.vocabulary.model;

import java.time.LocalDateTime;

/** Defines the minimn fields an entity must have to be deprecable. */
public interface Deprecable {

  Integer getReplacedByKey();

  void setReplacedByKey(Integer replacedByKey);

  LocalDateTime getDeprecated();

  void setDeprecated(LocalDateTime deprecated);

  String getDeprecatedBy();

  void setDeprecatedBy(String deprecatedBy);
}
