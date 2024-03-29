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

import java.io.Serializable;

import lombok.Builder;
import lombok.Getter;

/** Holder for the vocabulary search parameters. */
@Getter
@Builder
public class VocabularySearchParams implements Serializable {

  private final String query;
  private final String name;
  private final String namespace;
  private final Boolean deprecated;
  private final Long key;
  private final Boolean hasUnreleasedChanges;

  public static VocabularySearchParams empty() {
    return builder().build();
  }
}
