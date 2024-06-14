package org.gbif.vocabulary.model.search;

import org.gbif.vocabulary.model.LanguageRegion;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SuggestResult {
  private String name;
  private String label;
  private LanguageRegion labelLanguage;
  private List<Parent> parents = new ArrayList<>();

  @Data
  public static class Parent {
    private String name;
    private String label;
    private LanguageRegion labelLanguage;
  }
}
