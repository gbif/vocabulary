package org.gbif.vocabulary.api;

import java.io.Serializable;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PageableBase;
import org.gbif.api.model.common.paging.PagingRequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConceptListParams extends PageableBase implements Serializable {

  private String query;
  private Long vocabularyKey;
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

  public Pageable getPage() {
    return new PagingRequest(this.getOffset(), this.getLimit());
  }
}
