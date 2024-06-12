package org.gbif.vocabulary.persistence.dto;

import lombok.Data;

import org.gbif.vocabulary.model.LanguageRegion;

@Data
public class ParentDto {
  long key;
  String name;
  int depth;
  Long labelKey;
  LanguageRegion labelLanguage;
  String labelValue;
}
