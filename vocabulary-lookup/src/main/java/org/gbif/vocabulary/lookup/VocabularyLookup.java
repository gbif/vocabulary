package org.gbif.vocabulary.lookup;

import java.util.Optional;

import org.gbif.vocabulary.model.LanguageRegion;

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
