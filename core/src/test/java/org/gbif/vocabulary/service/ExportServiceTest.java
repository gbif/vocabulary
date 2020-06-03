package org.gbif.vocabulary.service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.enums.LanguageRegion;
import org.gbif.vocabulary.model.export.VocabularyExport;
import org.gbif.vocabulary.persistence.mappers.VocabularyReleaseMapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

/** Tests the {@link ExportService}. */
public class ExportServiceTest extends MockServiceBaseTest {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  @Autowired private ExportService exportService;
  @MockBean private VocabularyService vocabularyService;
  @MockBean private ConceptService conceptService;
  @Autowired private VocabularyReleaseMapper vocabularyReleaseMapper;

  @Test
  public void exportVocabularyTest() throws IOException {
    final String vocabularyName = "vocab";
    mockVocabulary(vocabularyName);

    Path path = exportService.exportVocabulary(vocabularyName);

    assertNotNull(path);
    assertTrue(path.getFileName().toString().startsWith(vocabularyName));

    VocabularyExport export = OBJECT_MAPPER.readValue(path.toFile(), VocabularyExport.class);
    assertEquals(export.getVocabulary().getName(), vocabularyName);
    assertEquals(3, export.getConcepts().size());
    assertNotNull(export.getMetadata().getCreatedDate());
  }

  private void mockVocabulary(String vocabularyName) {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(vocabularyName);

    Concept c1 = new Concept();
    c1.setName("c1");
    c1.setVocabularyKey(vocabulary.getKey());
    c1.setCreated(LocalDateTime.now());

    // labels
    Map<LanguageRegion, String> labels = new HashMap<>();
    labels.put(LanguageRegion.ENGLISH, "Label");
    labels.put(LanguageRegion.SPANISH, "Etiqueta");
    c1.setLabel(labels);

    // alternative labels
    Map<LanguageRegion, List<String>> alternativeLabels = new HashMap<>();
    alternativeLabels.put(LanguageRegion.ENGLISH, Arrays.asList("label2", "label3", "label4"));
    alternativeLabels.put(LanguageRegion.SPANISH, Arrays.asList("label5", "label6"));
    c1.setAlternativeLabels(alternativeLabels);

    // misapplied labels
    Map<LanguageRegion, List<String>> misappliedLabels = new HashMap<>();
    misappliedLabels.put(LanguageRegion.ENGLISH, Arrays.asList("labl2", "labl3", "labl4"));
    misappliedLabels.put(LanguageRegion.SPANISH, Arrays.asList("labl5", "labl6"));
    c1.setMisappliedLabels(misappliedLabels);

    Concept c2 = new Concept();
    c2.setName("c2");
    c2.setVocabularyKey(vocabulary.getKey());
    c2.setLabel(Collections.singletonMap(LanguageRegion.ENGLISH, "Label"));

    Concept c3 = new Concept();
    c3.setName("c3");
    c3.setVocabularyKey(vocabulary.getKey());
    c3.setLabel(Collections.singletonMap(LanguageRegion.ENGLISH, "Label"));

    when(vocabularyService.getByName(vocabularyName)).thenReturn(vocabulary);
    when(conceptService.list(any(), any()))
        .thenReturn(new PagingResponse<>(0, 100, 3L, Arrays.asList(c1, c2, c3)));
  }

  @Test
  public void releaseVocabularyTest() {
    // releases should be disabled in tests
    Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> exportService.releaseVocabulary("test", "1.0", Paths.get("test"), "user"));
  }
}
