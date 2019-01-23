package org.gbif.vocabulary.model;

import org.gbif.api.vocabulary.Language;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class Concept implements Auditable {

  // TODO: validations

  private Integer key;
  private Integer vocabularyKey;
  private Integer parentKey;
  private Integer replacedByKey;
  private String name;
  private Map<Language, String> label;
  private Map<Language, String> alternativeLabel;
  private Map<Language, String> misspeltLabel;
  private Map<Language, String> definition;
  private List<URI> externalDefinitions;
  private List<URI> sameAsUris;
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

  public Integer getVocabularyKey() {
    return vocabularyKey;
  }

  public void setVocabularyKey(Integer vocabularyKey) {
    this.vocabularyKey = vocabularyKey;
  }

  public Integer getParentKey() {
    return parentKey;
  }

  public void setParentKey(Integer parentKey) {
    this.parentKey = parentKey;
  }

  public Integer getReplacedByKey() {
    return replacedByKey;
  }

  public void setReplacedByKey(Integer replacedByKey) {
    this.replacedByKey = replacedByKey;
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

  public Map<Language, String> getAlternativeLabel() {
    return alternativeLabel;
  }

  public void setAlternativeLabel(Map<Language, String> alternativeLabel) {
    this.alternativeLabel = alternativeLabel;
  }

  public Map<Language, String> getMisspeltLabel() {
    return misspeltLabel;
  }

  public void setMisspeltLabel(Map<Language, String> misspeltLabel) {
    this.misspeltLabel = misspeltLabel;
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

  public List<URI> getSameAsUris() {
    return sameAsUris;
  }

  public void setSameAsUris(List<URI> sameAsUris) {
    this.sameAsUris = sameAsUris;
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
    Concept concept = (Concept) o;
    return Objects.equals(key, concept.key)
        && Objects.equals(vocabularyKey, concept.vocabularyKey)
        && Objects.equals(parentKey, concept.parentKey)
        && Objects.equals(replacedByKey, concept.replacedByKey)
        && Objects.equals(name, concept.name)
        && Objects.equals(label, concept.label)
        && Objects.equals(alternativeLabel, concept.alternativeLabel)
        && Objects.equals(misspeltLabel, concept.misspeltLabel)
        && Objects.equals(definition, concept.definition)
        && Objects.equals(externalDefinitions, concept.externalDefinitions)
        && Objects.equals(sameAsUris, concept.sameAsUris)
        && Objects.equals(editorialNotes, concept.editorialNotes)
        && Objects.equals(created, concept.created)
        && Objects.equals(createdBy, concept.createdBy)
        && Objects.equals(modified, concept.modified)
        && Objects.equals(modifiedBy, concept.modifiedBy)
        && Objects.equals(deleted, concept.deleted);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        key,
        vocabularyKey,
        parentKey,
        replacedByKey,
        name,
        label,
        alternativeLabel,
        misspeltLabel,
        definition,
        externalDefinitions,
        sameAsUris,
        editorialNotes,
        created,
        createdBy,
        modified,
        modifiedBy,
        deleted);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Concept.class.getSimpleName() + "[", "]")
        .add("key=" + key)
        .add("vocabularyKey=" + vocabularyKey)
        .add("parentKey=" + parentKey)
        .add("replacedByKey=" + replacedByKey)
        .add("name='" + name + "'")
        .add("label=" + label)
        .add("alternativeLabel=" + alternativeLabel)
        .add("misspeltLabel=" + misspeltLabel)
        .add("definition=" + definition)
        .add("externalDefinitions=" + externalDefinitions)
        .add("sameAsUris=" + sameAsUris)
        .add("editorialNotes=" + editorialNotes)
        .add("created=" + created)
        .add("createdBy='" + createdBy + "'")
        .add("modified=" + modified)
        .add("modifiedBy='" + modifiedBy + "'")
        .add("deleted=" + deleted)
        .toString();
  }
}
