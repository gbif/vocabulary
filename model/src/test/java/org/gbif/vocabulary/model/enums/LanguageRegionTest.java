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
package org.gbif.vocabulary.model.enums;

import org.gbif.vocabulary.model.LanguageRegion;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Tests the {@link LanguageRegion}. */
public class LanguageRegionTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void testFromIsoCode() {
    assertEquals(LanguageRegion.ENGLISH, LanguageRegion.fromLocale("en"));
    assertEquals(LanguageRegion.SPANISH, LanguageRegion.fromLocale("es-ES"));
    assertEquals(LanguageRegion.SPANISH, LanguageRegion.fromLocale("es-es"));
    assertEquals(LanguageRegion.ARABIC, LanguageRegion.fromLocale("ar"));
    assertEquals(LanguageRegion.ARPITAN, LanguageRegion.fromLocale("frp-IT"));
  }

  @Test
  public void testSerDe() {
    try {
      String json = MAPPER.writeValueAsString(LanguageRegion.AFAR);
      assertEquals(LanguageRegion.AFAR.getLocale(), MAPPER.readTree(json).asText());
      assertEquals(LanguageRegion.AFAR, MAPPER.readValue(json, LanguageRegion.class));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testKeySerDe() {
    try {
      Map<LanguageRegion, String> languageMap = new EnumMap<>(LanguageRegion.class);
      languageMap.put(LanguageRegion.SPANISH, "foo");

      String json = MAPPER.writeValueAsString(languageMap);
      assertTrue(MAPPER.readTree(json).has(LanguageRegion.SPANISH.getLocale()));

      JavaType type =
          MAPPER
              .getTypeFactory()
              .constructMapType(HashMap.class, LanguageRegion.class, String.class);
      Map<LanguageRegion, String> mapDeserialized = MAPPER.readValue(json, type);
      assertEquals("foo", mapDeserialized.get(LanguageRegion.SPANISH));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
