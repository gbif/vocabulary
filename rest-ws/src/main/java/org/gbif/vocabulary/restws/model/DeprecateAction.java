package org.gbif.vocabulary.restws.model;

/** Common interface for deprecating actions. */
public interface DeprecateAction {

  Integer getReplacementKey();

  void setReplacementKey(Integer replacementKey);

  String getDeprecatedBy();

  void setDeprecatedBy(String deprecatedBy);
}
