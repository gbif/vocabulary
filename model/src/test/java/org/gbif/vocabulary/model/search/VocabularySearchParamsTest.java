package org.gbif.vocabulary.model.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests the {@link VocabularySearchParams}. */
public class VocabularySearchParamsTest {

  @Test
  public void builderTest() {
    String name = "v1";
    boolean deprecated = true;
    String namespace = "ns";
    String query = "foo";

    VocabularySearchParams params =
        VocabularySearchParams.builder()
            .name(name)
            .deprecated(deprecated)
            .namespace(namespace)
            .query(query)
            .build();

    assertEquals(name, params.getName());
    assertEquals(deprecated, params.getDeprecated());
    assertEquals(namespace, params.getNamespace());
    assertEquals(query, params.getQuery());
  }
}
