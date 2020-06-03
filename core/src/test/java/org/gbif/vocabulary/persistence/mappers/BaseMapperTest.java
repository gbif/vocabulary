package org.gbif.vocabulary.persistence.mappers;

import org.gbif.api.model.registry.LenientEquals;
import org.gbif.vocabulary.PostgresDBExtension;
import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.model.enums.LanguageRegion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.gbif.vocabulary.TestUtils.DEPRECATED_BY;
import static org.gbif.vocabulary.TestUtils.assertDeprecated;
import static org.gbif.vocabulary.TestUtils.assertDeprecatedWithReplacement;
import static org.gbif.vocabulary.TestUtils.assertNotDeprecated;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base class to test a {@link BaseMapper} that contains some common tests for base operations of
 * {@link VocabularyEntity}.
 *
 * <p>It rolls back all the transactions, so there is no need to clean the DB before/after each
 * test.
 *
 * @param <T> {@link VocabularyEntity} to parameterized the class. Notice that it also has to
 *     implement {@link LenientEquals}.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
abstract class BaseMapperTest<T extends VocabularyEntity & LenientEquals<T>> {

  @RegisterExtension static PostgresDBExtension database = new PostgresDBExtension();

  private final BaseMapper<T> baseMapper;

  BaseMapperTest(BaseMapper<T> baseMapper) {
    this.baseMapper = baseMapper;
  }

  @Test
  public void crudTest() {
    // create
    T entity = createNewEntity();
    baseMapper.create(entity);
    assertNotNull(entity.getKey());

    // get
    T entitySaved = baseMapper.get(entity.getKey());
    assertTrue(entity.lenientEquals(entitySaved));

    // update
    entitySaved.getEditorialNotes().add("Note test 2");
    entitySaved.getLabel().put(LanguageRegion.SPANISH, "Etiqueta");
    baseMapper.update(entitySaved);

    T entityUpdated = baseMapper.get(entitySaved.getKey());
    assertTrue(entitySaved.lenientEquals(entityUpdated));

    entityUpdated.setDefinition(null);
    baseMapper.update(entityUpdated);
    entityUpdated = baseMapper.get(entitySaved.getKey());
    assertTrue(entityUpdated.getDefinition().isEmpty());
  }

  @Test
  public void getNonExistingEntityTest() {
    assertNull(baseMapper.get(Integer.MAX_VALUE));
  }

  @Test
  public void deprecationTest() {
    T entity1 = createNewEntity();
    baseMapper.create(entity1);
    assertNull(entity1.getDeprecated());

    T entity2 = createNewEntity();
    baseMapper.create(entity2);
    assertNull(entity2.getDeprecated());

    // deprecate
    baseMapper.deprecate(entity1.getKey(), DEPRECATED_BY, null);
    assertDeprecated(baseMapper.get(entity1.getKey()), DEPRECATED_BY);

    // undeprecate
    baseMapper.restoreDeprecated(entity1.getKey());
    assertNotDeprecated(baseMapper.get(entity1.getKey()));

    // deprecate with replacement
    baseMapper.deprecate(entity1.getKey(), DEPRECATED_BY, entity2.getKey());
    assertDeprecatedWithReplacement(
        baseMapper.get(entity1.getKey()), DEPRECATED_BY, entity2.getKey());

    // undeprecate with replacement
    baseMapper.restoreDeprecated(entity1.getKey());
    assertNotDeprecated(baseMapper.get(entity1.getKey()));
  }

  @Test
  public void isDeprecatedTest() {
    T entity = createNewEntity();
    baseMapper.create(entity);
    assertFalse(baseMapper.isDeprecated(entity.getKey()));

    baseMapper.deprecate(entity.getKey(), DEPRECATED_BY, null);
    assertTrue(baseMapper.isDeprecated(entity.getKey()));
  }

  abstract T createNewEntity();
}
