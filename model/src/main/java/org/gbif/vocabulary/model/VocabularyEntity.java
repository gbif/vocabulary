package org.gbif.vocabulary.model;

import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.vocabulary.TranslationLanguage;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

/** Defines the minimum fields that an vocabulary entity must have. */
public interface VocabularyEntity extends Auditable, Deprecable, Serializable {

  /** Unique identifier for persistence. */
  @Nullable
  @Null(groups = {PrePersist.class})
  @NotNull(groups = {PostPersist.class})
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
