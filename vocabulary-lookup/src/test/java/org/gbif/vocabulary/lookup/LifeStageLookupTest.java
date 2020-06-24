package org.gbif.vocabulary.lookup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class LifeStageLookupTest {

  private static final String LIFE_STAGE_VOCAB_FILE = "LifeStage.json";
  private static final VocabularyLookup LOOKUP =
      VocabularyLookup.load(
          Thread.currentThread()
              .getContextClassLoader()
              .getResourceAsStream(LIFE_STAGE_VOCAB_FILE));

  @Test
  public void adultTest() {
    assertEquals("Adult", LOOKUP.lookup("1 adult").get().getName());
    assertEquals("Adult", LOOKUP.lookup("1 adult(s)").get().getName());
    assertEquals("Adult", LOOKUP.lookup("1 ADult").get().getName());
    assertEquals("Adult", LOOKUP.lookup("1 adulto").get().getName());
    assertEquals("Adult", LOOKUP.lookup("10 adult").get().getName());
    assertEquals("Adult", LOOKUP.lookup("A").get().getName());
    assertEquals("Adult", LOOKUP.lookup("Ad").get().getName());
    assertEquals("Adult", LOOKUP.lookup("adulta").get().getName());
    assertEquals("Adult", LOOKUP.lookup("adults").get().getName());
    assertEquals("Adult", LOOKUP.lookup("old").get().getName());
    assertEquals("Adult", LOOKUP.lookup("old   adult").get().getName());
    assertEquals("Adult", LOOKUP.lookup("teneral").get().getName());
    assertEquals("Adult", LOOKUP.lookup("young adult").get().getName());

    assertFalse(LOOKUP.lookup("adult?").isPresent());
    assertFalse(LOOKUP.lookup("Adult or juvenile").isPresent());
    assertFalse(LOOKUP.lookup("Adult/juvenile").isPresent());
    assertFalse(LOOKUP.lookup("1").isPresent());
    assertFalse(LOOKUP.lookup("1 adulto, 1 juvenil").isPresent());
    assertFalse(LOOKUP.lookup("Adults and Juveniles").isPresent());
  }

  @Test
  public void subadultTest() {
    assertEquals("Subadult", LOOKUP.lookup("subadult").get().getName());
    assertEquals("Subadult", LOOKUP.lookup("subimago").get().getName());
    assertEquals("Subadult", LOOKUP.lookup("sub-adult").get().getName());
    assertEquals("Subadult", LOOKUP.lookup("subad").get().getName());
    assertEquals("Subadult", LOOKUP.lookup("subadulto").get().getName());
    assertEquals("Subadult", LOOKUP.lookup("sub-Adult").get().getName());
    assertEquals("Subadult", LOOKUP.lookup("sub adult").get().getName());

    assertFalse(LOOKUP.lookup("subadult?").isPresent());
  }

  @Test
  public void juvenileTest() {
    assertEquals("Juvenile", LOOKUP.lookup("young").get().getName());
  }

  @Test
  public void imagoTest() {
    assertEquals("Imago", LOOKUP.lookup("imago").get().getName());
    assertEquals("Imago", LOOKUP.lookup("fresh imago").get().getName());
    assertEquals("Imago", LOOKUP.lookup("imago/adult").get().getName());
    assertEquals("Imago", LOOKUP.lookup("imago:1").get().getName());
  }

  @Test
  public void larvaTest() {
    assertEquals("Larva", LOOKUP.lookup("1 larva").get().getName());
    assertEquals("Larva", LOOKUP.lookup("1st instar").get().getName());
    assertEquals("Larva", LOOKUP.lookup("2 larva").get().getName());
    assertEquals("Larva", LOOKUP.lookup("3 larvae").get().getName());
    assertEquals("Larva", LOOKUP.lookup("larvae").get().getName());
    assertEquals("Larva", LOOKUP.lookup("larvas").get().getName());
    assertEquals("Larva", LOOKUP.lookup("larval").get().getName());
    assertEquals("Larva", LOOKUP.lookup("metacercaria").get().getName());
    assertEquals("Larva", LOOKUP.lookup("larve").get().getName());

    assertEquals("Veliger", LOOKUP.lookup("veliger").get().getName());

    assertEquals("Nymph", LOOKUP.lookup("larva, nymph").get().getName());
    assertEquals("Nymph", LOOKUP.lookup("larva/nymph").get().getName());

    assertEquals("Nauplius", LOOKUP.lookup("nauplii").get().getName());

    assertEquals("Tadpole", LOOKUP.lookup("tadpole").get().getName());
    assertEquals("Tadpole", LOOKUP.lookup("tadpoles").get().getName());
    assertEquals("Tadpole", LOOKUP.lookup("têtard").get().getName());
    assertEquals("Tadpole", LOOKUP.lookup("Renacuajo").get().getName());
    assertEquals("Tadpole", LOOKUP.lookup("renacuajos").get().getName());

    assertEquals("Cyprid", LOOKUP.lookup("cyprid").get().getName());

    assertFalse(LOOKUP.lookup("larva; adult").isPresent());
    assertFalse(LOOKUP.lookup("adult, larva").isPresent());
    assertFalse(LOOKUP.lookup("adult;larva").isPresent());
    assertFalse(LOOKUP.lookup("cypris").isPresent());
  }

  @Test
  public void unknownTest() {
    assertEquals("Unknown", LOOKUP.lookup("undetermined").get().getName());
    assertEquals("Unknown", LOOKUP.lookup("unknown").get().getName());
    assertEquals("Unknown", LOOKUP.lookup("não informado").get().getName());
    assertEquals("Unknown", LOOKUP.lookup("not recorded").get().getName());
    assertEquals("Unknown", LOOKUP.lookup("indeterminado").get().getName());
    assertEquals("Unknown", LOOKUP.lookup("No se cuenta con el dato").get().getName());
    assertEquals("Unknown", LOOKUP.lookup("Life Stage Not Recorded").get().getName());
    assertEquals("Unknown", LOOKUP.lookup("Indéterminé").get().getName());
    assertEquals("Unknown", LOOKUP.lookup("  desconocido").get().getName());
    assertEquals("Unknown", LOOKUP.lookup("1K").get().getName());
    assertEquals("Unknown", LOOKUP.lookup("1k+").get().getName());
    assertEquals("Unknown", LOOKUP.lookup("1st calendar year").get().getName());

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
