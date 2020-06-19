package org.gbif.vocabulary.persistence.parameters;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Holder for normalized values in a specific json node. This class is used to look for entities
 * with duplicate names or labels.
 */
@Getter
@Setter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NormalizedValuesParam {

  /** This is the node used by the DB to store and retrieve the normalized names. */
  public static final String NAME_NODE = "name";
  /** This is the node used by the DB to store and retrieve the field with all normalized labels. */
  public static final String ALL_NODE = "all";
  /**
   * This is the node used by the DB to store and retrieve the field with the normalized hidden
   * labels.
   */
  public static final String HIDDEN_NODE = "hidden";

  private String node;
  private List<String> values;

  public static NormalizedValuesParam from(String node, List<String> values) {
    return new NormalizedValuesParam(node, values);
  }
}
