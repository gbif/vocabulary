package org.gbif.vocabulary.model.search;

import java.io.Serializable;

/** Holder for the concept search parameters. */
public class ConceptSearchParams implements Serializable {

  private final String query;
  private Integer vocabularyKey;
  private Integer parentKey;
  private Integer replacedByKey;
  private final String name;

  private ConceptSearchParams(
      String query,
      Integer vocabularyKey,
      Integer parentKey,
      Integer replacedByKey,
      String name) {
    this.query = query;
    this.vocabularyKey = vocabularyKey;
    this.parentKey = parentKey;
    this.replacedByKey = replacedByKey;
    this.name = name;
  }

  public String getQuery() {
    return query;
  }

  public Integer getVocabularyKey() {
    return vocabularyKey;
  }

  public Integer getParentKey() {
    return parentKey;
  }

  public Integer getReplacedByKey() {
    return replacedByKey;
  }

  public String getName() {
    return name;
  }

  /**
   * Creates a builder to create instances of {@link ConceptSearchParams}.
   *
   * @return {@link Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  private static class Builder {
    private String query;
    private Integer vocabularyKey;
    private Integer parentKey;
    private Integer replacedByKey;
    private String name;

    public Builder query(String query) {
      this.query = query;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder vocabularyKey(Integer vocabularyKey) {
      this.vocabularyKey = vocabularyKey;
      return this;
    }

    public Builder parentKey(Integer parentKey) {
      this.parentKey = parentKey;
      return this;
    }

    public Builder replacedByKey(Integer replacedByKey) {
      this.replacedByKey = replacedByKey;
      return this;
    }

    public ConceptSearchParams build() {
      return new ConceptSearchParams(query, vocabularyKey, parentKey, replacedByKey, name);
    }
  }
}
