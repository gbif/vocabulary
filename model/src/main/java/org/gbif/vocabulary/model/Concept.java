package org.gbif.vocabulary.model;

import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.vocabulary.TranslationLanguage;

import java.net.URI;
import java.util.*;
import javax.validation.constraints.NotNull;

/**
 * Models a concept in a {@link Vocabulary}.
 *
 * <p>A concept must be linked to a {@link Vocabulary} and supports nesting in concepts. A concept
 * is identified by its name, which is unique.
 */
public class Concept extends AbstractVocabularyEntity implements LenientEquals<Concept> {

  @NotNull private Long vocabularyKey;
  private Long parentKey;
  private Map<TranslationLanguage, List<String>> alternativeLabels =
      new EnumMap<>(TranslationLanguage.class);
  private Map<TranslationLanguage, List<String>> misappliedLabels =
      new EnumMap<>(TranslationLanguage.class);
  private List<URI> sameAsUris = new ArrayList<>();

  /** Vocabulary of the concept. */
  public Long getVocabularyKey() {
    return vocabularyKey;
  }

  public void setVocabularyKey(Long vocabularyKey) {
    this.vocabularyKey = vocabularyKey;
  }

  /** Concept parent in case it exists. */
  public Long getParentKey() {
    return parentKey;
  }

  public void setParentKey(Long parentKey) {
    this.parentKey = parentKey;
  }

  /** Indicates alternative labels commonly associated to the concept. */
  public Map<TranslationLanguage, List<String>> getAlternativeLabels() {
    return alternativeLabels;
  }

  public void setAlternativeLabels(Map<TranslationLanguage, List<String>> alternativeLabels) {
    this.alternativeLabels = alternativeLabels;
  }

  /** Indicates misapplied labels commonly associated to the concept. */
  public Map<TranslationLanguage, List<String>> getMisappliedLabels() {
    return misappliedLabels;
  }

  public void setMisappliedLabels(Map<TranslationLanguage, List<String>> misappliedLabels) {
    this.misappliedLabels = misappliedLabels;
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
        && Objects.equals(misappliedLabels, concept.misappliedLabels)
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
        misappliedLabels,
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
        .add("vocabularyKey=" + vocabularyKey)
        .add("parentKey=" + parentKey)
        .add("alternativeLabels=" + alternativeLabels)
        .add("misappliedLabels=" + misappliedLabels)
        .add("sameAsUris=" + sameAsUris)
        .add("key=" + key)
        .add("name='" + name + "'")
        .add("label=" + label)
        .add("definition=" + definition)
        .add("externalDefinitions=" + externalDefinitions)
        .add("editorialNotes=" + editorialNotes)
        .add("replacedByKey=" + replacedByKey)
        .add("deprecated=" + deprecated)
        .add("deprecatedBy='" + deprecatedBy + "'")
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
        && Objects.equals(misappliedLabels, other.misappliedLabels)
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
