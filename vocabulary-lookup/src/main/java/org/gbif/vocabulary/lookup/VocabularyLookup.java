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
package org.gbif.vocabulary.lookup;

import org.gbif.vocabulary.model.LanguageRegion;

import java.util.Optional;

/** Service to look up concepts within a vocabulary. */
public interface VocabularyLookup {

  /**
   * Looks up for a value in the vocabulary.
   *
   * @param value the value whose concept we are looking for
   * @return the {@link LookupConcept} found. Empty {@link Optional} if there was no match.
   */
  Optional<LookupConcept> lookup(String value);

  /**
   * Looks up for a value in the vocabulary but with a language context.
   *
   * <p>The contextLang can be used to influence the lookup or as a discriminator in case there are
   * multiple matches.
   *
   * @param value the value whose concept we are looking for
   * @param contextLang {@link LanguageRegion} to use in the lookup
   * @return the {@link LookupConcept} found. Empty {@link Optional} if there was no match.
   */
  Optional<LookupConcept> lookup(String value, LanguageRegion contextLang);
}
