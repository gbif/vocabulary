package org.gbif.vocabulary.model.export;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Models an export of a vocabulary with all its concepts. */
public class VocabularyExport {

  public static final String METADATA_PROP = "metadata";
  public static final String VOCABULARY_PROP = "vocabulary";
  public static final String CONCEPTS_PROP = "concepts";

  @JsonProperty(METADATA_PROP)
  private ExportMetadata metadata;

  @JsonProperty(VOCABULARY_PROP)
  private Vocabulary vocabulary;

  @JsonProperty(CONCEPTS_PROP)
  private List<Concept> concepts;

  public ExportMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(ExportMetadata metadata) {
    this.metadata = metadata;
  }

  public Vocabulary getVocabulary() {
    return vocabulary;
  }

  public void setVocabulary(Vocabulary vocabulary) {
    this.vocabulary = vocabulary;
  }

  public List<Concept> getConcepts() {
    return concepts;
  }

  public void setConcepts(List<Concept> concepts) {
    this.concepts = concepts;
  }
}
