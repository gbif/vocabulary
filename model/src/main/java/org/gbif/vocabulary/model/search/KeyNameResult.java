package org.gbif.vocabulary.model.search;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Utility container to hold the key and name of a {@link
 * org.gbif.vocabulary.model.VocabularyEntity}.
 */
public class KeyNameResult {

  private long key;
  private String name;

  public long getKey() {
    return key;
  }

  public void setKey(long key) {
    this.key = key;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    KeyNameResult that = (KeyNameResult) o;
    return key == that.key && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, name);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", KeyNameResult.class.getSimpleName() + "[", "]")
        .add("key=" + key)
        .add("name='" + name + "'")
        .toString();
  }
}
