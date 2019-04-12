package org.gbif.vocabulary.model;

import org.gbif.api.vocabulary.Language;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class VocabularyTest {

  @Test
  public void lenientEqualsTest() {
    Vocabulary v1 = new Vocabulary();
    v1.setName("v1");
    v1.setNamespace("ns");
    v1.setLabel(Collections.singletonMap(Language.ENGLISH, "label"));
    v1.setDefinition(Collections.singletonMap(Language.ENGLISH, "def"));
    v1.setEditorialNotes(Arrays.asList("n1", "n2"));
    v1.setExternalDefinitions(Collections.singletonList(URI.create("http://test.com")));

    Vocabulary v2 = new Vocabulary();
    v2.setName("v1");
    v2.setNamespace("ns");
    v2.setLabel(Collections.singletonMap(Language.ENGLISH, "label"));
    v2.setDefinition(Collections.singletonMap(Language.ENGLISH, "def"));
    v2.setEditorialNotes(Arrays.asList("n1", "n2"));
    v2.setExternalDefinitions(Collections.singletonList(URI.create("http://test.com")));

    assertTrue(v1.lenientEquals(v2));
  }
}
