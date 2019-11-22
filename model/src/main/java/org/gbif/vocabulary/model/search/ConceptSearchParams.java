package org.gbif.vocabulary.model.search;

import java.io.Serializable;

/** Holder for the concept search parameters. */
public class ConceptSearchParams implements Serializable {

  private final String query;
  private final Integer vocabularyKey;
  private final Integer parentKey;
  private final Integer replacedByKey;
  private final String name;
  private final Boolean deprecated;
  private final Integer key;

  private ConceptSearchParams(
      String query,
      Integer vocabularyKey,
      Integer parentKey,
      Integer replacedByKey,
      String name,
      Boolean deprecated,
      Integer key) {
    this.query = query;
    this.vocabularyKey = vocabularyKey;
    this.parentKey = parentKey;
    this.replacedByKey = replacedByKey;
    this.name = name;
    this.deprecated = deprecated;
    this.key = key;
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

  public Boolean getDeprecated() {
    return deprecated;
  }

  public Integer getKey() {
    return key;
  }

  /**
   * Creates a builder to create instances of {@link ConceptSearchParams}.
   *
   * @return {@link Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  public static ConceptSearchParams empty() {
    return builder().build();
  }

  public static class Builder {
    private String query;
    private Integer vocabularyKey;
    private Integer parentKey;
    private Integer replacedByKey;
    private Boolean deprecated;
    private String name;
    private Integer key;

    private Builder() {}

    public Builder query(String query) {
      this.query = query;
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

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder deprecated(Boolean deprecated) {
      this.deprecated = deprecated;
      return this;
    }

    public Builder key(Integer key) {
      this.key = key;
      return this;
    }

    public ConceptSearchParams build() {
      return new ConceptSearchParams(
          query, vocabularyKey, parentKey, replacedByKey, name, deprecated, key);
    }
  }
}
