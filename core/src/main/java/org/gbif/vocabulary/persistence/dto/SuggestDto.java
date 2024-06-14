package org.gbif.vocabulary.persistence.dto;

import lombok.Data;
import org.gbif.vocabulary.model.LanguageRegion;

import java.util.List;

@Data
public class SuggestDto {

  private long key;
  private LanguageRegion langParam;
  private LanguageRegion fallbackLangParam;
  private String name;
  private String label;
  private LanguageRegion labelLang;
  private List<ParentDto> parentDtos;
}
