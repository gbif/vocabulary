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

import java.util.Map;

import lombok.Data;

/**
 * Utility container to hold the key and name of a {@link
 * org.gbif.vocabulary.model.VocabularyEntity}.
 */
@Data
public class KeyNameResult {
  private long key;
  private String name;
  private Map<LanguageRegion, String> label;
}
