package org.gbif.vocabulary.model.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests the {@link ConceptSearchParams}. */
public class ConceptSearchParamsTest {

  @Test
  public void builderTest() {
    String name = "v1";
    boolean deprecated = true;
    String query = "foo";
    Integer key = 1;

    ConceptSearchParams params =
        ConceptSearchParams.builder()
            .name(name)
            .deprecated(deprecated)
            .parentKey(key)
            .replacedByKey(key)
            .vocabularyKey(key)
            .query(query)
            .build();

    assertEquals(name, params.getName());
    assertEquals(deprecated, params.getDeprecated());
    assertEquals(key, params.getParentKey());
    assertEquals(key, params.getReplacedByKey());
    assertEquals(key, params.getVocabularyKey());
    assertEquals(query, params.getQuery());
  }
}
