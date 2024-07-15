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
package org.gbif.vocabulary.model.export;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.LanguageRegion;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import lombok.Data;

@Data
public class ConceptExportView {

  @JsonUnwrapped private Concept concept;

  private Map<LanguageRegion, String> definition = new EnumMap<>(LanguageRegion.class);
  private Map<LanguageRegion, String> label = new EnumMap<>(LanguageRegion.class);
  private Map<LanguageRegion, Set<String>> alternativeLabels = new EnumMap<>(LanguageRegion.class);
  private Set<String> hiddenLabels = new HashSet<>();
  private Set<String> tags = new HashSet<>();
}
