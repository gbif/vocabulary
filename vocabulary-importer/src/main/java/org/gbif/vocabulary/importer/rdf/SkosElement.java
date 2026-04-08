/*
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
package org.gbif.vocabulary.importer.rdf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import org.apache.jena.vocabulary.SKOS;

@Getter
public class SkosElement {

  private final String uri;
  private final Set<String> types;
  private final Map<String, String> prefLabels;
  private final Map<String, String> definitions;
  private final Set<String> broaderUris;
  private final Set<String> memberUris;
  private final Set<String> inSchemeUris;
  private final boolean concept;
  private final String conceptName;
  private final String rank;
  private final TimeInterval hasBeginning;
  private final TimeInterval hasEnd;

  public SkosElement(
      String uri,
      Set<String> types,
      Map<String, String> prefLabels,
      Map<String, String> definitions,
      Set<String> broaderUris,
      Set<String> memberUris,
      Set<String> inSchemeUris,
      boolean concept,
      String conceptName,
      String rank,
      TimeInterval hasBeginning,
      TimeInterval hasEnd) {
    this.uri = uri;
    this.types = Collections.unmodifiableSet(new LinkedHashSet<>(types));
    this.prefLabels = Collections.unmodifiableMap(new LinkedHashMap<>(prefLabels));
    this.definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
    this.broaderUris = Collections.unmodifiableSet(new LinkedHashSet<>(broaderUris));
    this.memberUris = Collections.unmodifiableSet(new LinkedHashSet<>(memberUris));
    this.inSchemeUris = Collections.unmodifiableSet(new LinkedHashSet<>(inSchemeUris));
    this.concept = concept;
    this.conceptName = conceptName;
    this.rank = rank;
    this.hasBeginning = hasBeginning;
    this.hasEnd = hasEnd;
  }

  public boolean isCollection() {
    return types.contains(SKOS.Collection.getURI());
  }

  public boolean isConceptScheme() {
    return types.contains(SKOS.ConceptScheme.getURI());
  }

  public Optional<String> getPrefLabel(String language) {
    return Optional.ofNullable(prefLabels.get(language));
  }

  public Optional<String> getDisplayLabel() {
    if (prefLabels.containsKey("en")) {
      return Optional.of(prefLabels.get("en"));
    }
    if (prefLabels.containsKey("")) {
      return Optional.of(prefLabels.get(""));
    }
    return prefLabels.values().stream().findFirst();
  }

  public record TimeInterval(Double inMYA, Double marginOfError) {}
}
