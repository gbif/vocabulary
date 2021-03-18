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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests the {@link KeyNameResult}. */
public class KeyNameResultTest {

  @Test
  public void equalityTest() {
    KeyNameResult knr1 = new KeyNameResult();
    knr1.setName("a");
    knr1.setKey(1);

    KeyNameResult knr2 = new KeyNameResult();
    knr2.setName(knr1.getName());

    Assertions.assertNotEquals(knr1, knr2);

    knr1.setKey(knr2.getKey());
    Assertions.assertEquals(knr1, knr2);
  }
}
