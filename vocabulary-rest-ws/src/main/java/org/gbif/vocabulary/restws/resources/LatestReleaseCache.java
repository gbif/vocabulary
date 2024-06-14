package org.gbif.vocabulary.restws.resources;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.gbif.vocabulary.model.search.SuggestResult;

import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class LatestReleaseCache {

  /**
   * Cache for the concepts suggestions when querying the latest release versions. It's keyed by
   * vocabulary name and then by the sugestion search parameters.
   */
  static final Cache<String, Map<String, List<SuggestResult>>> conceptSuggestLatestReleaseCache =
      new Cache2kBuilder<String, Map<String, List<SuggestResult>>>() {}.weigher(
              (k, v) -> v.keySet().size())
          .maximumWeight(20_000)
          .eternal(true)
          .build();
}
