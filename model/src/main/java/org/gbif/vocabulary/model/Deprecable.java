package org.gbif.vocabulary.model;

import java.time.LocalDateTime;

/** Defines the minimum fields an entity must have to be deprecable. */
public interface Deprecable {

  /** Key of the entity that replaces a deprecated entity. */
  Integer getReplacedByKey();

  void setReplacedByKey(Integer replacedByKey);

  LocalDateTime getDeprecated();

  void setDeprecated(LocalDateTime deprecated);

  String getDeprecatedBy();

  void setDeprecatedBy(String deprecatedBy);
}
