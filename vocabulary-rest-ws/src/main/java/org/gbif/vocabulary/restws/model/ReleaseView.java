package org.gbif.vocabulary.restws.model;

import java.io.Serializable;
import java.util.List;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;

import lombok.Data;

/** View to display the content of a vocabulary release in the API. */
@Data
public class ReleaseView implements Serializable {

  private Vocabulary vocabulary;
  private List<Concept> concepts;
}
