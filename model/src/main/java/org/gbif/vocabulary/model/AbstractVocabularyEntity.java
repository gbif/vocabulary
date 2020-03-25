package org.gbif.vocabulary.model;

import org.gbif.vocabulary.model.enums.LanguageRegion;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotBlank;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for {@link VocabularyEntity} to provide a default implementation of {@link
 * VocabularyEntity}.
 */
@Data
public abstract class AbstractVocabularyEntity implements VocabularyEntity {

  Long key;
  @NotBlank String name;
  Map<LanguageRegion, String> label = new EnumMap<>(LanguageRegion.class);
  Map<LanguageRegion, String> definition = new EnumMap<>(LanguageRegion.class);
  List<URI> externalDefinitions = new ArrayList<>();
  List<String> editorialNotes = new ArrayList<>();

  // deprecation fields
  Long replacedByKey;
  LocalDateTime deprecated;
  String deprecatedBy;

  // audit fields
  LocalDateTime created;
  String createdBy;
  LocalDateTime modified;
  String modifiedBy;
  LocalDateTime deleted;
}
