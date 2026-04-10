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
package org.gbif.vocabulary.importer.geotime;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkosTraversalServiceTest {

  private final SkosTurtleReader turtleReader = new SkosTurtleReader();
  private final SkosTraversalService traversalService = new SkosTraversalService();

  @Test
  void shouldNormalizeGitHubBlobUrls() {
    String normalized =
        SkosTurtleReader.normalizeSource(
            "https://github.com/i-c-stratigraphy/chart/blob/main/chart.ttl");

    assertEquals(
        "https://raw.githubusercontent.com/i-c-stratigraphy/chart/main/chart.ttl", normalized);
  }

  @Test
  void shouldExtractAllResourcesFromTurtleFixture() throws URISyntaxException {
    Path fixturePath = fixturePath();

    List<SkosElement> elements = traversalService.extractElements(turtleReader.read(fixturePath.toString()));

    assertEquals(5, elements.size());
    assertEquals(1, elements.stream().filter(SkosElement::isConceptScheme).count());
    assertEquals(1, elements.stream().filter(SkosElement::isCollection).count());
    assertEquals(2, elements.stream().filter(SkosElement::isConcept).count());
    assertTrue(
        elements.stream()
            .anyMatch(
                element ->
                    "Ordovician".equals(element.getPrefLabel("en").orElse(null))
                        && element
                            .getDefinitions()
                            .containsValue("A geologic period of the Paleozoic era")));
  }

  @Test
  void shouldWalkHierarchyAndVisitDisconnectedResources() throws URISyntaxException {
    Path fixturePath = fixturePath();

    List<SkosElement> elements = traversalService.extractElements(turtleReader.read(fixturePath.toString()));
    List<String> visitedUris = new ArrayList<>();

    traversalService.walk(elements, (depth, element) -> visitedUris.add(depth + ":" + element.getUri()));

    assertEquals(
        List.of(
            "0:http://resource.geosciml.org/classifier/ics/ischart",
            "1:http://resource.geosciml.org/classifier/ics/ischart/Ages",
            "2:http://resource.geosciml.org/classifier/ics/ischart/Ordovician",
            "3:http://resource.geosciml.org/classifier/ics/ischart/Darriwilian",
            "0:http://purl.org/dc/terms/created"),
        visitedUris);
  }

  @Test
  void shouldWalkOnlyConceptElements() throws URISyntaxException {
    Path fixturePath = fixturePath();

    List<SkosElement> elements = traversalService.extractElements(turtleReader.read(fixturePath.toString()));
    List<String> visitedConceptUris = new ArrayList<>();

    traversalService.walkConcepts(
        elements, (depth, element) -> visitedConceptUris.add(depth + ":" + element.getUri()));

    assertEquals(
        List.of(
            "2:http://resource.geosciml.org/classifier/ics/ischart/Ordovician",
            "3:http://resource.geosciml.org/classifier/ics/ischart/Darriwilian"),
        visitedConceptUris);
  }

  @Test
  void shouldListOnlyConceptElements() throws URISyntaxException {
    Path fixturePath = fixturePath();

    List<SkosElement> elements = traversalService.extractElements(turtleReader.read(fixturePath.toString()));
    List<String> conceptUris =
        traversalService.listConcepts(elements).stream().map(SkosElement::getUri).toList();

    assertEquals(
        List.of(
            "http://resource.geosciml.org/classifier/ics/ischart/Ordovician",
            "http://resource.geosciml.org/classifier/ics/ischart/Darriwilian"),
        conceptUris);
  }

  private Path fixturePath() throws URISyntaxException {
    return Path.of(
        Objects.requireNonNull(getClass().getClassLoader().getResource("chart-sample.ttl"))
            .toURI());
  }
}
