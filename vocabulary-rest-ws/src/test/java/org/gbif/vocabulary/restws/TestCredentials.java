/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.vocabulary.restws;

import org.gbif.api.vocabulary.UserRole;

import java.util.Collections;
import java.util.List;

public enum TestCredentials {
  USER("user", Collections.singletonList(UserRole.USER)),
  ADMIN("admin", Collections.singletonList(UserRole.VOCABULARY_ADMIN)),
  EDITOR("editor", Collections.singletonList(UserRole.VOCABULARY_EDITOR)),
  INVALID_USER("fake", Collections.emptyList()),
  JWT_USER("jwtUser", "test_token_user", Collections.singletonList(UserRole.USER)),
  JWT_ADMIN("jwtAdmin", "test_token_admin", Collections.singletonList(UserRole.VOCABULARY_ADMIN)),
  JWT_EDITOR(
      "jwtEditor", "test_token_editor", Collections.singletonList(UserRole.VOCABULARY_EDITOR)),
  INVALID_JWT_USER("jwtFake", "fake token", Collections.emptyList());

  private String username;
  private String password;
  private List<UserRole> roles;
  private String token;

  TestCredentials(String username, List<UserRole> roles) {
    this.username = username;
    this.password = username;
    this.roles = roles;
  }

  TestCredentials(String username, String token, List<UserRole> roles) {
    this.username = username;
    this.password = username;
    this.token = token;
    this.roles = roles;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public List<UserRole> getRoles() {
    return roles;
  }

  public String getToken() {
    return token;
  }
}
