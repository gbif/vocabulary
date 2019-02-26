package org.gbif.vocabulary.restws.vocabulary;

/** Defines the deprecation action for a {@link org.gbif.vocabulary.model.Vocabulary}. */
public class DeprecateVocabularyAction {

  private Integer replacementKey;
  private boolean deprecateConcepts;

  public Integer getReplacementKey() {
    return replacementKey;
  }

  public void setReplacementKey(Integer replacementKey) {
    this.replacementKey = replacementKey;
  }

  public boolean isDeprecateConcepts() {
    return deprecateConcepts;
  }

  public void setDeprecateConcepts(boolean deprecateConcepts) {
    this.deprecateConcepts = deprecateConcepts;
  }
}
