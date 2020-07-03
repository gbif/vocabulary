package org.gbif.vocabulary.model.export;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** Metadata for an export. */
@Getter
@Setter
public class ExportMetadata implements Serializable {
  private LocalDateTime createdDate;
  private String version;
}
