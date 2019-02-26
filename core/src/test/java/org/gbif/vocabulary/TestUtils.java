package org.gbif.vocabulary;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Deprecable;
import org.gbif.vocabulary.model.Vocabulary;

import java.util.UUID;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Utility class for testing purposes. */
public final class TestUtils {

  public static final String DEPRECATED_BY = "deprecator";
  private static final String TEST_USER = "test";

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
      T deprecated, String deprecatedBy, int replacementKey) {
    assertNotNull(deprecated.getDeprecated());
    assertEquals(deprecatedBy, deprecated.getDeprecatedBy());
    assertEquals(replacementKey, deprecated.getReplacedByKey().intValue());
  }

  public static Concept createBasicConcept(int vocabularyKey) {
    Concept concept = new Concept();
    concept.setName(UUID.randomUUID().toString());
    concept.setVocabularyKey(vocabularyKey);
    concept.setCreatedBy(TEST_USER);
    concept.setModifiedBy(TEST_USER);
    return concept;
  }

  public static Vocabulary createBasicVocabulary() {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(UUID.randomUUID().toString());
    vocabulary.setCreatedBy(TEST_USER);
    vocabulary.setModifiedBy(TEST_USER);

    return vocabulary;
  }

}
