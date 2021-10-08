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
package org.gbif.vocabulary.model.export;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Getter;

/** Holder for the export parameters that are passed to the service layer. */
@Getter
@Builder
public class ExportParams implements Serializable {

  @NotNull private final String vocabularyName;
  @NotNull private final String version;
  @NotNull private final String user;
  @NotNull private final String comment;
  @NotNull private final String deployRepository;
  @NotNull private final String deployUser;
  @NotNull private final String deployPassword;
}
