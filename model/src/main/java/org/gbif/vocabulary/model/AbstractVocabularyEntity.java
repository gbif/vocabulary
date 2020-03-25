package org.gbif.vocabulary.model;

import org.gbif.vocabulary.model.vocabulary.LanguageRegion;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotBlank;

/**
 * Base class for {@link VocabularyEntity} to provide a default implementation of {@link
 * VocabularyEntity}.
 */
public abstract class AbstractVocabularyEntity implements VocabularyEntity {

  Long key;
  @NotBlank String name;
  Map<LanguageRegion, String> label = new EnumMap<>(LanguageRegion.class);
  Map<LanguageRegion, String> definition = new EnumMap<>(LanguageRegion.class);
  List<URI> externalDefinitions = new ArrayList<>();
  List<String> editorialNotes = new ArrayList<>();

  // deprecation fields
  Long replacedByKey;
  LocalDateTime deprecated;
  String deprecatedBy;

  // audit fields
  LocalDateTime created;
  String createdBy;
  LocalDateTime modified;
  String modifiedBy;
  LocalDateTime deleted;

  @Override
  public Long getKey() {
    return key;
  }

  @Override
  public void setKey(Long key) {
    this.key = key;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Map<LanguageRegion, String> getLabel() {
    return label;
  }

  @Override
  public void setLabel(Map<LanguageRegion, String> label) {
    this.label = label;
  }

  @Override
  public Map<LanguageRegion, String> getDefinition() {
    return definition;
  }

  @Override
  public void setDefinition(Map<LanguageRegion, String> definition) {
    this.definition = definition;
  }

  @Override
  public List<URI> getExternalDefinitions() {
    return externalDefinitions;
  }

  @Override
  public void setExternalDefinitions(List<URI> externalDefinitions) {
    this.externalDefinitions = externalDefinitions;
  }

  @Override
  public List<String> getEditorialNotes() {
    return editorialNotes;
  }

  @Override
  public void setEditorialNotes(List<String> editorialNotes) {
    this.editorialNotes = editorialNotes;
  }

  @Override
  public Long getReplacedByKey() {
    return replacedByKey;
  }

  @Override
  public void setReplacedByKey(Long replacedByKey) {
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
