package org.gbif.vocabulary.restws;

import org.gbif.api.vocabulary.UserRole;

import java.util.Arrays;
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
