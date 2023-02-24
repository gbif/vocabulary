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
package org.gbif.vocabulary.persistence.parameters;

import org.gbif.vocabulary.model.LanguageRegion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Holder for normalized values in a specific json node. This class is used to look for entities
 * with duplicate names or labels.
 */
@Getter
@Setter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NormalizedValuesParam {

  private String normalizedValue;
  private LanguageRegion language;

  public static NormalizedValuesParam from(String normalizedValue, LanguageRegion language) {
    return new NormalizedValuesParam(normalizedValue, language);
  }

  public static NormalizedValuesParam from(String normalizedValue) {
    return new NormalizedValuesParam(normalizedValue, null);
  }
}
