package org.gbif.vocabulary.model.search;

import java.io.Serializable;

/** Holder for the concept search parameters. */
public class ConceptSearchParams implements Serializable {

  private final String query;
  private final Long vocabularyKey;
  private final Long parentKey;
  private final Long replacedByKey;
  private final String name;
  private final Boolean deprecated;
  private final Long key;
  private Boolean hasParent;
  private Boolean hasReplacement;

  private ConceptSearchParams(
      String query,
      Long vocabularyKey,
      Long parentKey,
      Long replacedByKey,
      String name,
      Boolean deprecated,
      Long key,
      Boolean hasParent,
      Boolean hasReplacement) {
    this.query = query;
    this.vocabularyKey = vocabularyKey;
    this.parentKey = parentKey;
    this.replacedByKey = replacedByKey;
    this.name = name;
    this.deprecated = deprecated;
    this.key = key;
    this.hasParent = hasParent;
    this.hasReplacement = hasReplacement;
  }

  public String getQuery() {
    return query;
  }

  public Long getVocabularyKey() {
    return vocabularyKey;
  }

  public Long getParentKey() {
    return parentKey;
  }

  public Long getReplacedByKey() {
    return replacedByKey;
  }

  public String getName() {
    return name;
  }

  public Boolean getDeprecated() {
    return deprecated;
  }

  public Long getKey() {
    return key;
  }

  public Boolean getHasParent() {
    return hasParent;
  }

  public Boolean getHasReplacement() {
    return hasReplacement;
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
    private Long vocabularyKey;
    private Long parentKey;
    private Long replacedByKey;
    private Boolean deprecated;
    private String name;
    private Long key;
    private Boolean hasParent;
    private Boolean hasReplacement;

    private Builder() {}

    public Builder query(String query) {
      this.query = query;
      return this;
    }

    public Builder vocabularyKey(Long vocabularyKey) {
      this.vocabularyKey = vocabularyKey;
      return this;
    }

    public Builder parentKey(Long parentKey) {
      this.parentKey = parentKey;
      return this;
    }

    public Builder replacedByKey(Long replacedByKey) {
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

    public Builder key(Long key) {
      this.key = key;
      return this;
    }

    public Builder hasParent(Boolean hasParent) {
      this.hasParent = hasParent;
      return this;
    }

    public Builder hasReplacement(Boolean hasReplacement) {
      this.hasReplacement = hasReplacement;
      return this;
    }

    public ConceptSearchParams build() {
      return new ConceptSearchParams(
          query,
          vocabularyKey,
          parentKey,
          replacedByKey,
          name,
          deprecated,
          key,
          hasParent,
          hasReplacement);
    }
  }
}
