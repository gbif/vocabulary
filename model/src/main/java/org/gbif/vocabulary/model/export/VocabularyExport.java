/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
import java.util.List;

import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** Models an export of a vocabulary with all its concepts. */
@Getter
@Setter
public class VocabularyExport implements Serializable {

  public static final String METADATA_PROP = "metadata";
  public static final String VOCABULARY_PROP = "vocabulary";
  public static final String CONCEPTS_PROP = "concepts";

  @JsonProperty(METADATA_PROP)
  private ExportMetadata metadata;

  @JsonProperty(VOCABULARY_PROP)
  private Vocabulary vocabulary;

  @JsonProperty(CONCEPTS_PROP)
  private List<Concept> concepts;
}
