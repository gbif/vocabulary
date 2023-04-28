package org.gbif.vocabulary.model.exception;

import lombok.Getter;

public class EntityNotFoundException extends RuntimeException {

  @Getter private final EntityType entityType;

  public EntityNotFoundException(EntityType entityType, String message) {
    super(message);
    this.entityType = entityType;
  }

  public enum EntityType {
    VOCABULARY,
    CONCEPT,
    RELEASE;
  }
}
