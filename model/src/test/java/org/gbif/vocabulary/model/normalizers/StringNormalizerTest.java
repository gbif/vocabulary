package org.gbif.vocabulary.model.normalizers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests the {@link StringNormalizer} . */
public class StringNormalizerTest {

  @Test
  public void normalizeNameTest() {
    assertEquals("samplename", StringNormalizer.normalizeName("sample-name"));
    assertEquals("samplename", StringNormalizer.normalizeName("_sample  -name  "));
  }

  @Test
  public void normalizeLabelTest() {
    assertEquals("sampletext", StringNormalizer.normalizeLabel(" SamPLE  TEXT  "));
  }

  @Test
  public void replaceNonAsciiCharsWithReplacementsTest() {
    assertEquals(
        "IObDGHLTZo aeiouancuuuuALEO",
        StringNormalizer.replaceNonAsciiCharactersWithEquivalents("ƗØƀĐǤĦŁŦƵø àéîoüåñçǖǘǚǜǍĽĒŎ"));
  }
}
