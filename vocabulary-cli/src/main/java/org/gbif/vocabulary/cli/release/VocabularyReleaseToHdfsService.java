package org.gbif.vocabulary.cli.release;

import org.gbif.common.messaging.MessageListener;

import com.google.common.util.concurrent.AbstractIdleService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VocabularyReleaseToHdfsService extends AbstractIdleService {

  private final VocabularyReleaseToHdfsConfiguration config;
  private MessageListener listener;

  public VocabularyReleaseToHdfsService(VocabularyReleaseToHdfsConfiguration config) {
    this.config = config;
  }

  @Override
  protected void startUp() throws Exception {
    log.info("Started vocabulary-release-to-hdfs service with parameters : {}", config);
    listener = new MessageListener(config.messaging.getConnectionParameters(), 1);
    listener.listen(config.queueName, config.poolSize, new VocabularyReleaseToHdfsCallback(config));
  }

  @Override
  protected void shutDown() throws Exception {
    listener.close();
    log.info("Stopping vocabulary-release-to-hdfs service");
  }
}
