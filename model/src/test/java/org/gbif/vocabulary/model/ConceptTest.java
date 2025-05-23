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

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests {@link Concept}. */
public class ConceptTest {

  @Test
  public void equalityTest() {
    Concept c1 = new Concept();
    c1.setKey(1L);
    c1.setName("n1");
    c1.setVocabularyKey(1L);
    c1.setParentKey(2L);
    c1.setReplacedByKey(1L);
    c1.setDefinition(
        Collections.singletonList(
            Definition.builder().language(LanguageRegion.ENGLISH).value("def").build()));
    c1.setSameAsUris(Collections.singletonList(URI.create("http://test.com")));
    c1.setEditorialNotes(Arrays.asList("n1", "n2"));
    c1.setExternalDefinitions(Collections.singletonList(URI.create("http://test.com")));
    c1.setCreated(ZonedDateTime.now());
    c1.setDeprecated(ZonedDateTime.now());

    Concept c2 = new Concept();
    c2.setKey(c1.getKey());
    c2.setName(c1.getName());
    c2.setVocabularyKey(c1.getVocabularyKey());
    c2.setParentKey(c1.getParentKey());
    c2.setReplacedByKey(c1.getReplacedByKey());
    c2.setDefinition(c1.getDefinition());
    c2.setSameAsUris(c1.getSameAsUris());
    c2.setEditorialNotes(c1.getEditorialNotes());
    c2.setExternalDefinitions(c1.getExternalDefinitions());
    c2.setCreated(c1.getCreated());
    c2.setDeprecated(c1.getDeprecated());

    assertTrue(c1.lenientEquals(c2));

    c1.setModified(ZonedDateTime.now());
    assertTrue(c1.lenientEquals(c2));
    assertNotEquals(c1, c2);
  }
}
