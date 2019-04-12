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
