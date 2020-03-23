package org.gbif.vocabulary.model;

import org.gbif.api.vocabulary.TranslationLanguage;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotBlank;

/** Defines the minimum fields that an vocabulary entity must have. */
public interface VocabularyEntity extends Auditable, Deprecable, Serializable {

  /** Unique identifier for persistence. */
  Long getKey();

  void setKey(Long key);

  /** Unique name. */
  @NotBlank
  String getName();

  void setName(String name);

  /** Representative label per language. */
  Map<TranslationLanguage, String> getLabel();

  void setLabel(Map<TranslationLanguage, String> label);

  /** Definition of the entity by language. */
  Map<TranslationLanguage, String> getDefinition();

  void setDefinition(Map<TranslationLanguage, String> definition);

  /** Additional external definitions. */
  List<URI> getExternalDefinitions();

  void setExternalDefinitions(List<URI> externalDefinitions);

  /** Notes for editorial purposes. */
  List<String> getEditorialNotes();

  void setEditorialNotes(List<String> editorialNotes);
}
