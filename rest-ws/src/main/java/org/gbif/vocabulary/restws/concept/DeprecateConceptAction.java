package org.gbif.vocabulary.restws.concept;

/** Defines the deprecation action for a {@link org.gbif.vocabulary.model.Concept}. */
public class DeprecateConceptAction {

  private Integer replacementKey;
  private boolean deprecateChildren;

  public Integer getReplacementKey() {
    return replacementKey;
  }

  public void setReplacementKey(Integer replacementKey) {
    this.replacementKey = replacementKey;
  }

  public boolean isDeprecateChildren() {
    return deprecateChildren;
  }

  public void setDeprecateChildren(boolean deprecateChildren) {
    this.deprecateChildren = deprecateChildren;
  }
}
