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

/** Tests {@link Vocabulary}. */
public class VocabularyTest {

  @Test
  public void equalityTest() {
    Vocabulary v1 = new Vocabulary();
    v1.setKey(1L);
    v1.setName("v1");
    v1.setNamespace("ns");
    v1.setDefinition(
        Collections.singletonList(
            Definition.builder().language(LanguageRegion.ENGLISH).value("def").build()));
    v1.setEditorialNotes(Arrays.asList("n1", "n2"));
    v1.setExternalDefinitions(Collections.singletonList(URI.create("http://test.com")));
    v1.setCreated(ZonedDateTime.now());
    v1.setDeprecated(ZonedDateTime.now());

    Vocabulary v2 = new Vocabulary();
    v2.setKey(v1.getKey());
    v2.setName(v1.getName());
    v2.setNamespace(v1.getNamespace());
    v2.setDefinition(v1.getDefinition());
    v2.setEditorialNotes(v1.getEditorialNotes());
    v2.setExternalDefinitions(v1.getExternalDefinitions());
    v2.setCreated(v1.getCreated());
    v2.setDeprecated(v1.getDeprecated());

    assertTrue(v1.lenientEquals(v2));

    v1.setModified(ZonedDateTime.now());
    assertTrue(v1.lenientEquals(v2));
    assertNotEquals(v1, v2);
  }
}
