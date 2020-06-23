package org.gbif.vocabulary.service.impl;

import org.junit.jupiter.api.Test;

import static org.gbif.vocabulary.service.impl.DefaultExportService.checkVersionFormat;
import static org.gbif.vocabulary.service.impl.DefaultExportService.getVersionNumber;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests specific methods of {@link DefaultExportService}. */
public class DefaultExportServiceTest {

  @Test
  public void checkVersionFormatTest() {
    assertDoesNotThrow(() -> checkVersionFormat("0.0.1"));
    assertDoesNotThrow(() -> checkVersionFormat("0.0.1-SNAPSHOT"));
    assertThrows(IllegalArgumentException.class, () -> checkVersionFormat("1"));
    assertThrows(IllegalArgumentException.class, () -> checkVersionFormat("1,0"));
    assertThrows(IllegalArgumentException.class, () -> checkVersionFormat("1.0"));
    assertThrows(IllegalArgumentException.class, () -> checkVersionFormat("1.0."));
    assertThrows(IllegalArgumentException.class, () -> checkVersionFormat("a.b.c"));
  }

  @Test
  public void getVersionNumberTest() {
    assertEquals(1, getVersionNumber("0.0.1"));
    assertEquals(101, getVersionNumber("1.0.1"));
    assertEquals(111, getVersionNumber("1.1.1-SNAPSHOT"));
  }
}
