package org.gbif.vocabulary.model.search;

import lombok.Data;
import org.gbif.vocabulary.model.LanguageRegion;

@Data
public class LookupResult {

  private String conceptName;
  private String conceptLink;
  private String matchedLabel;
  private LanguageRegion matchedLabelLanguage;
  private String matchedAlternativeLabel;
  private LanguageRegion matchedAlternativeLabelLanguage;
  private String matchedHiddenLabel;
}
