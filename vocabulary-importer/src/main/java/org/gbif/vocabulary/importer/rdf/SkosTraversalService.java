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

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class SkosTraversalService {

  public List<SkosElement> readElements(String source) {
    return extractElements(new SkosTurtleReader().read(source));
  }

  public List<SkosElement> extractElements(Model model) {
    Map<String, MutableSkosElement> elementsByUri = new TreeMap<>();

    model.listStatements().forEachRemaining(statement -> collectStatement(statement, elementsByUri, model));

    return elementsByUri.values().stream().map(MutableSkosElement::toImmutable).toList();
  }

  public void walk(Model model, BiConsumer<Integer, SkosElement> visitor) {
    walk(extractElements(model), visitor);
  }

  public void walk(Collection<SkosElement> elements, BiConsumer<Integer, SkosElement> visitor) {
    Map<String, SkosElement> elementsByUri =
        elements.stream().collect(Collectors.toMap(SkosElement::getUri, element -> element, (a, b) -> a, LinkedHashMap::new));

    Map<String, Set<String>> childrenByUri = buildChildrenIndex(elementsByUri);
    Set<String> children =
        childrenByUri.values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    List<String> roots =
        elementsByUri.values().stream()
            .filter(element -> isRoot(element, children))
            .map(SkosElement::getUri)
            .sorted()
            .toList();

    Set<String> visited = new LinkedHashSet<>();
    roots.forEach(root -> depthFirstWalk(root, 0, elementsByUri, childrenByUri, visited, visitor));

    elementsByUri.keySet().stream()
        .sorted()
        .filter(uri -> !visited.contains(uri))
        .forEach(uri -> depthFirstWalk(uri, 0, elementsByUri, childrenByUri, visited, visitor));
  }

  public void walkConcepts(Model model, BiConsumer<Integer, SkosElement> visitor) {
    walkConcepts(extractElements(model), visitor);
  }

  public void walkConcepts(Collection<SkosElement> elements, BiConsumer<Integer, SkosElement> visitor) {
    walk(elements, (depth, element) -> {
      if (element.isConcept()) {
        visitor.accept(depth, element);
      }
    });
  }

  public List<SkosElement> listConcepts(Model model) {
    return listConcepts(extractElements(model));
  }

  public List<SkosElement> listConcepts(Collection<SkosElement> elements) {
    List<SkosElement> concepts = new java.util.ArrayList<>();
    walkConcepts(elements, (depth, element) -> concepts.add(element));
    return concepts;
  }

  private void collectStatement(Statement statement, Map<String, MutableSkosElement> elementsByUri, Model model) {
    if (!statement.getSubject().isURIResource()) {
      return;
    }

    String subjectUri = statement.getSubject().getURI();
    MutableSkosElement element = elementsByUri.computeIfAbsent(subjectUri, MutableSkosElement::new);

    RDFNode object = statement.getObject();
    Property predicate = statement.getPredicate();
    String localName = predicate.getLocalName();

    if (RDF.type.equals(predicate) && object.isURIResource()) {
      String typeUri = object.asResource().getURI();
      element.types.add(typeUri);
      if (SKOS.Concept.getURI().equals(typeUri)) {
        element.concept = true;
      }
      return;
    }
    if (SKOS.prefLabel.equals(predicate) && object.isLiteral()) {
      addLiteralValue(element.prefLabels, object.asLiteral());
      return;
    }
    if (SKOS.definition.equals(predicate) && object.isLiteral()) {
      addLiteralValue(element.definitions, object.asLiteral());
      return;
    }
    if (SKOS.broader.equals(predicate) && object.isURIResource()) {
      elementsByUri.computeIfAbsent(object.asResource().getURI(), MutableSkosElement::new);
      element.broaderUris.add(object.asResource().getURI());
      return;
    }
    if (SKOS.member.equals(predicate) && object.isURIResource()) {
      elementsByUri.computeIfAbsent(object.asResource().getURI(), MutableSkosElement::new);
      element.memberUris.add(object.asResource().getURI());
      return;
    }
    if (SKOS.inScheme.equals(predicate) && object.isURIResource()) {
      elementsByUri.computeIfAbsent(object.asResource().getURI(), MutableSkosElement::new);
      element.inSchemeUris.add(object.asResource().getURI());
    }

    // Handle rank predicate - match by local name
    if ("rank".equals(localName) && object.isURIResource()) {
      element.rank = object.asNode().getLocalName();
      return;
    }

    // Handle time:hasBeginning predicate
    if ("hasBeginning".equals(localName) && object.isAnon()) {
      element.hasBeginning = parseTimeInterval(object.asResource(), model);
      return;
    }

    // Handle time:hasEnd predicate
    if ("hasEnd".equals(localName) && object.isAnon()) {
      element.hasEnd = parseTimeInterval(object.asResource(), model);
      return;
    }
  }

  private void addLiteralValue(Map<String, String> valuesByLanguage, Literal literal) {
    valuesByLanguage.putIfAbsent(literal.getLanguage(), literal.getString());
  }

  private SkosElement.TimeInterval parseTimeInterval(Resource resource, Model model) {
    Double inMYA = null;
    Double marginOfError = null;

    // Extract inMYA value - find the property with local name "inMYA"
    List<Statement> myaStatements = resource.listProperties().toList().stream()
        .filter(stmt -> "inMYA".equals(stmt.getPredicate().getLocalName()))
        .toList();
    if (!myaStatements.isEmpty()) {
      RDFNode myaNode = myaStatements.get(0).getObject();
      if (myaNode.isLiteral()) {
        try {
          inMYA = Double.parseDouble(myaNode.asLiteral().getString());
        } catch (NumberFormatException e) {
          // Handle parsing error
        }
      }
    }

    // Extract marginOfError value - find the property with local name "marginOfError"
    List<Statement> marginStatements = resource.listProperties().toList().stream()
        .filter(stmt -> "marginOfError".equals(stmt.getPredicate().getLocalName()))
        .toList();
    if (!marginStatements.isEmpty()) {
      RDFNode marginNode = marginStatements.get(0).getObject();
      if (marginNode.isLiteral()) {
        try {
          marginOfError = Double.parseDouble(marginNode.asLiteral().getString());
        } catch (NumberFormatException e) {
          // Handle parsing error
        }
      }
    }

    if (inMYA != null) {
      return new SkosElement.TimeInterval(inMYA, marginOfError);
    }
    return null;
  }

  private Map<String, Set<String>> buildChildrenIndex(Map<String, SkosElement> elementsByUri) {
    Map<String, Set<String>> childrenByUri = new TreeMap<>();

    elementsByUri.values().forEach(element -> childrenByUri.putIfAbsent(element.getUri(), new TreeSet<>()));

    for (SkosElement element : elementsByUri.values()) {
      for (String schemeUri : element.getInSchemeUris()) {
        addChild(childrenByUri, elementsByUri, schemeUri, element.getUri());
      }
      for (String broaderUri : element.getBroaderUris()) {
        addChild(childrenByUri, elementsByUri, broaderUri, element.getUri());
      }
      for (String memberUri : element.getMemberUris()) {
        addChild(childrenByUri, elementsByUri, element.getUri(), memberUri);
      }
    }

    return childrenByUri;
  }

  private void addChild(
      Map<String, Set<String>> childrenByUri,
      Map<String, SkosElement> elementsByUri,
      String parentUri,
      String childUri) {
    if (!elementsByUri.containsKey(parentUri) || !elementsByUri.containsKey(childUri)) {
      return;
    }
    childrenByUri.computeIfAbsent(parentUri, key -> new TreeSet<>()).add(childUri);
  }

  private boolean isRoot(SkosElement element, Set<String> children) {
    if (element.isConceptScheme()) {
      return true;
    }
    return !children.contains(element.getUri()) && hasStructuralRelationships(element);
  }

  private boolean hasStructuralRelationships(SkosElement element) {
    return element.isConcept()
        || element.isCollection()
        || !element.getBroaderUris().isEmpty()
        || !element.getMemberUris().isEmpty()
        || !element.getInSchemeUris().isEmpty();
  }

  private void depthFirstWalk(
      String uri,
      int depth,
      Map<String, SkosElement> elementsByUri,
      Map<String, Set<String>> childrenByUri,
      Set<String> visited,
      BiConsumer<Integer, SkosElement> visitor) {
    if (!visited.add(uri)) {
      return;
    }

    SkosElement current = elementsByUri.get(uri);
    visitor.accept(depth, current);

    for (String childUri : childrenByUri.getOrDefault(uri, Set.of())) {
      depthFirstWalk(childUri, depth + 1, elementsByUri, childrenByUri, visited, visitor);
    }
  }

  private static String extractConceptName(String uri) {
    int hashIndex = uri.lastIndexOf('#');
    int slashIndex = uri.lastIndexOf('/');
    int splitIndex = Math.max(hashIndex, slashIndex);
    if (splitIndex < 0 || splitIndex == uri.length() - 1) {
      return uri;
    }
    return uri.substring(splitIndex + 1);
  }

  private static class MutableSkosElement {

    private final String uri;
    private final Set<String> types = new TreeSet<>();
    private final Map<String, String> prefLabels = new TreeMap<>();
    private final Map<String, String> definitions = new TreeMap<>();
    private final Set<String> broaderUris = new TreeSet<>();
    private final Set<String> memberUris = new TreeSet<>();
    private final Set<String> inSchemeUris = new TreeSet<>();
    private boolean concept = false;
    private String rank = null;
    private SkosElement.TimeInterval hasBeginning = null;
    private SkosElement.TimeInterval hasEnd = null;

    private MutableSkosElement(String uri) {
      this.uri = uri;
    }

    private SkosElement toImmutable() {
      String conceptName = concept ? extractConceptName(uri) : null;
      return new SkosElement(
          uri,
          types,
          prefLabels,
          definitions,
          broaderUris,
          memberUris,
          inSchemeUris,
          concept,
          conceptName,
          rank,
          hasBeginning,
          hasEnd);
    }
  }
}

