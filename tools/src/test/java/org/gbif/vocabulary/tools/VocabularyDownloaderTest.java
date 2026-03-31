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
package org.gbif.vocabulary.tools;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.gbif.vocabulary.model.export.Export;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class VocabularyDownloaderTest {

  @Disabled("manual test")
  @Test
  public void loadVocabularyFromApiUrl() throws IOException {
    InputStream in =
        VocabularyDownloader.downloadLatestVocabularyVersion(
            "http://api.gbif-dev.org/v1/", "LifeStage");

    Export export =
        new ObjectMapper()
            .registerModule(
                new JavaTimeModule()
                    .addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer()))
            .readValue(in, Export.class);
    assertNotNull(export);
  }

  private static class ZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {

    private final LocalDateTimeDeserializer localDateTimeDeserializer =
        new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    @Override
    public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

      // try first with timezone, otherwise without it
      try {
        return ZonedDateTime.parse(p.getText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
      } catch (Exception ex) {
        return localDateTimeDeserializer.deserialize(p, ctxt).atZone(ZoneId.systemDefault());
      }
    }
  }
}
