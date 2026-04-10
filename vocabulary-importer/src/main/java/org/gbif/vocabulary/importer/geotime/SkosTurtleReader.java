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
package org.gbif.vocabulary.importer.geotime;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

public class SkosTurtleReader {

  private static final int CONNECTION_TIMEOUT_MS = 30_000;
  private static final int READ_TIMEOUT_MS = 60_000;

  public Model read(String source) {
    String normalizedSource = normalizeSource(source);
    OpenedSource openedSource = openSource(normalizedSource);

    Model model = ModelFactory.createDefaultModel();
    try (InputStream inputStream = openedSource.inputStream()) {
      RDFDataMgr.read(model, inputStream, openedSource.baseUri(), Lang.TTL);
      return model;
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to read RDF turtle source " + normalizedSource, e);
    }
  }

  static String normalizeSource(String source) {
    String trimmed = Objects.requireNonNull(source, "source can't be null").trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("source can't be blank");
    }
    // Convert GitHub blob URLs to raw URLs to get the actual file content instead of HTML
    if (trimmed.contains("github.com") && trimmed.contains("/blob/")) {
      return trimmed.replace("github.com", "raw.githubusercontent.com").replace("/blob/", "/");
    }
    return trimmed;
  }

  private OpenedSource openSource(String source) {
    try {
      if (source.startsWith("https://") || source.startsWith("http://")) {
        URLConnection conn = URI.create(source).toURL().openConnection();
        conn.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        return new OpenedSource(conn.getInputStream(), source);
      }

      Path path = source.startsWith("file:") ? Path.of(URI.create(source)) : Path.of(source);
      return new OpenedSource(Files.newInputStream(path), path.toUri().toString());
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to open turtle source " + source, e);
    }
  }

  private record OpenedSource(InputStream inputStream, String baseUri) {}
}
