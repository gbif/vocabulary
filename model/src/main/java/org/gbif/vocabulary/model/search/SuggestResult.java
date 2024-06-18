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
package org.gbif.vocabulary.model.search;

import org.gbif.vocabulary.model.LanguageRegion;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SuggestResult {
  private String name;
  private String label;
  private LanguageRegion labelLanguage;
  private List<Parent> parents = new ArrayList<>();

  @Data
  public static class Parent {
    private String name;
    private String label;
    private LanguageRegion labelLanguage;
  }
}
