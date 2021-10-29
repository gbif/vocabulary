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
