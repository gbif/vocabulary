package org.gbif.vocabulary.restws.model;

import lombok.Getter;
import lombok.Setter;

/** Defines the deprecation action for a {@link org.gbif.vocabulary.model.Vocabulary}. */
@Getter
@Setter
public class DeprecateVocabularyAction implements DeprecateAction {
  private Long replacementKey;
  private boolean deprecateConcepts;
  private String deprecatedBy;
}
