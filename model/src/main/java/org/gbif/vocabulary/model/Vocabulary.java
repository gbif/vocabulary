package org.gbif.vocabulary.model;

import org.gbif.api.model.registry.LenientEquals;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Models a Vocabulary.
 *
 * <p>A vocabulary should be identified by its name, which should be unique.
 */
public class Vocabulary extends AbstractVocabularyEntity implements LenientEquals<Vocabulary> {

  private String namespace;

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
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
        && Objects.equals(replacedByKey, that.replacedByKey)
        && Objects.equals(deprecated, that.deprecated)
        && Objects.equals(deprecatedBy, that.deprecatedBy)
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
        replacedByKey,
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
    return new StringJoiner(", ", Vocabulary.class.getSimpleName() + "[", "]")
        .add("key=" + key)
        .add("namespace='" + namespace + "'")
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
  public boolean lenientEquals(Vocabulary other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    return Objects.equals(key, other.key)
        && Objects.equals(namespace, other.namespace)
        && Objects.equals(name, other.name)
        && Objects.equals(label, other.label)
        && Objects.equals(definition, other.definition)
        && Objects.equals(externalDefinitions, other.externalDefinitions)
        && Objects.equals(editorialNotes, other.editorialNotes)
        && Objects.equals(replacedByKey, other.replacedByKey)
        && Objects.equals(deprecated, other.deprecated)
        && Objects.equals(deprecatedBy, other.deprecatedBy)
        && Objects.equals(deleted, other.deleted);
  }
}
