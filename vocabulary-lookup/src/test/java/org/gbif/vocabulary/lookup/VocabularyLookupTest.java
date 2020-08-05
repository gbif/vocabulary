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
package org.gbif.vocabulary.lookup;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.enums.LanguageRegion;
import org.gbif.vocabulary.model.export.VocabularyExport;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link VocabularyLookup}. */
public class VocabularyLookupTest {

  private static final String TEST_VOCAB_FILE = "test-vocab.json";
  private static final String INVALID_VOCAB_FILE = "invalid-vocab.json";

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
  public void invalidVocabularyLoadTest() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            VocabularyLookup.newBuilder()
                .from(
                    Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(INVALID_VOCAB_FILE))
                .build());
  }

  @Disabled("manual test")
  @Test
  public void loadVocabularyFromApiUrl() throws IOException {
    InputStream in =
        VocabularyDownloader.downloadLatestVocabularyVersion(
            "http://api.gbif-dev.org/v1/", "LifeStage");

    VocabularyExport export =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .readValue(in, VocabularyExport.class);
    assertNotNull(export);
  }

  @Test
  public void lookupTest() {
    VocabularyLookup vocabulary =
        VocabularyLookup.newBuilder()
            .from(
                Thread.currentThread().getContextClassLoader().getResourceAsStream(TEST_VOCAB_FILE))
            .build();

    Optional<Concept> concept = vocabulary.lookup("February");
    assertTrue(concept.isPresent());
    assertEquals("February", concept.get().getName());

    concept = vocabulary.lookup("Fev");
    assertTrue(concept.isPresent());
    assertEquals("February", concept.get().getName());

    concept = vocabulary.lookup("Fév");
    assertTrue(concept.isPresent());
    assertEquals("February", concept.get().getName());

    concept = vocabulary.lookup("ÉnERo");
    assertTrue(concept.isPresent());
    assertEquals("January", concept.get().getName());

    concept = vocabulary.lookup("eneiro.");
    assertFalse(concept.isPresent());
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

    Optional<Concept> concept = vocabulary.lookup("Marzo", LanguageRegion.SPANISH);
    assertTrue(concept.isPresent());
    assertEquals("March", concept.get().getName());

    concept = vocabulary.lookup("Marzo", LanguageRegion.GERMAN);
    assertTrue(concept.isPresent());
    assertEquals("February", concept.get().getName());
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

    assertEquals("Adult", lookup.lookup("1325 adult").get().getName());

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

    assertEquals("Adult", lookup.lookup("1325 adult").get().getName());
    assertEquals("Adult", lookup.lookup("1325 adult (dsgds)").get().getName());
    assertEquals("Adult", lookup.lookup("adult (dsgds)").get().getName());
  }
}
