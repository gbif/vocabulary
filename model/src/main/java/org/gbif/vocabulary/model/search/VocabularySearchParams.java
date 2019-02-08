package org.gbif.vocabulary.model.search;


import org.gbif.api.model.common.paging.Pageable;

import java.io.Serializable;

/** Holder for the vocabulary search parameters. */
public class VocabularySearchParams implements Serializable {

  private final String query;
  private final String name;
  private final String namespace;
  private final Pageable page;

  private VocabularySearchParams(String query, String name, String namespace, Pageable page) {
    this.query = query;
    this.name = name;
    this.namespace = namespace;
    this.page = page;
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

  public Pageable getPage() {
    return page;
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
    private Pageable page;

    Builder query(String query) {
      this.query = query;
      return this;
    }

    Builder name(String name) {
      this.name = name;
      return this;
    }

    Builder namespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    Builder page(Pageable page) {
      this.page = page;
      return this;
    }

    VocabularySearchParams build() {
      return new VocabularySearchParams(query, name, namespace, page);
    }
  }
}
