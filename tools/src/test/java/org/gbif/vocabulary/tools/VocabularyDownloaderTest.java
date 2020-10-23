package org.gbif.vocabulary.tools;

import java.io.IOException;
import java.io.InputStream;

import org.gbif.vocabulary.model.export.VocabularyExport;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class VocabularyDownloaderTest {

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

}
