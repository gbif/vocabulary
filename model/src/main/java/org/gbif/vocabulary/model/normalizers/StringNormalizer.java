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
package org.gbif.vocabulary.model.normalizers;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class to normalize vocabulary entities. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringNormalizer {

  private static final String EMPTY = "";
  private static final Pattern NAME_PATTERN = Pattern.compile("[\\-_\\s]");
  private static final Pattern LABEL_PATTERN = Pattern.compile("[\\s]");

  /**
   * Normalizes a name of a vocabulary entity.
   *
   * <p><b>This normalization is the same that the DB does</b>
   *
   * @param name name to normalize
   * @return the name normalized
   */
  public static String normalizeName(String name) {
    return NAME_PATTERN.matcher(name).replaceAll(EMPTY).toLowerCase();
  }

  /**
   * Normalizes a label of a vocabulary entity.
   *
   * <p><b>This normalization is the same that the DB does</b>
   *
   * @param label label to normalize
   * @return the label normalized
   */
  public static String normalizeLabel(String label) {
    return LABEL_PATTERN.matcher(label).replaceAll(EMPTY).toLowerCase();
  }

  /**
   * Normalizes a list of labels of a vocabulary entity.
   *
   * @param labels labels to normalize
   * @return the labels normalized
   */
  public static List<String> normalizeLabels(Collection<String> labels) {
    return labels.stream().map(StringNormalizer::normalizeLabel).collect(Collectors.toList());
  }

  /**
   * Replaces non-ASCII characters with their equivalents.
   *
   * @param input text to normalize
   * @return string without non-ASCII characters
   */
  public static String replaceNonAsciiCharactersWithEquivalents(String input) {
    return AsciiParser.parse(input);
  }
}
