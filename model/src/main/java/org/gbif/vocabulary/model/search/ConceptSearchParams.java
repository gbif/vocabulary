package org.gbif.vocabulary.model.search;

import java.io.Serializable;

import lombok.Builder;
import lombok.Getter;

/** Holder for the concept search parameters. */
@Getter
@Builder
public class ConceptSearchParams implements Serializable {

  private final String query;
  private final Long vocabularyKey;
  private final Long parentKey;
  private final Long replacedByKey;
  private final String name;
  private final Boolean deprecated;
  private final Long key;
  private Boolean hasParent;
  private Boolean hasReplacement;

  public static ConceptSearchParams empty() {
    return builder().build();
  }
}
