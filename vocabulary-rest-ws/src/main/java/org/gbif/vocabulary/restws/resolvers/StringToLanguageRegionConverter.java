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
package org.gbif.vocabulary.restws.resolvers;

import org.gbif.vocabulary.model.LanguageRegion;
import org.springframework.core.convert.converter.Converter;

/**
 * Converts a request query parameter string value to a {@link LanguageRegion}.
 * Tries to match by locale first (e.g. "es-ES"), then by enum name as fallback.
 */
public class StringToLanguageRegionConverter implements Converter<String, LanguageRegion> {

  @Override
  public LanguageRegion convert(String source) {
    if (source == null || source.trim().isEmpty()) {
      return null;
    }

    String value = source.trim();

    // Try matching by locale (e.g. "es-ES", "en", "zh-CN")
    LanguageRegion byLocale = LanguageRegion.fromLocale(value);
    if (byLocale != LanguageRegion.UNKNOWN) {
      return byLocale;
    }

    // Fallback: try matching by enum constant name (e.g. "SPANISH")
    try {
      return LanguageRegion.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown language region: " + value, e);
    }
  }
}

