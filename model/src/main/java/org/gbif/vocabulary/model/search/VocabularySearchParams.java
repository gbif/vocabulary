package org.gbif.vocabulary.model.search;

import java.io.Serializable;

/** Holder for the vocabulary search parameters. */
public class VocabularySearchParams implements Serializable {

  private final String query;
  private final String name;
  private final String namespace;

  private VocabularySearchParams(String query, String name, String namespace) {
    this.query = query;
    this.name = name;
    this.namespace = namespace;
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

  /**
   * Creates a builder to create instances of {@link VocabularySearchParams}.
   *
   * @return {@link Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  private static class Builder {
    private String query;
    private String name;
    private String namespace;

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

    public VocabularySearchParams build() {
      return new VocabularySearchParams(query, name, namespace);
    }
  }
}
