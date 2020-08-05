/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
