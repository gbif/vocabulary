package org.gbif.vocabulary.model.normalizers;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Utility class to normalize vocabulary entities. */
public class StringNormalizer {

  private static final String EMPTY = "";
  private static final Pattern NAME_PATTERN = Pattern.compile("[\\-_\\s]");
  private static final Pattern LABEL_PATTERN = Pattern.compile("[\\s]");
  private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^A-Za-z0-9]");

  private StringNormalizer() {}

  /**
   * Normalizes a name of a vocabulary entity.
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

  /**
   * Replaces all non-alphanumeric characters. It doesn't change the case.
   *
   * @param input text to normalize
   * @return text without non-alphanumeric characters
   */
  public static String replaceNonAlphanumericCharacters(String input) {
    return NON_ALPHANUMERIC_PATTERN.matcher(input).replaceAll(EMPTY);
  }
}
