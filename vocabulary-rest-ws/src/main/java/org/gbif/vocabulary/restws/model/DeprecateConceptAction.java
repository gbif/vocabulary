package org.gbif.vocabulary.restws.model;

import lombok.Getter;
import lombok.Setter;

/** Defines the deprecation action for a {@link org.gbif.vocabulary.model.Concept}. */
@Getter
@Setter
public class DeprecateConceptAction implements DeprecateAction {
  private Long replacementKey;
  private boolean deprecateChildren;
  private String deprecatedBy;
}
