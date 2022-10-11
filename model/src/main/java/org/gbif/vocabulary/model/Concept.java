/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

  /** Concept parent key in case it exists. */
  private Long parentKey;

  /** Indicates alternative labels commonly associated to the concept. */
  private List<Label> alternativeLabels = new ArrayList<>();

  /** Indicates hidden labels commonly associated to the concept. */
  private List<HiddenLabel> hiddenLabels = new ArrayList<>();

  /** External URIs for concepts considered equivalent. */
  private List<URI> sameAsUris = new ArrayList<>();

  /** Concept tags. */
  private List<Tag> tags = new ArrayList<>();

  @Override
  public boolean lenientEquals(Concept other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    return Objects.equals(key, other.key)
        && Objects.equals(vocabularyKey, other.vocabularyKey)
        && Objects.equals(parentKey, other.parentKey)
        && Objects.equals(name, other.name)
        && Objects.equals(labels, other.labels)
        && Objects.equals(alternativeLabels, other.alternativeLabels)
        && Objects.equals(hiddenLabels, other.hiddenLabels)
        && Objects.equals(definitions, other.definitions)
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
