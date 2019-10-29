package org.gbif.vocabulary.model.export;

import java.time.LocalDateTime;

/** Metadata for an export. */
public class ExportMetadata {

  private LocalDateTime createdDate;

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }
}
