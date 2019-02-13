package org.gbif.vocabulary.model;

import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.vocabulary.Language;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import javax.validation.constraints.NotNull;

/**
 * Models a concept in a {@link Vocabulary}.
 *
 * <p>A concept must be linked to a {@link Vocabulary} and supports nesting in concepts. A concept
 * should be identified by its name, which should be unique.
 */
public class Concept implements VocabularyEntity, LenientEquals<Concept> {

  private Integer key;
  private Integer vocabularyKey;
  private Integer parentKey;
  private Integer replacedByKey;
  private String name;
  private Map<Language, String> label = new EnumMap<>(Language.class);
  private Map<Language, List<String>> alternativeLabels = new EnumMap<>(Language.class);
  private Map<Language, List<String>> misspeltLabels = new EnumMap<>(Language.class);
  private Map<Language, String> definition = new EnumMap<>(Language.class);
  private List<URI> externalDefinitions = new ArrayList<>();
  private List<URI> sameAsUris = new ArrayList<>();
  private List<String> editorialNotes = new ArrayList<>();

  // audit fields
  private LocalDateTime created;
  private String createdBy;
  private LocalDateTime modified;
  private String modifiedBy;
  private LocalDateTime deleted;

  @Override
  public Integer getKey() {
    return key;
  }

  @Override
  public void setKey(Integer key) {
    this.key = key;
  }

  @NotNull
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

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Map<Language, String> getLabel() {
    return label;
  }

  @Override
  public void setLabel(Map<Language, String> label) {
    this.label = label;
  }

  public Map<Language, List<String>> getAlternativeLabels() {
    return alternativeLabels;
  }

  public void setAlternativeLabels(Map<Language, List<String>> alternativeLabels) {
    this.alternativeLabels = alternativeLabels;
  }

  public Map<Language, List<String>> getMisspeltLabels() {
    return misspeltLabels;
  }

  public void setMisspeltLabels(Map<Language, List<String>> misspeltLabels) {
    this.misspeltLabels = misspeltLabels;
  }

  @Override
  public Map<Language, String> getDefinition() {
    return definition;
  }

  @Override
  public void setDefinition(Map<Language, String> definition) {
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

  public List<URI> getSameAsUris() {
    return sameAsUris;
  }

  public void setSameAsUris(List<URI> sameAsUris) {
    this.sameAsUris = sameAsUris;
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
        && Objects.equals(alternativeLabels, concept.alternativeLabels)
        && Objects.equals(misspeltLabels, concept.misspeltLabels)
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
        alternativeLabels,
        misspeltLabels,
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
        .add("alternativeLabel=" + alternativeLabels)
        .add("misspeltLabel=" + misspeltLabels)
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

  @Override
  public boolean lenientEquals(Concept other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    return Objects.equals(key, other.key)
        && Objects.equals(vocabularyKey, other.vocabularyKey)
        && Objects.equals(parentKey, other.parentKey)
        && Objects.equals(replacedByKey, other.replacedByKey)
        && Objects.equals(name, other.name)
        && Objects.equals(label, other.label)
        && Objects.equals(alternativeLabels, other.alternativeLabels)
        && Objects.equals(misspeltLabels, other.misspeltLabels)
        && Objects.equals(definition, other.definition)
        && Objects.equals(externalDefinitions, other.externalDefinitions)
        && Objects.equals(sameAsUris, other.sameAsUris)
        && Objects.equals(editorialNotes, other.editorialNotes)
        && Objects.equals(deleted, other.deleted);
  }
}
