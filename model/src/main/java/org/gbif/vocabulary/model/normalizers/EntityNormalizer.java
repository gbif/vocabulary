package org.gbif.vocabulary.model.normalizers;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Utility class to normalize vocabulary entities. */
public class EntityNormalizer {

  private static final Pattern NAME_PATTERN = Pattern.compile("[\\-_\\s]");
  private static final Pattern LABEL_PATTERN = Pattern.compile("[\\s]");

  private EntityNormalizer() {}

  /**
   * Normalizes a name of a vocabulary entity.
   *
   * @param name name to normalize
   * @return the name normalized
   */
  public static String normalizeName(String name) {
    return NAME_PATTERN.matcher(name).replaceAll("").toLowerCase();
  }

  /**
   * Normalizes a label of a vocabulary entity.
   *
   * @param label label to normalize
   * @return the label normalized
   */
  public static String normalizeLabel(String label) {
    return LABEL_PATTERN.matcher(label).replaceAll("").toLowerCase();
  }

  /**
   * Normalizes a list of labels of a vocabulary entity.
   *
   * @param labels labels to normalize
   * @return the labels normalized
   */
  public static List<String> normalizeLabels(Collection<String> labels) {
    return labels.stream().map(EntityNormalizer::normalizeLabel).collect(Collectors.toList());
  }
}
