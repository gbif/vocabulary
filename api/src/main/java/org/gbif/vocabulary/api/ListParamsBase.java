package org.gbif.vocabulary.api;

import java.util.Objects;
import java.util.StringJoiner;

import org.gbif.api.model.common.paging.Pageable;

import lombok.Builder;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class ListParamsBase implements Pageable {

  @Builder.Default
  protected long offset = 0L;
  @Builder.Default
  protected int limit = 20;

  protected ListParamsBase() {
    this.offset = 0L;
    this.limit = 20;
  }

  protected ListParamsBase(Pageable page) {
    this.setOffset(page.getOffset());
    this.setLimit(page.getLimit());
  }

  protected ListParamsBase(long offset, int limit) {
    this.setOffset(offset);
    this.setLimit(limit);
  }

  public int getLimit() {
    return this.limit;
  }

  public void setLimit(int limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("Limit cannot be negative");
    } else {
      this.limit = limit;
    }
  }

  public long getOffset() {
    return this.offset;
  }

  public void setOffset(long offset) {
    if (offset < 0L) {
      throw new IllegalArgumentException("Offset cannot be negative");
    } else {
      this.offset = offset;
    }
  }

  public void addOffset(long offsetDiff) {
    this.offset += offsetDiff;
    if (this.offset < 0L) {
      this.offset = 0L;
    }
  }

  public void copyPagingValues(Pageable pageable) {
    this.limit = pageable.getLimit();
    this.offset = pageable.getOffset();
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o != null && this.getClass() == o.getClass()) {
      ListParamsBase that = (ListParamsBase) o;
      return this.offset == that.offset && this.limit == that.limit;
    } else {
      return false;
    }
  }

  public int hashCode() {
    return Objects.hash(this.offset, this.limit);
  }

  public String toString() {
    return (new StringJoiner(", ", ListParamsBase.class.getSimpleName() + "[", "]"))
        .add("offset=" + this.offset)
        .add("limit=" + this.limit)
        .toString();
  }
}
