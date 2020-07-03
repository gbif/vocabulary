package org.gbif.vocabulary.model;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.gbif.vocabulary.model.enums.LanguageRegion;
import org.gbif.vocabulary.model.utils.PostPersist;
import org.gbif.vocabulary.model.utils.PrePersist;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

/** Defines the minimum fields that an vocabulary entity must have. */
public interface VocabularyEntity extends Auditable, Deprecable, Serializable {

  /** Unique identifier for persistence. */
  @Null(groups = {PrePersist.class})
  @NotNull(groups = {PostPersist.class})
  Long getKey();

  void setKey(Long key);

  /** Unique name. */
  @NotBlank
  String getName();

  void setName(String name);

  /** Representative label per language. */
  Map<LanguageRegion, String> getLabel();

  void setLabel(Map<LanguageRegion, String> label);

  /** Definition of the entity by language. */
  Map<LanguageRegion, String> getDefinition();

  void setDefinition(Map<LanguageRegion, String> definition);

  /** Additional external definitions. */
  List<URI> getExternalDefinitions();

  void setExternalDefinitions(List<URI> externalDefinitions);

  /** Notes for editorial purposes. */
  List<String> getEditorialNotes();

  void setEditorialNotes(List<String> editorialNotes);
}
