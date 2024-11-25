package org.gbif.vocabulary.persistence.dto;

import lombok.Data;
import org.gbif.vocabulary.model.LanguageRegion;

@Data
public class LookupDto {

  private long key;
  private String name;
  private String label;
  private LanguageRegion labelLang;
  private String altLabel;
  private LanguageRegion altLabelLang;
  private String hiddenLabel;
}
