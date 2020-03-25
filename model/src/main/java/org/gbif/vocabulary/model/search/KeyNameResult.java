package org.gbif.vocabulary.model.search;

import lombok.Data;

/**
 * Utility container to hold the key and name of a {@link
 * org.gbif.vocabulary.model.VocabularyEntity}.
 */
@Data
public class KeyNameResult {
  private long key;
  private String name;
}
