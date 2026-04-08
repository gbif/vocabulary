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
package org.gbif.vocabulary.importer;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeologicalContextImporterTest {

  @Test
  void shouldExtractTagValueFromColonSeparatedName() {
    Concept concept = conceptWithTags("rank: eon");

    String value = extractTagValueFromName(concept, "rank");

    assertEquals("eon", value);
  }

  @Test
  void shouldTrimTagNameAndValueWhenSplittingByColon() {
    Concept concept = conceptWithTags("rank :   eon  ");

    String value = extractTagValueFromName(concept, "rank");

    assertEquals("eon", value);
  }

  @Test
  void shouldReturnNullWhenTagNameHasNoColon() {
    Concept concept = conceptWithTags("rank eon");

    String value = extractTagValueFromName(concept, "rank");

    assertNull(value);
  }

  @Test
  void shouldReturnNullWhenTagValueIsEmptyAfterColon() {
    Concept concept = conceptWithTags("rank:   ");

    String value = extractTagValueFromName(concept, "rank");

    assertNull(value);
  }

  @Test
  void shouldMatchNumericValuesForStartAgeTag() {
    assertTrue(matchesNumericValue(538.8d, "538.8"));
    assertFalse(matchesNumericValue(538.8d, "538.80x"));
  }

  @Test
  void shouldMapLanguageToLocaleWithFallbackWhenRegionIsMissing() {
    assertEquals(LanguageRegion.SPANISH, resolveLanguageRegion("es"));
    assertEquals(LanguageRegion.ENGLISH, resolveLanguageRegion("en"));
  }

  @Test
  void shouldResolveUniqueIso2LetterCodeBeforeLocaleLookup() {
    assertEquals(LanguageRegion.BASQUE, resolveLanguageRegion("eu"));
  }

  @Test
  void shouldMapZhToChineseSimplified() {
    assertEquals(LanguageRegion.CHINESE_SIMPLIFIED, resolveLanguageRegion("zh"));
  }

  @Test
  void shouldReturnUnknownLanguageWhenCannotMapLocale() {
    assertEquals(LanguageRegion.UNKNOWN, resolveLanguageRegion("zz"));
    assertEquals(LanguageRegion.UNKNOWN, resolveLanguageRegion(""));
  }

  private static Concept conceptWithTags(String... tagNames) {
    Concept concept = new Concept();
    concept.setTags(List.of(tagNames).stream().map(Tag::of).toList());
    return concept;
  }

  private static String extractTagValueFromName(Concept concept, String expectedTagName) {
    try {
      Method method =
          GeologicalContextImporter.class.getDeclaredMethod(
              "extractTagValueFromName", Concept.class, String.class);
      method.setAccessible(true);
      return (String) method.invoke(null, concept, expectedTagName);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Unable to invoke extractTagValueFromName", e);
    }
  }

  private static boolean matchesNumericValue(Double expectedValue, String actualValue) {
    try {
      Method method =
          GeologicalContextImporter.class.getDeclaredMethod(
              "matchesNumericValue", Double.class, String.class);
      method.setAccessible(true);
      return (boolean) method.invoke(null, expectedValue, actualValue);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Unable to invoke matchesNumericValue", e);
    }
  }

  private static LanguageRegion resolveLanguageRegion(String rawLanguage) {
    try {
      Method method =
          GeologicalContextImporter.class.getDeclaredMethod("resolveLanguageRegion", String.class);
      method.setAccessible(true);
      return (LanguageRegion) method.invoke(null, rawLanguage);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Unable to invoke resolveLanguageRegion", e);
    }
  }
}

