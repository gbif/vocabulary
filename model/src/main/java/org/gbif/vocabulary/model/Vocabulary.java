package org.gbif.vocabulary.model;

import java.util.Objects;

import org.gbif.vocabulary.model.utils.LenientEquals;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class Vocabulary extends AbstractVocabularyEntity
    implements LenientEquals<Vocabulary> {

  /** Namespace for imported vocabularies. */
  String namespace;

  @Override
  public boolean lenientEquals(Vocabulary other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    return Objects.equals(key, other.key)
        && Objects.equals(namespace, other.namespace)
        && Objects.equals(name, other.name)
        && Objects.equals(definition, other.definition)
        && Objects.equals(externalDefinitions, other.externalDefinitions)
        && Objects.equals(editorialNotes, other.editorialNotes)
        && Objects.equals(replacedByKey, other.replacedByKey)
        && Objects.equals(deprecated, other.deprecated)
        && Objects.equals(deprecatedBy, other.deprecatedBy)
        && Objects.equals(deleted, other.deleted);
  }
}
