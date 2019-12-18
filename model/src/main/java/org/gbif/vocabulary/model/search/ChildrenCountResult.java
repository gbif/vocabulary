package org.gbif.vocabulary.model.search;

import java.util.Objects;

/** Utility container to hold a concept key and its number of children. */
public class ChildrenCountResult {

  private final int conceptKey;
  private final int childrenCount;

  public ChildrenCountResult(int conceptKey, int childrenCount) {
    this.conceptKey = conceptKey;
    this.childrenCount = childrenCount;
  }

  public int getConceptKey() {
    return conceptKey;
  }

  public int getChildrenCount() {
    return childrenCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChildrenCountResult that = (ChildrenCountResult) o;
    return conceptKey == that.conceptKey && childrenCount == that.childrenCount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(conceptKey, childrenCount);
  }
}
