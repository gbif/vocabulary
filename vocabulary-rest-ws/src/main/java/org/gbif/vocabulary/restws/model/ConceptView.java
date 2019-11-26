package org.gbif.vocabulary.restws.model;

import org.gbif.vocabulary.model.Concept;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Custom view to represent a {@link Concept} plus some additional information. */
public class ConceptView implements Serializable {

  @JsonUnwrapped
  private Concept concept;
  private List<String> parents;

  public ConceptView() {}

  public ConceptView(Concept concept) {
    this.concept = concept;
  }

  public Concept getConcept() {
    return concept;
  }

  public void setConcept(Concept concept) {
    this.concept = concept;
  }

  public List<String> getParents() {
    return parents;
  }

  public void setParents(List<String> parents) {
    this.parents = parents;
  }
}
