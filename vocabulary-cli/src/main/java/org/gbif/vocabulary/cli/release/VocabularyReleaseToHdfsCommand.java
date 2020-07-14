package org.gbif.vocabulary.cli.release;

import org.gbif.cli.Command;
import org.gbif.cli.service.ServiceCommand;

import org.kohsuke.MetaInfServices;

import com.google.common.util.concurrent.Service;

@MetaInfServices(Command.class)
public class VocabularyReleaseToHdfsCommand extends ServiceCommand {

  private final VocabularyReleaseToHdfsConfiguration config =
      new VocabularyReleaseToHdfsConfiguration();

  public VocabularyReleaseToHdfsCommand() {
    super("vocabulary-release-to-hdfs");
  }

  @Override
  protected Service getService() {
    return new VocabularyReleaseToHdfsService(config);
  }

  @Override
  protected Object getConfigurationObject() {
    return config;
  }
}
