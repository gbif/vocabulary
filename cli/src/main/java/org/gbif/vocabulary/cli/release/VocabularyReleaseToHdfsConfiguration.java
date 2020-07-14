package org.gbif.vocabulary.cli.release;

import org.gbif.common.messaging.config.MessagingConfiguration;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.ToString;

@ToString
public class VocabularyReleaseToHdfsConfiguration {

  @ParametersDelegate @Valid @NotNull
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @Parameter(names = "--queue-name")
  @NotNull
  public String queueName;

  @Parameter(names = "--pool-size")
  @NotNull
  @Min(1)
  public int poolSize = 1;

  @Parameter(names = "--target-directory")
  @NotNull
  public String targetDirectory;

  @Parameter(names = "--hdfs-site-config")
  @NotNull
  public String hdfsSiteConfig;

  @Parameter(names = "--hdfs-prefix")
  @NotNull
  public String hdfsPrefix = "hdfs://ha-nn";
}
