/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import org.gbif.vocabulary.model.utils.LenientEquals;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class VocabularyRelease implements LenientEquals<VocabularyRelease> {

  private Long key;
  private long vocabularyKey;
  @NotBlank private String version;
  @NotBlank private String exportUrl;
  LocalDateTime created;
  @NotBlank String createdBy;
  @NotBlank String comment;

  @Override
  public boolean lenientEquals(VocabularyRelease other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    return Objects.equals(key, other.key)
        && Objects.equals(vocabularyKey, other.vocabularyKey)
        && Objects.equals(version, other.version)
        && Objects.equals(exportUrl, other.exportUrl)
        && Objects.equals(comment, other.comment);
  }
}
