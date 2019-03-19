package org.gbif.vocabulary.restws;

import org.gbif.api.vocabulary.UserRole;

import java.util.Arrays;
import java.util.List;

public enum TestUser {
  USER("user", Arrays.asList(UserRole.USER)),
  ADMIN("admin", Arrays.asList(UserRole.VOCABULARY_ADMIN)),
  EDITOR("editor", Arrays.asList(UserRole.VOCABULARY_EDITOR));

  private String username;
  private String password;
  private List<UserRole> roles;

  TestUser(String username, List<UserRole> roles) {
    this.username = username;
    this.password = username;
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
}
