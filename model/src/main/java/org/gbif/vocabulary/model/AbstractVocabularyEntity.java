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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/**
 * Base class for {@link VocabularyEntity} to provide a default implementation of {@link
 * VocabularyEntity}.
 */
@Data
public abstract class AbstractVocabularyEntity implements VocabularyEntity {

  Long key;
  @NotBlank String name;
  List<URI> externalDefinitions = new ArrayList<>();
  List<String> editorialNotes = new ArrayList<>();

  // deprecation fields
  Long replacedByKey;
  ZonedDateTime deprecated;
  String deprecatedBy;

  // audit fields
  ZonedDateTime created;
  String createdBy;
  ZonedDateTime modified;
  String modifiedBy;
}
