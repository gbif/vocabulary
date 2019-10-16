package org.gbif.vocabulary.model;

import org.gbif.api.vocabulary.Language;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests {@link Concept}. */
public class ConceptTest {

  @Test
  public void equalityTest() {
    Concept c1 = new Concept();
    c1.setKey(1);
    c1.setName("n1");
    c1.setVocabularyKey(1);
    c1.setLabel(Collections.singletonMap(Language.ENGLISH, "label"));
    c1.setParentKey(2);
    c1.setReplacedByKey(1);
    c1.setAlternativeLabels(Collections.singletonMap(Language.ENGLISH, Arrays.asList("alt")));
    c1.setMisappliedLabels(Collections.singletonMap(Language.ENGLISH, Arrays.asList("misspelt")));
    c1.setDefinition(Collections.singletonMap(Language.ENGLISH, "def"));
    c1.setSameAsUris(Collections.singletonList(URI.create("http://test.com")));
    c1.setEditorialNotes(Arrays.asList("n1", "n2"));
    c1.setExternalDefinitions(Collections.singletonList(URI.create("http://test.com")));
    c1.setCreated(LocalDateTime.now());
    c1.setDeleted(LocalDateTime.now());
    c1.setDeprecated(LocalDateTime.now());

    Concept c2 = new Concept();
    c2.setKey(c1.getKey());
    c2.setName(c1.getName());
    c2.setVocabularyKey(c1.getVocabularyKey());
    c2.setLabel(c1.getLabel());
    c2.setParentKey(c1.getParentKey());
    c2.setReplacedByKey(c1.getReplacedByKey());
    c2.setAlternativeLabels(c1.getAlternativeLabels());
    c2.setMisappliedLabels(c1.getMisappliedLabels());
    c2.setDefinition(c1.getDefinition());
    c2.setSameAsUris(c1.getSameAsUris());
    c2.setEditorialNotes(c1.getEditorialNotes());
    c2.setExternalDefinitions(c1.getExternalDefinitions());
    c2.setCreated(c1.getCreated());
    c2.setDeleted(c1.getDeleted());
    c2.setDeprecated(c1.getDeprecated());

    assertTrue(c1.lenientEquals(c2));

    c1.setModified(LocalDateTime.now());
    assertTrue(c1.lenientEquals(c2));
    assertNotEquals(c1, c2);
  }
}
