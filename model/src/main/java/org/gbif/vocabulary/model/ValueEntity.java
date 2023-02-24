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

import org.gbif.vocabulary.model.utils.PostPersist;
import org.gbif.vocabulary.model.utils.PrePersist;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

/** Model entities whose main purpose is to store a string value. */
public interface ValueEntity extends Serializable {

  @Null(groups = {PrePersist.class})
  @NotNull(groups = {PostPersist.class})
  Long getKey();

  void setKey(Long key);

  @NotBlank
  String getValue();

  void setValue(String value);
}
