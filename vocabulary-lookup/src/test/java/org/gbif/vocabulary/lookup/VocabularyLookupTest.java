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
package org.gbif.vocabulary.lookup;

import org.gbif.vocabulary.model.LanguageRegion;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link VocabularyLookup}. */
public class VocabularyLookupTest {

  private static final String TEST_VOCAB_FILE = "test-vocab.json";

  @Test
  public void loadVocabularyFromInputStreamTest() {
    VocabularyLookup lookup =
        VocabularyLookup.newBuilder()
            .from(
                Thread.currentThread().getContextClassLoader().getResourceAsStream(TEST_VOCAB_FILE))
            .build();
    assertNotNull(lookup);
  }

  @Test
  public void lookupTest() {
    VocabularyLookup vocabulary =
        VocabularyLookup.newBuilder()
            .from(
                Thread.currentThread().getContextClassLoader().getResourceAsStream(TEST_VOCAB_FILE))
            .build();

    Optional<LookupConcept> concept = vocabulary.lookup("February");
    assertTrue(concept.isPresent());
    assertEquals("February", concept.get().getConcept().getName());
    assertEquals(1, concept.get().getParents().size());
    assertTrue(concept.get().getParents().contains("January"));

    concept = vocabulary.lookup("Fev");
    assertTrue(concept.isPresent());
    assertEquals("February", concept.get().getConcept().getName());

    concept = vocabulary.lookup("Fév");
    assertTrue(concept.isPresent());
    assertEquals("February", concept.get().getConcept().getName());

    concept = vocabulary.lookup("ÉnERo");
    assertTrue(concept.isPresent());
    assertEquals("January", concept.get().getConcept().getName());
    assertEquals(0, concept.get().getParents().size());

    concept = vocabulary.lookup("eneiro.");
    assertFalse(concept.isPresent());

    concept = vocabulary.lookup("march");
    assertTrue(concept.isPresent());
    assertEquals(2, concept.get().getParents().size());
    assertEquals(Arrays.asList("February", "January"), concept.get().getParents());
  }

  @Test
  public void lookupWithLanguageTest() {
    VocabularyLookup vocabulary =
        VocabularyLookup.newBuilder()
            .from(
                Thread.currentThread().getContextClassLoader().getResourceAsStream(TEST_VOCAB_FILE))
            .build();

    assertFalse(vocabulary.lookup("Marzo").isPresent());
    assertFalse(vocabulary.lookup("Marzo", LanguageRegion.ENGLISH).isPresent());
    assertTrue(vocabulary.lookup("Marzo", LanguageRegion.GERMAN).isPresent());

    Optional<LookupConcept> concept = vocabulary.lookup("Marzo", LanguageRegion.SPANISH);
    assertTrue(concept.isPresent());
    assertEquals("March", concept.get().getConcept().getName());

    concept = vocabulary.lookup("Marzo", LanguageRegion.GERMAN);
    assertTrue(concept.isPresent());
    assertEquals("February", concept.get().getConcept().getName());
  }

  @Test
  public void lookupWithPrefiltersTest() {
    VocabularyLookup lookup =
        VocabularyLookup.newBuilder()
            .from(
                Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("LifeStage.json"))
            .withPrefilter(PreFilters.REMOVE_NUMERIC_PREFIX)
            .build();

    assertEquals("Adult", lookup.lookup("1325 adult").get().getConcept().getName());

    lookup =
        VocabularyLookup.newBuilder()
            .from(
                Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("LifeStage.json"))
            .withPrefilter(
                PreFilters.REMOVE_NUMERIC_PREFIX.andThen(
                    PreFilters.REMOVE_PARENTHESIS_CONTENT_SUFFIX))
            .build();

    assertEquals("Adult", lookup.lookup("1325 adult").get().getConcept().getName());
    assertEquals("Adult", lookup.lookup("1325 adult (dsgds)").get().getConcept().getName());
    assertEquals("Adult", lookup.lookup("adult (dsgds)").get().getConcept().getName());
  }
}
