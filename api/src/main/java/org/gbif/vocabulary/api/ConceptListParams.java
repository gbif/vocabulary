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
package org.gbif.vocabulary.api;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConceptListParams extends ListParamsBase implements Serializable {

  private String q;
  private Long parentKey;
  private String parent;
  private Long replacedByKey;
  private String name;
  private Boolean deprecated;
  private Long key;
  private Boolean hasParent;
  private Boolean hasReplacement;
  boolean includeChildrenCount;
  boolean includeChildren;
  boolean includeParents;
  List<String> tags;
  String hiddenLabel;

  public Pageable getPage() {
    return new PagingRequest(this.getOffset(), this.getLimit());
  }
}
