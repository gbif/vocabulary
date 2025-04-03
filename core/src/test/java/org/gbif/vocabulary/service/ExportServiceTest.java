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

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Definition;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.UserRoles;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.export.Export;
import org.gbif.vocabulary.model.export.VocabularyExportView;
import org.gbif.vocabulary.persistence.mappers.VocabularyReleaseMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    VocabularyExportView v = new VocabularyExportView();
    Vocabulary voc = new Vocabulary();
    Definition definition =
        Definition.builder().language(LanguageRegion.ENGLISH).value("def").build();
    definition.setLanguage(LanguageRegion.ENGLISH);
    definition.setValue("v");
    voc.setDefinition(Collections.singletonList(definition));
    v.setVocabulary(voc);
    v.setDefinition(Collections.singletonMap(LanguageRegion.ENGLISH, "f"));

    ObjectMapper om = new ObjectMapper();
    System.out.println(om.writeValueAsString(v));

    final String vocabularyName = "vocab";
    mockVocabulary(vocabularyName);

    Path path = exportService.exportVocabulary(vocabularyName);

    assertNotNull(path);
    assertTrue(path.getFileName().toString().startsWith(vocabularyName));

    Export export = OBJECT_MAPPER.readValue(path.toFile(), Export.class);
    assertEquals(export.getVocabularyExport().getVocabulary().getName(), vocabularyName);
    assertEquals(3, export.getConceptExports().size());
    assertNotNull(export.getMetadata().getCreatedDate());

    export
        .getConceptExports()
        .forEach(
            c -> {
              if (c.getConcept().getName().equals("c1")) {
                assertEquals(2, c.getTags().size());
              }
            });
  }

  private void mockVocabulary(String vocabularyName) {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setKey(1L);
    vocabulary.setName(vocabularyName);

    Concept c1 = new Concept();
    c1.setKey(1L);
    c1.setName("c1");
    c1.setVocabularyKey(vocabulary.getKey());
    c1.setCreated(ZonedDateTime.now());

    // tags
    Tag tag1 = new Tag();
    tag1.setName("tag1");
    c1.getTags().add(tag1);
    Tag tag2 = new Tag();
    tag2.setName("tag2");
    c1.getTags().add(tag2);

    // labels
    List<Label> c1Labels = new ArrayList<>();
    c1Labels.add(Label.builder().language(LanguageRegion.ENGLISH).value("Label").build());
    c1Labels.add(Label.builder().language(LanguageRegion.SPANISH).value("Etiqueta").build());

    // alternative labels
    List<Label> c1AlternativeLabels = new ArrayList<>();
    c1AlternativeLabels.add(
        Label.builder().language(LanguageRegion.ENGLISH).value("label2").build());
    c1AlternativeLabels.add(
        Label.builder().language(LanguageRegion.ENGLISH).value("label3").build());
    c1AlternativeLabels.add(
        Label.builder().language(LanguageRegion.ENGLISH).value("label4").build());
    c1AlternativeLabels.add(
        Label.builder().language(LanguageRegion.SPANISH).value("label5").build());
    c1AlternativeLabels.add(
        Label.builder().language(LanguageRegion.SPANISH).value("label6").build());

    // hidden labels
    List<HiddenLabel> c1HiddenLabels = new ArrayList<>();
    c1HiddenLabels.add(HiddenLabel.builder().value("labl2").build());
    c1HiddenLabels.add(HiddenLabel.builder().value("labl3").build());
    c1HiddenLabels.add(HiddenLabel.builder().value("labl4").build());
    c1HiddenLabels.add(HiddenLabel.builder().value("labl5").build());
    c1HiddenLabels.add(HiddenLabel.builder().value("labl6").build());

    Concept c2 = new Concept();
    c2.setKey(2L);
    c2.setName("c2");
    c2.setVocabularyKey(vocabulary.getKey());
    List<Label> c2Labels = new ArrayList<>();
    c2Labels.add(Label.builder().language(LanguageRegion.ENGLISH).value("Label").build());

    Concept c3 = new Concept();
    c3.setKey(3L);
    c3.setName("c3");
    c3.setVocabularyKey(vocabulary.getKey());
    List<Label> c3Labels = new ArrayList<>();
    c3Labels.add(Label.builder().language(LanguageRegion.ENGLISH).value("Label").build());

    when(vocabularyService.getByName(vocabularyName)).thenReturn(vocabulary);
    when(conceptService.list(any(), any()))
        .thenReturn(new PagingResponse<>(0, 100, 3L, Arrays.asList(c1, c2, c3)));
    when(conceptService.listLabels(c1.getKey(), null)).thenReturn(c1Labels);
    when(conceptService.listAlternativeLabels(c1.getKey(), null, new PagingRequest(0, 1000)))
        .thenReturn(
            new PagingResponse<>(
                0L,
                c1AlternativeLabels.size(),
                (long) c1AlternativeLabels.size(),
                c1AlternativeLabels));
    when(conceptService.listHiddenLabels(c1.getKey(), null, new PagingRequest(0, 1000)))
        .thenReturn(
            new PagingResponse<>(
                0L, c1HiddenLabels.size(), (long) c1HiddenLabels.size(), c1HiddenLabels));
    when(conceptService.listLabels(c2.getKey(), null)).thenReturn(c2Labels);
    when(conceptService.listAlternativeLabels(c2.getKey(), null, new PagingRequest(0, 1000)))
        .thenReturn(new PagingResponse<>(0L, 0, 0L, new ArrayList<>()));
    when(conceptService.listHiddenLabels(c2.getKey(), null, new PagingRequest(0, 1000)))
        .thenReturn(new PagingResponse<>(0L, 0, 0L, new ArrayList<>()));
    when(conceptService.listLabels(c3.getKey(), null)).thenReturn(c3Labels);
    when(conceptService.listAlternativeLabels(c3.getKey(), null, new PagingRequest(0, 1000)))
        .thenReturn(new PagingResponse<>(0L, 0, 0L, new ArrayList<>()));
    when(conceptService.listHiddenLabels(c3.getKey(), null, new PagingRequest(0, 1000)))
        .thenReturn(new PagingResponse<>(0L, 0, 0L, new ArrayList<>()));
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
