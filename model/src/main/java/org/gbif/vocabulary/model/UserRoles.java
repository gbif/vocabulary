package org.gbif.vocabulary.model;

/**
 * Roles to use in the method security.
 *
 * <p>Needed since we can't use enums in the security annotations.
 */
public final class UserRoles {

  private UserRoles() {}

  public static final String VOCABULARY_ADMIN = "VOCABULARY_ADMIN";
  public static final String VOCABULARY_EDITOR = "VOCABULARY_EDITOR";
}
