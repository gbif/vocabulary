package org.gbif.vocabulary.service;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.vocabulary.TranslationLanguage;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.export.VocabularyExport;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
public class ExportServiceTest extends MockServiceBaseTest {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  @Autowired private ExportService exportService;
  @MockBean private VocabularyService vocabularyService;
  @MockBean private ConceptService conceptService;

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
    Map<TranslationLanguage, String> labels = new HashMap<>();
    labels.put(TranslationLanguage.ENGLISH, "Label");
    labels.put(TranslationLanguage.SPANISH, "Etiqueta");
    c1.setLabel(labels);

    // alternative labels
    Map<TranslationLanguage, List<String>> alternativeLabels = new HashMap<>();
    alternativeLabels.put(TranslationLanguage.ENGLISH, Arrays.asList("label2", "label3", "label4"));
    alternativeLabels.put(TranslationLanguage.SPANISH, Arrays.asList("label5", "label6"));
    c1.setAlternativeLabels(alternativeLabels);

    // misspelt labels
    Map<TranslationLanguage, List<String>> misappliedLabels = new HashMap<>();
    misappliedLabels.put(TranslationLanguage.ENGLISH, Arrays.asList("labl2", "labl3", "labl4"));
    misappliedLabels.put(TranslationLanguage.SPANISH, Arrays.asList("labl5", "labl6"));
    c1.setMisappliedLabels(misappliedLabels);

    Concept c2 = new Concept();
    c2.setName("c2");
    c2.setVocabularyKey(vocabulary.getKey());
    c2.setLabel(Collections.singletonMap(TranslationLanguage.ENGLISH, "Label"));

    Concept c3 = new Concept();
    c3.setName("c3");
    c3.setVocabularyKey(vocabulary.getKey());
    c3.setLabel(Collections.singletonMap(TranslationLanguage.ENGLISH, "Label"));

    when(vocabularyService.getByName(vocabularyName)).thenReturn(vocabulary);
    when(conceptService.list(any(), any()))
        .thenReturn(new PagingResponse<>(0, 100, 3L, Arrays.asList(c1, c2, c3)));
  }
}
