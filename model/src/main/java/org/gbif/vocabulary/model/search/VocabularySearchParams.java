package org.gbif.vocabulary.model.search;

import java.io.Serializable;

import lombok.Builder;
import lombok.Getter;

/** Holder for the vocabulary search parameters. */
@Getter
@Builder
public class VocabularySearchParams implements Serializable {

  private final String query;
  private final String name;
  private final String namespace;
  private final Boolean deprecated;
  private final Long key;

  public static VocabularySearchParams empty() {
    return builder().build();
  }
}
