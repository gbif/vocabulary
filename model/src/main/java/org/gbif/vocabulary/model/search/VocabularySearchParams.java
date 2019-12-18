package org.gbif.vocabulary.model.search;

import java.io.Serializable;

/** Holder for the vocabulary search parameters. */
public class VocabularySearchParams implements Serializable {

  private final String query;
  private final String name;
  private final String namespace;
  private final Boolean deprecated;
  private final Long key;

  private VocabularySearchParams(
      String query, String name, String namespace, Boolean deprecated, Long key) {
    this.query = query;
    this.name = name;
    this.namespace = namespace;
    this.deprecated = deprecated;
    this.key = key;
  }

  public String getQuery() {
    return query;
  }

  public String getName() {
    return name;
  }

  public String getNamespace() {
    return namespace;
  }

  public Boolean getDeprecated() {
    return deprecated;
  }

  public Long getKey() {
    return key;
  }

  /**
   * Creates a builder to create instances of {@link VocabularySearchParams}.
   *
   * @return {@link Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  public static VocabularySearchParams empty() {
    return builder().build();
  }

  public static class Builder {
    private String query;
    private String name;
    private String namespace;
    private Boolean deprecated;
    private Long key;

    private Builder() {}

    public Builder query(String query) {
      this.query = query;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder namespace(String namespace) {
      this.namespace = namespace;
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

    public VocabularySearchParams build() {
      return new VocabularySearchParams(query, name, namespace, deprecated, key);
    }
  }
}
