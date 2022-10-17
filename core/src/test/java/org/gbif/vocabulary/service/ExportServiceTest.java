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
package org.gbif.vocabulary.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.UserRoles;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.export.VocabularyExport;
import org.gbif.vocabulary.persistence.mappers.VocabularyReleaseMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    c1.getLabels()
        .add(
            Label.builder()
                .entityKey(c1.getKey())
                .language(LanguageRegion.ENGLISH)
                .value("Label")
                .build());
    c1.getLabels()
        .add(
            Label.builder()
                .entityKey(c1.getKey())
                .language(LanguageRegion.SPANISH)
                .value("Etiqueta")
                .build());

    // alternative labels
    c1.getAlternativeLabels()
        .add(
            Label.builder()
                .entityKey(c1.getKey())
                .language(LanguageRegion.ENGLISH)
                .value("label2")
                .build());
    c1.getAlternativeLabels()
        .add(
            Label.builder()
                .entityKey(c1.getKey())
                .language(LanguageRegion.ENGLISH)
                .value("label3")
                .build());
    c1.getAlternativeLabels()
        .add(
            Label.builder()
                .entityKey(c1.getKey())
                .language(LanguageRegion.ENGLISH)
                .value("label4")
                .build());
    c1.getAlternativeLabels()
        .add(
            Label.builder()
                .entityKey(c1.getKey())
                .language(LanguageRegion.SPANISH)
                .value("label5")
                .build());
    c1.getAlternativeLabels()
        .add(
            Label.builder()
                .entityKey(c1.getKey())
                .language(LanguageRegion.SPANISH)
                .value("label6")
                .build());

    // hidden labels
    c1.getHiddenLabels().add(HiddenLabel.builder().entityKey(c1.getKey()).value("labl2").build());
    c1.getHiddenLabels().add(HiddenLabel.builder().entityKey(c1.getKey()).value("labl3").build());
    c1.getHiddenLabels().add(HiddenLabel.builder().entityKey(c1.getKey()).value("labl4").build());
    c1.getHiddenLabels().add(HiddenLabel.builder().entityKey(c1.getKey()).value("labl5").build());
    c1.getHiddenLabels().add(HiddenLabel.builder().entityKey(c1.getKey()).value("labl6").build());

    Concept c2 = new Concept();
    c2.setName("c2");
    c2.setVocabularyKey(vocabulary.getKey());
    c2.setLabels(
        Collections.singletonList(
            Label.builder()
                .entityKey(c2.getKey())
                .language(LanguageRegion.ENGLISH)
                .value("Label")
                .build()));

    Concept c3 = new Concept();
    c3.setName("c3");
    c3.setVocabularyKey(vocabulary.getKey());
    c3.setLabels(
        Collections.singletonList(
            Label.builder()
                .entityKey(c3.getKey())
                .language(LanguageRegion.ENGLISH)
                .value("Label")
                .build()));

    when(vocabularyService.getByName(vocabularyName)).thenReturn(vocabulary);
    when(conceptService.list(any(), any()))
        .thenReturn(new PagingResponse<>(0, 100, 3L, Arrays.asList(c1, c2, c3)));
  }

  @WithMockUser
  @Test
  public void unauthorizedReleaseTest() {
    assertThrows(AccessDeniedException.class, () -> exportService.releaseVocabulary(null));
  }

  @WithMockUser(authorities = UserRoles.VOCABULARY_EDITOR)
  @Test
  public void forbiddenReleaseTest() {
    assertThrows(AccessDeniedException.class, () -> exportService.releaseVocabulary(null));
  }
}
