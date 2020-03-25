package org.gbif.vocabulary.restws.model;

import org.gbif.vocabulary.model.Concept;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Custom view to represent a {@link Concept} plus some additional information. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConceptView implements Serializable {

  @JsonUnwrapped private Concept concept;
  private List<String> parents;
  private Integer childrenCount;

  public ConceptView(Concept concept) {
    this.concept = concept;
  }

  public ConceptView(Concept concept, Integer childrenCount) {
    this.concept = concept;
    this.childrenCount = childrenCount;
  }

  public Concept getConcept() {
    return concept;
  }

  public ConceptView setConcept(Concept concept) {
    this.concept = concept;
    return this;
  }

  public List<String> getParents() {
    return parents;
  }

  public ConceptView setParents(List<String> parents) {
    this.parents = parents;
    return this;
  }

  public Integer getChildrenCount() {
    return childrenCount;
  }

  public ConceptView setChildrenCount(Integer childrenCount) {
    this.childrenCount = childrenCount;
    return this;
  }
}
