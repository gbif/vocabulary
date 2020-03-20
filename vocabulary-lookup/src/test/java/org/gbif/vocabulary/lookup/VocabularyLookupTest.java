package org.gbif.vocabulary.lookup;

import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.export.VocabularyExport;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link VocabularyLookup}. */
public class VocabularyLookupTest {

  private static final String TEST_FILE = "test-vocab.json";

  @Test
  public void loadVocabularyFromInputStreamTest() {
    VocabularyLookup lookup =
        VocabularyLookup.load(
            Thread.currentThread().getContextClassLoader().getResourceAsStream(TEST_FILE));
    assertNotNull(lookup);
  }

  @Disabled("manual test")
  @Test
  public void loadVocabularyFromApiUrl() throws IOException {
    InputStream in = VocabularyDownloader.downloadVocabulary("http://localhost:8080", "a");

    VocabularyExport export =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .readValue(in, VocabularyExport.class);
    assertNotNull(export);
  }

  @Test
  public void lookupTest() {
    VocabularyLookup vocabulary =
        VocabularyLookup.load(
            Thread.currentThread().getContextClassLoader().getResourceAsStream(TEST_FILE));

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
    assertTrue(concept.isPresent());
    assertEquals("January", concept.get().getName());
  }

  @Test
  public void lookupWithLanguageTest() {
    VocabularyLookup vocabulary =
        VocabularyLookup.load(
            Thread.currentThread().getContextClassLoader().getResourceAsStream(TEST_FILE));

    assertFalse(vocabulary.lookup("Marzo").isPresent());
    assertFalse(vocabulary.lookup("Marzo", Language.ENGLISH).isPresent());
    assertFalse(vocabulary.lookup("Marzo", Language.GERMAN).isPresent());

    Optional<Concept> concept = vocabulary.lookup("Marzo", Language.SPANISH);
    assertTrue(concept.isPresent());
    assertEquals("March", concept.get().getName());

    concept = vocabulary.lookup("Marzo", Language.FRENCH);
    assertTrue(concept.isPresent());
    assertEquals("February", concept.get().getName());
  }
}
