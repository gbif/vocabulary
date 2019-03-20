package org.gbif.vocabulary.model;

import org.gbif.api.vocabulary.Language;

import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/** Defines the minimum fields that an vocabulary entity must have. */
public interface VocabularyEntity extends Auditable, Deprecable {

  Integer getKey();

  void setKey(Integer key);

  @NotNull
  String getName();

  void setName(String name);

  Map<Language, String> getLabel();

  void setLabel(Map<Language, String> label);

  Map<Language, String> getDefinition();

  void setDefinition(Map<Language, String> definition);

  List<URI> getExternalDefinitions();

  void setExternalDefinitions(List<URI> externalDefinitions);

  List<String> getEditorialNotes();

  void setEditorialNotes(List<String> editorialNotes);
}
