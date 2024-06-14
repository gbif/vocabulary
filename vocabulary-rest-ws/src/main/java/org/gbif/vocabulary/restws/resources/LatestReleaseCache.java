package org.gbif.vocabulary.restws.resources;

import org.cache2k.Cache;

import org.gbif.vocabulary.model.search.SuggestResult;

import java.util.List;
import java.util.Map;

import org.cache2k.Cache2kBuilder;

class LatestReleaseCache {

  /**
   * Cache for the concepts suggestions when querying the latest release versions. It's keyed by
   * vocabulary name and then by the sugestion search parameters.
   */
  static final Cache<String, Map<String, List<SuggestResult>>> conceptSuggestLatestReleaseCache =
      new Cache2kBuilder<String, Map<String, List<SuggestResult>>>() {}.entryCapacity(3000).build();
}
