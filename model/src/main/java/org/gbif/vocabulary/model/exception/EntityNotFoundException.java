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
package org.gbif.vocabulary.model.exception;

import lombok.Getter;

/**
 * Exception class to use when an entity is not found in the system.
 */
public class EntityNotFoundException extends RuntimeException {

  @Getter private final EntityType entityType;

  public EntityNotFoundException(EntityType entityType, String message) {
    super(message);
    this.entityType = entityType;
  }

  public enum EntityType {
    VOCABULARY,
    CONCEPT,
    RELEASE;
  }
}
