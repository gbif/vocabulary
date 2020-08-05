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
