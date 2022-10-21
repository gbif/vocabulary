package org.gbif.vocabulary.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.gbif.vocabulary.model.utils.LenientEquals;

import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class Concept extends AbstractVocabularyEntity implements LenientEquals<Concept> {

  /** Vocabulary of the concept. */
  @NotNull Long vocabularyKey;

  /** Concept parent key in case it exists. */
  Long parentKey;

  /** External URIs for concepts considered equivalent. */
  List<URI> sameAsUris = new ArrayList<>();

  /** Concept tags. */
  List<Tag> tags = new ArrayList<>();

  @Override
  public boolean lenientEquals(Concept other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    return Objects.equals(key, other.key)
        && Objects.equals(vocabularyKey, other.vocabularyKey)
        && Objects.equals(parentKey, other.parentKey)
        && Objects.equals(name, other.name)
        && Objects.equals(definition, other.definition)
        && Objects.equals(externalDefinitions, other.externalDefinitions)
        && Objects.equals(sameAsUris, other.sameAsUris)
        && Objects.equals(editorialNotes, other.editorialNotes)
        && Objects.equals(replacedByKey, other.replacedByKey)
        && Objects.equals(deprecated, other.deprecated)
        && Objects.equals(deprecatedBy, other.deprecatedBy)
        && Objects.equals(deleted, other.deleted)
        && Objects.equals(tags, other.tags);
  }
}
