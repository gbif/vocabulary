package org.gbif.vocabulary.model.export;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Models an export of a vocabulary with all its concepts. */
@Getter
@Setter
public class VocabularyExport implements Serializable {

  public static final String METADATA_PROP = "metadata";
  public static final String VOCABULARY_PROP = "vocabulary";
  public static final String CONCEPTS_PROP = "concepts";

  @JsonProperty(METADATA_PROP)
  private ExportMetadata metadata;

  @JsonProperty(VOCABULARY_PROP)
  private Vocabulary vocabulary;

  @JsonProperty(CONCEPTS_PROP)
  private List<Concept> concepts;
}
