package org.gbif.vocabulary.model;

import org.gbif.api.vocabulary.Language;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConceptTest {

  @Test
  public void lenientEqualsTest() {
    Concept c1 = new Concept();
    c1.setName("n1");
    c1.setVocabularyKey(1);
    c1.setLabel(Collections.singletonMap(Language.ENGLISH, "label"));
    c1.setParentKey(2);
    c1.setAlternativeLabels(Collections.singletonMap(Language.ENGLISH, Arrays.asList("alt")));
    c1.setMisspeltLabels(Collections.singletonMap(Language.ENGLISH, Arrays.asList("misspelt")));
    c1.setDefinition(Collections.singletonMap(Language.ENGLISH, "def"));
    c1.setSameAsUris(Collections.singletonList(URI.create("http://test.com")));
    c1.setEditorialNotes(Arrays.asList("n1", "n2"));
    c1.setExternalDefinitions(Collections.singletonList(URI.create("http://test.com")));

    Concept c2 = new Concept();
    c2.setName("n1");
    c2.setVocabularyKey(1);
    c2.setLabel(Collections.singletonMap(Language.ENGLISH, "label"));
    c2.setParentKey(2);
    c2.setAlternativeLabels(Collections.singletonMap(Language.ENGLISH, Arrays.asList("alt")));
    c2.setMisspeltLabels(Collections.singletonMap(Language.ENGLISH, Arrays.asList("misspelt")));
    c2.setDefinition(Collections.singletonMap(Language.ENGLISH, "def"));
    c2.setSameAsUris(Collections.singletonList(URI.create("http://test.com")));
    c2.setEditorialNotes(Arrays.asList("n1", "n2"));
    c2.setExternalDefinitions(Collections.singletonList(URI.create("http://test.com")));

    assertTrue(c1.lenientEquals(c2));
  }
}
