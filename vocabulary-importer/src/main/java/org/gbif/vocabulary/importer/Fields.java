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
package org.gbif.vocabulary.importer;

import java.util.Arrays;
import java.util.List;

public class Fields {

  static final String CONCEPT = "CONCEPT";
  static final String PARENT = "PARENT";
  static final String LABEL_PREFIX = "LABEL_";
  static final String ALT_LABELS_PREFIX = "ALTERNATIVELABELS_";
  static final String DEFINITION_PREFIX = "DEFINITION_";
  static final String SAME_AS_URIS = "SAMEASURIS";
  static final String EXTERNAL_DEFINITIONS = "EXTERNALDEFINITIONS";
  static final String TAGS = "TAGS";

  static final List<String> CONCEPT_FIELDS =
      Arrays.asList(CONCEPT, PARENT, SAME_AS_URIS, EXTERNAL_DEFINITIONS);
}
