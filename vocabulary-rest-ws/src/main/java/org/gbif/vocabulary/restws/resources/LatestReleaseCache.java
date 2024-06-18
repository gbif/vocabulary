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
package org.gbif.vocabulary.restws.resources;

import org.gbif.vocabulary.model.search.SuggestResult;

import java.util.List;
import java.util.Map;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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
