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

import org.gbif.vocabulary.model.utils.LenientEquals;

import java.time.LocalDateTime;
import java.util.Objects;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class Label implements LabelEntity, LenientEquals<Label> {

  private Long key;
  private LanguageRegion language;
  private String value;
  private String createdBy;
  private LocalDateTime created;

  @Override
  public boolean lenientEquals(Label other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    return Objects.equals(key, other.key)
        && Objects.equals(language, other.language)
        && Objects.equals(value, other.value);
  }
}
