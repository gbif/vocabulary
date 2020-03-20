package org.gbif.vocabulary.persistence.parameters;

import java.util.List;

/**
 * Holder for normalized values in a specific json node. This class is used to look for entities
 * with duplicate names or labels.
 */
public class NormalizedValuesParam {

  /** This is the node used by the DB to store and retrieve the normalized names. */
  public static final String NAME_NODE = "name";
  /** This is the node used by the DB to store and retrieve the field with all normalized labels. */
  public static final String ALL_NODE = "all";

  private NormalizedValuesParam(String node, List<String> values) {
    this.node = node;
    this.values = values;
  }

  public static NormalizedValuesParam from(String node, List<String> values) {
    return new NormalizedValuesParam(node, values);
  }

  private String node;
  private List<String> values;

  public String getNode() {
    return node;
  }

  public void setNode(String node) {
    this.node = node;
  }

  public List<String> getValues() {
    return values;
  }

  public void setValues(List<String> values) {
    this.values = values;
  }
}
