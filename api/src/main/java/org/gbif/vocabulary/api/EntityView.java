package org.gbif.vocabulary.api;

import org.gbif.vocabulary.model.VocabularyEntity;

public interface EntityView<T extends VocabularyEntity> {

  T getEntity();
}
