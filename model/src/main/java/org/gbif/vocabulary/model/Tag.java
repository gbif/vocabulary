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
package org.gbif.vocabulary.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Data;
import org.gbif.vocabulary.model.utils.LenientEquals;

@Data
public class Tag implements Serializable, LenientEquals<Tag> {

  private Integer key;
  @NotBlank private String name;
  private String description;

  @Pattern(regexp = "^#[A-Z0-9]{6}$")
  private String color;

  private ZonedDateTime created;
  private String createdBy;
  private ZonedDateTime modified;
  private String modifiedBy;

  public static Tag of(String name) {
    Tag tag = new Tag();
    tag.setName(name);
    return tag;
  }

  @Override
  public boolean lenientEquals(Tag other) {
    if (this == other) {
      return true;
    } else {
      return Objects.equals(this.name, other.name)
          && Objects.equals(this.color, other.color)
          && Objects.equals(this.description, other.description);
    }
  }
}
