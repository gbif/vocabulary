package org.gbif.vocabulary.lookup;

import java.util.List;

import org.gbif.vocabulary.model.Concept;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
public class LookupConcept {

  private Concept concept;
  private List<String> parents;
}
