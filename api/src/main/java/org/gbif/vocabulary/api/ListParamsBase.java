/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.vocabulary.api;

import org.gbif.api.model.common.paging.Pageable;

import java.util.Objects;
import java.util.StringJoiner;

import lombok.Builder;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class ListParamsBase implements Pageable {

  @Builder.Default protected long offset = 0L;
  @Builder.Default protected int limit = 20;

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
