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
