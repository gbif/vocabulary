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
package org.gbif.vocabulary;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Deprecable;
import org.gbif.vocabulary.model.Vocabulary;

import java.util.Random;
import java.util.UUID;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Utility class for testing purposes. */
public final class TestUtils {

  public static final String DEPRECATED_BY = "deprecator";
  private static final String TEST_USER = "test";
  private static final Random RANDOM = new Random();

  public static final BiFunction<Integer, Long, Pageable> PAGE_FN =
      (limit, offset) ->
          new Pageable() {
            @Override
            public int getLimit() {
              return limit;
            }

            @Override
            public long getOffset() {
              return offset;
            }
          };

  public static final Pageable DEFAULT_PAGE = PAGE_FN.apply(10, 0L);

  private TestUtils() {}

  public static <T extends Deprecable> void assertNotDeprecated(T restored) {
    assertNull(restored.getDeprecated());
    assertNull(restored.getReplacedByKey());
    assertNull(restored.getDeprecatedBy());
  }

  public static <T extends Deprecable> void assertDeprecated(T deprecated, String deprecatedBy) {
    assertNotNull(deprecated.getDeprecated());
    assertNull(deprecated.getReplacedByKey());
    assertEquals(deprecatedBy, deprecated.getDeprecatedBy());
  }

  public static <T extends Deprecable> void assertDeprecatedWithReplacement(
      T deprecated, String deprecatedBy, long replacementKey) {
    assertNotNull(deprecated.getDeprecated());
    assertEquals(deprecatedBy, deprecated.getDeprecatedBy());
    assertEquals(replacementKey, deprecated.getReplacedByKey().intValue());
  }

  public static Concept createBasicConcept(long vocabularyKey) {
    Concept concept = new Concept();
    concept.setName(getRandomName());
    concept.setVocabularyKey(vocabularyKey);
    concept.setCreatedBy(TEST_USER);
    concept.setModifiedBy(TEST_USER);
    return concept;
  }

  public static Vocabulary createBasicVocabulary() {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(getRandomName());
    vocabulary.setCreatedBy(TEST_USER);
    vocabulary.setModifiedBy(TEST_USER);
    return vocabulary;
  }

  public static String getRandomName() {
    return "N" + UUID.randomUUID().toString().replace("-", "");
  }
}
