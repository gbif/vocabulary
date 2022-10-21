package org.gbif.vocabulary.api;

import java.io.Serializable;

import org.gbif.vocabulary.model.Vocabulary;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@NoArgsConstructor
public class VocabularyView implements Serializable, EntityView<Vocabulary> {

  @JsonUnwrapped Vocabulary vocabulary;

  String labelsLink;

  public VocabularyView(Vocabulary vocabulary) {
    this.vocabulary = vocabulary;
  }

  public VocabularyView(Vocabulary vocabulary, String labelsLink) {
    this.vocabulary = vocabulary;
    this.labelsLink = labelsLink;
  }

  public Vocabulary getVocabulary() {
    return vocabulary;
  }

  public VocabularyView setVocabulary(Vocabulary vocabulary) {
    this.vocabulary = vocabulary;
    return this;
  }

  @Override
  public Vocabulary getEntity() {
    return vocabulary;
  }
}
