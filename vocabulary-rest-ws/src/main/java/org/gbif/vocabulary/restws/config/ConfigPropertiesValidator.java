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
package org.gbif.vocabulary.restws.config;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Custom {@link Validator} for the configuration properties. Needed to validate properties than
 * depend on another property.
 */
@Component
public class ConfigPropertiesValidator implements Validator {

  @Override
  public boolean supports(Class<?> clazz) {
    return ExportConfig.class.isAssignableFrom(clazz)
        || MessagingConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    if (target instanceof ExportConfig) {
      ExportConfig.validate((ExportConfig) target, errors);
    } else if (target instanceof MessagingConfig) {
      MessagingConfig.validate((MessagingConfig) target, errors);
      if (true)
        System.out.println("TEST2");
    }
  }
}
