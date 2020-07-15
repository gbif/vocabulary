package org.gbif.vocabulary.lookup;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Predefined prefilters that can be reused in {@link VocabularyLookup}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PreFilters {

  private static final String EMPTY = "";
  private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^A-Za-z0-9]");
  private static final Pattern NON_LETTERS_PATTERN = Pattern.compile("[^A-Za-z]");
  private static final Pattern NUMBERS_PATTERN = Pattern.compile("[0-9]");
  private static final Pattern NUMERIC_PREFIX_PATTERN = Pattern.compile("^[0-9]+");
  private static final Pattern SIGNED_DECIMAL_NUMBERS_PREFIX_PATTERN =
      Pattern.compile("^[+-]?[0-9]+([.,][0-9]+)*|^[+-]?[0-9]*[.,][0-9]+");
  private static final Pattern PARENTHESIS_CONTENT_SUFFIX_PATTERN = Pattern.compile("\\(.*\\)$");

  /** Removes all non alpha numeric characters. */
  public static final UnaryOperator<String> REMOVE_NON_ALPHANUMERIC =
      s -> NON_ALPHANUMERIC_PATTERN.matcher(s).replaceAll(EMPTY);

  /** Removes all numeric chars. */
  public static final UnaryOperator<String> REMOVE_NUMERIC =
      s -> NUMBERS_PATTERN.matcher(s).replaceAll(EMPTY);

  /** Removes all the characters that are not letters. */
  public static final UnaryOperator<String> REMOVE_NON_LETTER =
      s -> NON_LETTERS_PATTERN.matcher(s).replaceAll(EMPTY);

  /**
   * Removes all the numbers at the beginning of a string.
   *
   * <p>For example, for "1test" it will return "test".
   */
  public static final UnaryOperator<String> REMOVE_NUMERIC_PREFIX =
      s -> NUMERIC_PREFIX_PATTERN.matcher(s).replaceAll(EMPTY);

  /**
   * Removes all the numbers at the beginning of a string. These numbers can signed and contain
   * decimals, either separated by '.' or ','.
   *
   * <p>For example, for "1.2test" it will return "test".
   */
  public static final UnaryOperator<String> REMOVE_SIGNED_DECIMAL_NUMBERS_PREFIX =
      s -> SIGNED_DECIMAL_NUMBERS_PREFIX_PATTERN.matcher(s).replaceAll(EMPTY);

  /**
   * Removes all the characters contained inside a parenthesis at the end of a string.
   *
   * <p>For example, for "test(other info)" it will return "test".
   */
  public static final UnaryOperator<String> REMOVE_PARENTHESIS_CONTENT_SUFFIX =
      s -> PARENTHESIS_CONTENT_SUFFIX_PATTERN.matcher(s).replaceAll(EMPTY);
}
