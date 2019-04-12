package org.gbif.vocabulary.model;

import org.gbif.api.vocabulary.Language;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests {@link Vocabulary}. */
public class VocabularyTest {

  @Test
  public void equalityTest() {
    Vocabulary v1 = new Vocabulary();
    v1.setKey(1);
    v1.setName("v1");
    v1.setNamespace("ns");
    v1.setLabel(Collections.singletonMap(Language.ENGLISH, "label"));
    v1.setDefinition(Collections.singletonMap(Language.ENGLISH, "def"));
    v1.setEditorialNotes(Arrays.asList("n1", "n2"));
    v1.setExternalDefinitions(Collections.singletonList(URI.create("http://test.com")));
    v1.setDeleted(LocalDateTime.now());
    v1.setDeprecated(LocalDateTime.now());

    Vocabulary v2 = new Vocabulary();
    v2.setKey(v1.getKey());
    v2.setName(v1.getName());
    v2.setNamespace(v1.getNamespace());
    v2.setLabel(v1.getLabel());
    v2.setDefinition(v1.getDefinition());
    v2.setEditorialNotes(v1.getEditorialNotes());
    v2.setExternalDefinitions(v1.getExternalDefinitions());
    v2.setDeleted(v1.getDeleted());
    v2.setDeprecated(v1.getDeprecated());

    assertTrue(v1.lenientEquals(v2));

    v1.setModified(LocalDateTime.now());
    assertTrue(v1.lenientEquals(v2));
    assertNotEquals(v1, v2);
  }
}
