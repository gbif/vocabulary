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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PathwayLookupTest {

  private static final String PATHWAY_VOCAB_FILE = "Pathway.json";
  private static final InMemoryVocabularyLookup LOOKUP =
      InMemoryVocabularyLookup.newBuilder()
          .from(
              Thread.currentThread()
                  .getContextClassLoader()
                  .getResourceAsStream(PATHWAY_VOCAB_FILE))
          .build();

  @Test
  public void adultTest() {
    LookupConcept lookupConcept = LOOKUP.lookup("natural dispersal").get();
    assertEquals(2, lookupConcept.getParents().size());
    assertEquals("unaided", lookupConcept.getParents().get(0).getName());
    assertEquals("corridorAndDispersal", lookupConcept.getParents().get(1).getName());
  }
}
