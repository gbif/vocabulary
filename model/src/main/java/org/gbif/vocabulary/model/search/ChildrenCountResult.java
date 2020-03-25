package org.gbif.vocabulary.model.search;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Utility container to hold a concept key and its number of children. */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class ChildrenCountResult {
  private final long conceptKey;
  private final int childrenCount;
}
