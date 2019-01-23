package org.gbif.vocabulary.model;

import org.gbif.api.vocabulary.Language;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class Vocabulary implements Auditable {

  // TODO: validations

  private Integer key;
  private String namespace;
  private String name;
  private Map<Language, String> label;
  private Map<Language, String> definition;
  private List<URI> externalDefinitions;
  private List<String> editorialNotes;

  // audit fields
  private LocalDateTime created;
  private String createdBy;
  private LocalDateTime modified;
  private String modifiedBy;
  private LocalDateTime deleted;

  public Integer getKey() {
    return key;
  }

  public void setKey(Integer key) {
    this.key = key;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<Language, String> getLabel() {
    return label;
  }

  public void setLabel(Map<Language, String> label) {
    this.label = label;
  }

  public Map<Language, String> getDefinition() {
    return definition;
  }

  public void setDefinition(Map<Language, String> definition) {
    this.definition = definition;
  }

  public List<URI> getExternalDefinitions() {
    return externalDefinitions;
  }

  public void setExternalDefinitions(List<URI> externalDefinitions) {
    this.externalDefinitions = externalDefinitions;
  }

  public List<String> getEditorialNotes() {
    return editorialNotes;
  }

  public void setEditorialNotes(List<String> editorialNotes) {
    this.editorialNotes = editorialNotes;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Vocabulary that = (Vocabulary) o;
    return Objects.equals(key, that.key)
        && Objects.equals(namespace, that.namespace)
        && Objects.equals(name, that.name)
        && Objects.equals(label, that.label)
        && Objects.equals(definition, that.definition)
        && Objects.equals(externalDefinitions, that.externalDefinitions)
        && Objects.equals(editorialNotes, that.editorialNotes)
        && Objects.equals(created, that.created)
        && Objects.equals(createdBy, that.createdBy)
        && Objects.equals(modified, that.modified)
        && Objects.equals(modifiedBy, that.modifiedBy)
        && Objects.equals(deleted, that.deleted);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        key,
        namespace,
        name,
        label,
        definition,
        externalDefinitions,
        editorialNotes,
        created,
        createdBy,
        modified,
        modifiedBy,
        deleted);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Vocabulary.class.getSimpleName() + "[", "]")
        .add("key=" + key)
        .add("namespace='" + namespace + "'")
        .add("name='" + name + "'")
        .add("label=" + label)
        .add("definition=" + definition)
        .add("externalDefinitions=" + externalDefinitions)
        .add("editorialNotes=" + editorialNotes)
        .add("created=" + created)
        .add("createdBy='" + createdBy + "'")
        .add("modified=" + modified)
        .add("modifiedBy='" + modifiedBy + "'")
        .add("deleted=" + deleted)
        .toString();
  }
}
