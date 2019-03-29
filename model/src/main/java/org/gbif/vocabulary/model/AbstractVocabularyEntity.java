package org.gbif.vocabulary.model;

import java.time.LocalDateTime;

/**
 * Base class for {@link VocabularyEntity} to provide a default implementation of {@link
 * VocabularyEntity}.
 */
public abstract class AbstractVocabularyEntity implements VocabularyEntity {

  // deprecation fields
  Integer replacedByKey;
  LocalDateTime deprecated;
  String deprecatedBy;

  // audit fields
  LocalDateTime created;
  String createdBy;
  LocalDateTime modified;
  String modifiedBy;
  LocalDateTime deleted;

  @Override
  public Integer getReplacedByKey() {
    return replacedByKey;
  }

  @Override
  public void setReplacedByKey(Integer replacedByKey) {
    this.replacedByKey = replacedByKey;
  }

  @Override
  public LocalDateTime getDeprecated() {
    return deprecated;
  }

  @Override
  public void setDeprecated(LocalDateTime deprecated) {
    this.deprecated = deprecated;
  }

  @Override
  public String getDeprecatedBy() {
    return deprecatedBy;
  }

  @Override
  public void setDeprecatedBy(String deprecatedBy) {
    this.deprecatedBy = deprecatedBy;
  }

  @Override
  public LocalDateTime getCreated() {
    return created;
  }

  @Override
  public void setCreated(LocalDateTime created) {
    this.created = created;
  }

  @Override
  public String getCreatedBy() {
    return createdBy;
  }

  @Override
  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  @Override
  public LocalDateTime getModified() {
    return modified;
  }

  @Override
  public void setModified(LocalDateTime modified) {
    this.modified = modified;
  }

  @Override
  public String getModifiedBy() {
    return modifiedBy;
  }

  @Override
  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  @Override
  public LocalDateTime getDeleted() {
    return deleted;
  }

  @Override
  public void setDeleted(LocalDateTime deleted) {
    this.deleted = deleted;
  }
}
