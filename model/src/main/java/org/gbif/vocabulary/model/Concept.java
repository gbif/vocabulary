package org.gbif.vocabulary.model;

import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.vocabulary.Language;

import java.net.URI;
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
 * is identified by its name, which is unique.
 */
public class Concept extends AbstractVocabularyEntity implements LenientEquals<Concept> {

  @NotNull private Integer vocabularyKey;
  private Integer parentKey;
  private Map<Language, List<String>> alternativeLabels = new EnumMap<>(Language.class);
  private Map<Language, List<String>> misspeltLabels = new EnumMap<>(Language.class);
  private List<URI> sameAsUris = new ArrayList<>();

  /** Vocabulary of the concept. */
  public Integer getVocabularyKey() {
    return vocabularyKey;
  }

  public void setVocabularyKey(Integer vocabularyKey) {
    this.vocabularyKey = vocabularyKey;
  }

  /** Concept parent in case it exists. */
  public Integer getParentKey() {
    return parentKey;
  }

  public void setParentKey(Integer parentKey) {
    this.parentKey = parentKey;
  }

  /** Indicates alternative labels commonly associated to the concept. */
  public Map<Language, List<String>> getAlternativeLabels() {
    return alternativeLabels;
  }

  public void setAlternativeLabels(Map<Language, List<String>> alternativeLabels) {
    this.alternativeLabels = alternativeLabels;
  }

  /** Indicates misspelt labels commonly associated to the concept. */
  public Map<Language, List<String>> getMisspeltLabels() {
    return misspeltLabels;
  }

  public void setMisspeltLabels(Map<Language, List<String>> misspeltLabels) {
    this.misspeltLabels = misspeltLabels;
  }

  /** External URIs for concepts considered equivalent. */
  public List<URI> getSameAsUris() {
    return sameAsUris;
  }

  public void setSameAsUris(List<URI> sameAsUris) {
    this.sameAsUris = sameAsUris;
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
        && Objects.equals(deprecated, concept.deprecated)
        && Objects.equals(deprecatedBy, concept.deprecatedBy)
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
        deprecated,
        deprecatedBy,
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
        .add("deprecated=" + deprecated)
        .add("deprecatedBy=" + deprecatedBy)
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
        && Objects.equals(name, other.name)
        && Objects.equals(label, other.label)
        && Objects.equals(alternativeLabels, other.alternativeLabels)
        && Objects.equals(misspeltLabels, other.misspeltLabels)
        && Objects.equals(definition, other.definition)
        && Objects.equals(externalDefinitions, other.externalDefinitions)
        && Objects.equals(sameAsUris, other.sameAsUris)
        && Objects.equals(editorialNotes, other.editorialNotes)
        && Objects.equals(replacedByKey, other.replacedByKey)
        && Objects.equals(deprecated, other.deprecated)
        && Objects.equals(deprecatedBy, other.deprecatedBy)
        && Objects.equals(deleted, other.deleted);
  }
}
