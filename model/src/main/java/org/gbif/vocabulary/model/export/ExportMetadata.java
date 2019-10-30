package org.gbif.vocabulary.model.export;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Metadata for an export. */
public class ExportMetadata implements Serializable {

  private LocalDateTime createdDate;

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }
}
