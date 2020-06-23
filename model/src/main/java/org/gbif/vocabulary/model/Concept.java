package org.gbif.vocabulary.model;

import org.gbif.api.model.registry.LenientEquals;
import org.gbif.vocabulary.model.enums.LanguageRegion;

import java.net.URI;
import java.util.*;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Models a concept in a {@link Vocabulary}.
 *
 * <p>A concept must be linked to a {@link Vocabulary} and supports nesting in concepts. A concept
 * is identified by its name, which is unique.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class Concept extends AbstractVocabularyEntity implements LenientEquals<Concept> {

  /** Vocabulary of the concept. */
  @NotNull private Long vocabularyKey;

  /** Concept parent in case it exists. */
  private Long parentKey;

  /** Indicates alternative labels commonly associated to the concept. */
  private Map<LanguageRegion, List<String>> alternativeLabels = new EnumMap<>(LanguageRegion.class);

  /** Indicates hidden labels commonly associated to the concept. */
  private List<String> hiddenLabels = new ArrayList<>();

  /** External URIs for concepts considered equivalent. */
  private List<URI> sameAsUris = new ArrayList<>();

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
        && Objects.equals(hiddenLabels, other.hiddenLabels)
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
