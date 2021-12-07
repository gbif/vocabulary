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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class LifeStageLookupTest {

  private static final String LIFE_STAGE_VOCAB_FILE = "LifeStage.json";
  private static final InMemoryVocabularyLookup LOOKUP =
      InMemoryVocabularyLookup.newBuilder()
          .from(
              Thread.currentThread()
                  .getContextClassLoader()
                  .getResourceAsStream(LIFE_STAGE_VOCAB_FILE))
          .withPrefilter(PreFilters.REMOVE_NUMERIC_PREFIX)
          .build();

  @Test
  public void adultTest() {
    assertEquals("Adult", LOOKUP.lookup("1 adult").get().getConcept().getName());
    assertEquals("Adult", LOOKUP.lookup("1 adult(s)").get().getConcept().getName());
    assertEquals("Adult", LOOKUP.lookup("1 ADult").get().getConcept().getName());
    assertEquals("Adult", LOOKUP.lookup("1 adulto").get().getConcept().getName());
    assertEquals("Adult", LOOKUP.lookup("10 adult").get().getConcept().getName());
    assertEquals("Adult", LOOKUP.lookup("A").get().getConcept().getName());
    assertEquals("Adult", LOOKUP.lookup("Ad").get().getConcept().getName());
    assertEquals("Adult", LOOKUP.lookup("adulta").get().getConcept().getName());
    assertEquals("Adult", LOOKUP.lookup("adults").get().getConcept().getName());
    assertEquals("Adult", LOOKUP.lookup("old").get().getConcept().getName());
    assertEquals("Adult", LOOKUP.lookup("old   adult").get().getConcept().getName());
    assertEquals("Adult", LOOKUP.lookup("teneral").get().getConcept().getName());
    assertEquals("Adult", LOOKUP.lookup("young adult").get().getConcept().getName());

    assertFalse(LOOKUP.lookup("adult?").isPresent());
    assertFalse(LOOKUP.lookup("Adult or juvenile").isPresent());
    assertFalse(LOOKUP.lookup("Adult/juvenile").isPresent());
    assertFalse(LOOKUP.lookup("1").isPresent());
    assertFalse(LOOKUP.lookup("1 adulto, 1 juvenil").isPresent());
    assertFalse(LOOKUP.lookup("Adults and Juveniles").isPresent());
  }

  @Test
  public void subadultTest() {
    assertEquals("Subadult", LOOKUP.lookup("subadult").get().getConcept().getName());
    assertEquals("Subadult", LOOKUP.lookup("subimago").get().getConcept().getName());
    assertEquals("Subadult", LOOKUP.lookup("sub-adult").get().getConcept().getName());
    assertEquals("Subadult", LOOKUP.lookup("subad").get().getConcept().getName());
    assertEquals("Subadult", LOOKUP.lookup("subadulto").get().getConcept().getName());
    assertEquals("Subadult", LOOKUP.lookup("sub-Adult").get().getConcept().getName());
    assertEquals("Subadult", LOOKUP.lookup("sub adult").get().getConcept().getName());

    assertFalse(LOOKUP.lookup("subadult?").isPresent());
  }

  @Test
  public void juvenileTest() {
    assertEquals("Juvenile", LOOKUP.lookup("young").get().getConcept().getName());
  }

  @Test
  public void imagoTest() {
    assertEquals("Imago", LOOKUP.lookup("imago").get().getConcept().getName());
    assertEquals("Imago", LOOKUP.lookup("fresh imago").get().getConcept().getName());
    assertEquals("Imago", LOOKUP.lookup("imago/adult").get().getConcept().getName());
    assertEquals("Imago", LOOKUP.lookup("imago:1").get().getConcept().getName());
  }

  @Test
  public void larvaTest() {
    assertEquals("Larva", LOOKUP.lookup("1 larva").get().getConcept().getName());
    assertEquals("Larva", LOOKUP.lookup("1st instar").get().getConcept().getName());
    assertEquals("Larva", LOOKUP.lookup("2 larva").get().getConcept().getName());
    assertEquals("Larva", LOOKUP.lookup("3 larvae").get().getConcept().getName());
    assertEquals("Larva", LOOKUP.lookup("larvae").get().getConcept().getName());
    assertEquals("Larva", LOOKUP.lookup("larvas").get().getConcept().getName());
    assertEquals("Larva", LOOKUP.lookup("larval").get().getConcept().getName());
    assertEquals("Larva", LOOKUP.lookup("metacercaria").get().getConcept().getName());
    assertEquals("Larva", LOOKUP.lookup("larve").get().getConcept().getName());

    assertEquals("Veliger", LOOKUP.lookup("veliger").get().getConcept().getName());

    assertEquals("Nymph", LOOKUP.lookup("larva, nymph").get().getConcept().getName());
    assertEquals("Nymph", LOOKUP.lookup("larva/nymph").get().getConcept().getName());

    assertEquals("Nauplius", LOOKUP.lookup("nauplii").get().getConcept().getName());

    assertEquals("Tadpole", LOOKUP.lookup("tadpole").get().getConcept().getName());
    assertEquals("Tadpole", LOOKUP.lookup("tadpoles").get().getConcept().getName());
    assertEquals("Tadpole", LOOKUP.lookup("têtard").get().getConcept().getName());
    assertEquals("Tadpole", LOOKUP.lookup("Renacuajo").get().getConcept().getName());
    assertEquals("Tadpole", LOOKUP.lookup("renacuajos").get().getConcept().getName());

    assertEquals("Cyprid", LOOKUP.lookup("cyprid").get().getConcept().getName());

    assertFalse(LOOKUP.lookup("larva; adult").isPresent());
    assertFalse(LOOKUP.lookup("adult, larva").isPresent());
    assertFalse(LOOKUP.lookup("adult;larva").isPresent());
    assertFalse(LOOKUP.lookup("cypris").isPresent());
  }

  @Test
  public void unknownTest() {
    assertEquals("Unknown", LOOKUP.lookup("undetermined").get().getConcept().getName());
    assertEquals("Unknown", LOOKUP.lookup("unknown").get().getConcept().getName());
    assertEquals("Unknown", LOOKUP.lookup("não informado").get().getConcept().getName());
    assertEquals("Unknown", LOOKUP.lookup("not recorded").get().getConcept().getName());
    assertEquals("Unknown", LOOKUP.lookup("indeterminado").get().getConcept().getName());
    assertEquals("Unknown", LOOKUP.lookup("No se cuenta con el dato").get().getConcept().getName());
    assertEquals("Unknown", LOOKUP.lookup("Life Stage Not Recorded").get().getConcept().getName());
    assertEquals("Unknown", LOOKUP.lookup("Indéterminé").get().getConcept().getName());
    assertEquals("Unknown", LOOKUP.lookup("  desconocido").get().getConcept().getName());
    assertEquals("Unknown", LOOKUP.lookup("1K").get().getConcept().getName());
    assertEquals("Unknown", LOOKUP.lookup("1k+").get().getConcept().getName());
    assertEquals("Unknown", LOOKUP.lookup("1st calendar year").get().getConcept().getName());

    assertFalse(LOOKUP.lookup("not applicable").isPresent());
    assertFalse(LOOKUP.lookup("No Aplica").isPresent());
    assertFalse(LOOKUP.lookup("Adults and Juveniles").isPresent());
    assertFalse(LOOKUP.lookup("19").isPresent());
  }

  @Test
  public void notFoundTest() {
    assertFalse(LOOKUP.lookup("x").isPresent());
    assertFalse(LOOKUP.lookup("yearling").isPresent());
    assertFalse(LOOKUP.lookup("young of year").isPresent());
    assertFalse(LOOKUP.lookup("young-of-the-year (YOY)/pelagic stage").isPresent());
    assertFalse(LOOKUP.lookup("http://vocab.nerc.ac.uk/collection/S11/current/S1127/").isPresent());
    assertFalse(LOOKUP.lookup("C").isPresent());
    assertFalse(LOOKUP.lookup("S").isPresent());
    assertFalse(LOOKUP.lookup("C1").isPresent());
    assertFalse(LOOKUP.lookup("CIII").isPresent());
    assertFalse(LOOKUP.lookup("At least 1st calendar year").isPresent());
    assertFalse(LOOKUP.lookup("age unknown").isPresent());
    assertFalse(LOOKUP.lookup("seed").isPresent());
  }
}
