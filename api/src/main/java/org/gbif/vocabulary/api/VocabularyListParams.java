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
public class VocabularyListParams extends PageableBase implements Serializable {

  private String query;
  private String name;
  private String namespace;
  private Boolean deprecated;
  private Long key;

  public Pageable getPage() {
    return new PagingRequest(this.getOffset(), this.getLimit());
  }
}
