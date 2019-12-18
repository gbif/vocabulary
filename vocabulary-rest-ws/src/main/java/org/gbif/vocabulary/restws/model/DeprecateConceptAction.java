package org.gbif.vocabulary.restws.model;

/** Defines the deprecation action for a {@link org.gbif.vocabulary.model.Concept}. */
public class DeprecateConceptAction implements DeprecateAction {

  private Long replacementKey;
  private boolean deprecateChildren;
  private String deprecatedBy;

  @Override
  public Long getReplacementKey() {
    return replacementKey;
  }

  @Override
  public void setReplacementKey(Long replacementKey) {
    this.replacementKey = replacementKey;
  }

  public boolean isDeprecateChildren() {
    return deprecateChildren;
  }

  public void setDeprecateChildren(boolean deprecateChildren) {
    this.deprecateChildren = deprecateChildren;
  }

  @Override
  public String getDeprecatedBy() {
    return deprecatedBy;
  }

  @Override
  public void setDeprecatedBy(String deprecatedBy) {
    this.deprecatedBy = deprecatedBy;
  }
}
