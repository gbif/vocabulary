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
package org.gbif.vocabulary.model.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests the {@link ConceptSearchParams}. */
public class ConceptSearchParamsTest {

  @Test
  public void builderTest() {
    String name = "v1";
    boolean deprecated = true;
    String query = "foo";
    Long key = 1L;

    ConceptSearchParams params =
        ConceptSearchParams.builder()
            .name(name)
            .deprecated(deprecated)
            .parentKey(key)
            .replacedByKey(key)
            .vocabularyKey(key)
            .query(query)
            .build();

    assertEquals(name, params.getName());
    assertEquals(deprecated, params.getDeprecated());
    assertEquals(key, params.getParentKey());
    assertEquals(key, params.getReplacedByKey());
    assertEquals(key, params.getVocabularyKey());
    assertEquals(query, params.getQuery());
  }
}
