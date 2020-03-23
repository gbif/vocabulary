package org.gbif.vocabulary.service.validator;

import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.model.search.KeyNameResult;

import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** Contians validity checks for entities. */
public final class EntityValidator {

  private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9-_]*");

  private EntityValidator() {}

  /**
   * Checks if an entity has similarities and if its name follow the establisehd conventions.
   *
   * @param entity {@link VocabularyEntity} to validate
   * @param similaritiesExtractor provides the similarities for the entity that is being validated
   * @param <T> {@link org.gbif.vocabulary.model.VocabularyEntity}
   */
  public static <T extends VocabularyEntity> void validateEntity(
      T entity, Supplier<List<KeyNameResult>> similaritiesExtractor) {

    if (!NAME_PATTERN.matcher(entity.getName()).matches()) {
      throw new IllegalArgumentException(
          "Entity name has to match the regex " + NAME_PATTERN.pattern());
    }

    List<KeyNameResult> similarities = similaritiesExtractor.get();
    if (!similarities.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot create entity because it conflicts with other entities, e.g.: "
              + similarities.toString());
    }
  }
}
