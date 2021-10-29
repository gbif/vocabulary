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

import org.gbif.vocabulary.model.utils.PostPersist;
import org.gbif.vocabulary.model.utils.PrePersist;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

/** Defines the minimum fields that an vocabulary entity must have. */
public interface VocabularyEntity extends Auditable, Deprecable, Serializable {

  /** Unique identifier for persistence. */
  @Null(groups = {PrePersist.class})
  @NotNull(groups = {PostPersist.class})
  Long getKey();

  void setKey(Long key);

  /** Unique name. */
  @NotBlank
  String getName();

  void setName(String name);

  /** Representative label per language. */
  Map<LanguageRegion, String> getLabel();

  void setLabel(Map<LanguageRegion, String> label);

  /** Definition of the entity by language. */
  Map<LanguageRegion, String> getDefinition();

  void setDefinition(Map<LanguageRegion, String> definition);

  /** Additional external definitions. */
  List<URI> getExternalDefinitions();

  void setExternalDefinitions(List<URI> externalDefinitions);

  /** Notes for editorial purposes. */
  List<String> getEditorialNotes();

  void setEditorialNotes(List<String> editorialNotes);
}
