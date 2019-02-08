package org.gbif.vocabulary.model.search;

import org.gbif.api.model.common.paging.Pageable;

import java.io.Serializable;

/** Holder for the concept search parameters. */
public class ConceptSearchParams implements Serializable {

  private final String query;
  private Integer vocabularyKey;
  private Integer parentKey;
  private Integer replacedByKey;
  private final String name;
  private final Pageable page;

  private ConceptSearchParams(
      String query,
      Integer vocabularyKey,
      Integer parentKey,
      Integer replacedByKey,
      String name,
      Pageable page) {
    this.query = query;
    this.vocabularyKey = vocabularyKey;
    this.parentKey = parentKey;
    this.replacedByKey = replacedByKey;
    this.name = name;
    this.page = page;
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

  public Pageable getPage() {
    return page;
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
    private Pageable page;

    Builder query(String query) {
      this.query = query;
      return this;
    }

    Builder name(String name) {
      this.name = name;
      return this;
    }

    Builder vocabularyKey(Integer vocabularyKey) {
      this.vocabularyKey = vocabularyKey;
      return this;
    }

    Builder parentKey(Integer parentKey) {
      this.parentKey = parentKey;
      return this;
    }

    Builder replacedByKey(Integer replacedByKey) {
      this.replacedByKey = replacedByKey;
      return this;
    }

    Builder page(Pageable page) {
      this.page = page;
      return this;
    }

    ConceptSearchParams build() {
      return new ConceptSearchParams(query, vocabularyKey, parentKey, replacedByKey, name, page);
    }
  }
}
