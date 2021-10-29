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
package org.gbif.vocabulary.model;

import java.time.LocalDateTime;

/** Defines the minimum fields an entity must have to be deprecable. */
public interface Deprecable {

  /** Key of the entity that replaces a deprecated entity. */
  Long getReplacedByKey();

  void setReplacedByKey(Long replacedByKey);

  LocalDateTime getDeprecated();

  void setDeprecated(LocalDateTime deprecated);

  String getDeprecatedBy();

  void setDeprecatedBy(String deprecatedBy);
}
