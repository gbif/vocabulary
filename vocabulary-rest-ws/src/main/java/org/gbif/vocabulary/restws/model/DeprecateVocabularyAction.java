package org.gbif.vocabulary.restws.model;

/** Defines the deprecation action for a {@link org.gbif.vocabulary.model.Vocabulary}. */
public class DeprecateVocabularyAction implements DeprecateAction {

  private Long replacementKey;
  private boolean deprecateConcepts;
  private String deprecatedBy;

  @Override
  public Long getReplacementKey() {
    return replacementKey;
  }

  @Override
  public void setReplacementKey(Long replacementKey) {
    this.replacementKey = replacementKey;
  }

  public boolean isDeprecateConcepts() {
    return deprecateConcepts;
  }

  public void setDeprecateConcepts(boolean deprecateConcepts) {
    this.deprecateConcepts = deprecateConcepts;
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
