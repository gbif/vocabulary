/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.vocabulary.api;

import org.gbif.vocabulary.model.Concept;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Custom view to represent a {@link Concept} plus some additional information. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class ConceptView implements Serializable {

  @JsonUnwrapped private Concept concept;
  private List<String> parents;
  private Integer childrenCount;
  private List<String> children;

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

  public List<String> getChildren() {
    return children;
  }

  public ConceptView setChildren(List<String> children) {
    this.children = children;
    return this;
  }
}
