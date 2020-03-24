package org.gbif.vocabulary.restws.model;

/** Common interface for deprecating actions. */
public interface DeprecateAction {

  Long getReplacementKey();

  void setReplacementKey(Long replacementKey);

  String getDeprecatedBy();

  void setDeprecatedBy(String deprecatedBy);
}
