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
package org.gbif.vocabulary.lookup;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.export.ConceptExportView;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
public class LookupConcept {

  private Concept concept;
  private List<Parent> parents;
  private List<String> tags;

  @Data
  @AllArgsConstructor(staticName = "of")
  public static class Parent {
    private Long key;
    private Long parentKey;
    private String name;
    private List<String> tags;

    public static Parent from(ConceptExportView conceptExportView) {
      return Parent.of(
          conceptExportView.getConcept().getKey(),
          conceptExportView.getConcept().getParentKey(),
          conceptExportView.getConcept().getName(),
          new ArrayList<>(conceptExportView.getTags()));
    }
  }
}
