package org.gbif.vocabulary.lookup;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.export.VocabularyExport;
import org.gbif.vocabulary.model.vocabulary.LanguageRegion;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests the {@link VocabularyLookup}. */
public class VocabularyLookupTest {

  private static final String TEST_VOCAB_FILE = "test-vocab.json";
  private static final String INVALID_VOCAB_FILE = "invalid-vocab.json";

  @Test
  public void loadVocabularyFromInputStreamTest() {
    VocabularyLookup lookup =
        VocabularyLookup.load(
            Thread.currentThread().getContextClassLoader().getResourceAsStream(TEST_VOCAB_FILE));
    assertNotNull(lookup);
  }

  @Test
  public void invalidVocabularyLoadTest() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            VocabularyLookup.load(
                Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(INVALID_VOCAB_FILE)));
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
            Thread.currentThread().getContextClassLoader().getResourceAsStream(TEST_VOCAB_FILE));

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
            Thread.currentThread().getContextClassLoader().getResourceAsStream(TEST_VOCAB_FILE));

    assertFalse(vocabulary.lookup("Marzo").isPresent());
    assertFalse(vocabulary.lookup("Marzo", LanguageRegion.ENGLISH).isPresent());
    assertFalse(vocabulary.lookup("Marzo", LanguageRegion.GERMAN).isPresent());

    Optional<Concept> concept = vocabulary.lookup("Marzo", LanguageRegion.SPANISH);
    assertTrue(concept.isPresent());
    assertEquals("March", concept.get().getName());

    concept = vocabulary.lookup("Marzo", LanguageRegion.FRENCH);
    assertTrue(concept.isPresent());
    assertEquals("February", concept.get().getName());
  }
}
