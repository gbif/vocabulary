package org.gbif.vocabulary.api;

import org.gbif.vocabulary.model.VocabularyEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface EntityView<T extends VocabularyEntity> {

  @JsonIgnore
  T getEntity();
}
