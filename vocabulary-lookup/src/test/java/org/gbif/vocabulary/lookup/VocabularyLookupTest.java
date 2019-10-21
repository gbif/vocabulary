package org.gbif.vocabulary.lookup;

import org.gbif.vocabulary.model.export.VocabularyExport;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    InputStream in = VocabularyDownloader.downloadVocabulary("http://localhost:8080", "ada");

    VocabularyExport export =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .readValue(in, VocabularyExport.class);
    assertNotNull(export);
  }
}
