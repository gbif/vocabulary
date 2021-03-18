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
package org.gbif.vocabulary.lookup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests the {@link PreFilters}. */
public class PreFiltersTest {

  @Test
  public void removeNonAlphanumericCharactersTest() {
    assertEquals("abc123", PreFilters.REMOVE_NON_ALPHANUMERIC.apply("!?|%#&*<>ab-c123{-)];' "));
  }

  @Test
  public void removeNumericTest() {
    assertEquals("abc", PreFilters.REMOVE_NUMERIC.apply("123ab35c234"));
    assertEquals("abc", PreFilters.REMOVE_NUMERIC.apply("123abc"));
    assertEquals("abc", PreFilters.REMOVE_NUMERIC.apply("abc234"));
  }

  @Test
  public void removeNonLettersTest() {
    assertEquals("abc", PreFilters.REMOVE_NON_LETTER.apply("123ab35c234"));
    assertEquals("abc", PreFilters.REMOVE_NON_LETTER.apply("(214-325)abc"));
    assertEquals("abc", PreFilters.REMOVE_NON_LETTER.apply("abc (124-124%&)"));
  }

  @Test
  public void removedNumericPrefixTest() {
    assertEquals("abc", PreFilters.REMOVE_NUMERIC_PREFIX.apply("234abc"));
    assertEquals("abc123", PreFilters.REMOVE_NUMERIC_PREFIX.apply("234abc123"));
  }

  @Test
  public void removeSignedDecimalNumbersPrefixTest() {
    assertEquals(
        ".abc123", PreFilters.REMOVE_SIGNED_DECIMAL_NUMBERS_PREFIX.apply("234.123.abc123"));
    assertEquals(" abc 123", PreFilters.REMOVE_SIGNED_DECIMAL_NUMBERS_PREFIX.apply("234 abc 123"));
    assertEquals(" abc 123", PreFilters.REMOVE_SIGNED_DECIMAL_NUMBERS_PREFIX.apply("23.4 abc 123"));
    assertEquals(" abc 123", PreFilters.REMOVE_SIGNED_DECIMAL_NUMBERS_PREFIX.apply("23,4 abc 123"));
    assertEquals(" abc 123", PreFilters.REMOVE_SIGNED_DECIMAL_NUMBERS_PREFIX.apply(",44 abc 123"));
    assertEquals(" abc 123", PreFilters.REMOVE_SIGNED_DECIMAL_NUMBERS_PREFIX.apply(".45 abc 123"));
    assertEquals("abc", PreFilters.REMOVE_SIGNED_DECIMAL_NUMBERS_PREFIX.apply("-23abc"));
    assertEquals("+-23abc", PreFilters.REMOVE_SIGNED_DECIMAL_NUMBERS_PREFIX.apply("+-23abc"));
    assertEquals("--23abc", PreFilters.REMOVE_SIGNED_DECIMAL_NUMBERS_PREFIX.apply("--23abc"));
    assertEquals("abc", PreFilters.REMOVE_SIGNED_DECIMAL_NUMBERS_PREFIX.apply("+23.4abc"));
  }

  @Test
  public void removeParenthesisContentSuffixTest() {
    assertEquals("a12b", PreFilters.REMOVE_PARENTHESIS_CONTENT_SUFFIX.apply("a12b(sf 123 =_)"));
    assertEquals("a12b ", PreFilters.REMOVE_PARENTHESIS_CONTENT_SUFFIX.apply("a12b (sf 123 =_)"));
    assertEquals("a12b ))", PreFilters.REMOVE_PARENTHESIS_CONTENT_SUFFIX.apply("a12b ))"));
    assertEquals("(af)a(a) s", PreFilters.REMOVE_PARENTHESIS_CONTENT_SUFFIX.apply("(af)a(a) s"));
  }
}
