package org.gbif.vocabulary.persistence.mappers;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.vocabulary.Language;
import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.model.search.KeyNameResult;

import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base class to test a {@link BaseMapper} that contains some common tests for base operations of
 * {@link VocabularyEntity}.
 *
 * @param <T> {@link VocabularyEntity} to parameterized the class. Notice that it also has to
 *     implement {@link LenientEquals}.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
@SpringBootTest
abstract class BaseMapperTest<T extends VocabularyEntity & LenientEquals<T>> {

  static final BiFunction<Integer, Long, Pageable> PAGE_FN =
      (limit, offset) ->
          new Pageable() {
            @Override
            public int getLimit() {
              return limit;
            }

            @Override
            public long getOffset() {
              return offset;
            }
          };

  static final Pageable DEFAULT_PAGE = PAGE_FN.apply(10, 0L);

  private final BaseMapper<T> baseMapper;

  BaseMapperTest(BaseMapper<T> baseMapper) {
    this.baseMapper = baseMapper;
  }

  @Test
  public void crudTest() {
    // create
    T entity = createNewEntity("name");
    baseMapper.create(entity);
    assertNotNull(entity.getKey());

    // get
    T entitySaved = baseMapper.get(entity.getKey());
    assertTrue(entity.lenientEquals(entitySaved));

    // update
    entitySaved.getEditorialNotes().add("Note test 2");
    entitySaved.getLabel().put(Language.SPANISH, "Etiqueta");
    baseMapper.update(entitySaved);

    T entityUpdated = baseMapper.get(entitySaved.getKey());
    assertTrue(entitySaved.lenientEquals(entityUpdated));

    entityUpdated.setDefinition(null);
    baseMapper.update(entityUpdated);
    entityUpdated = baseMapper.get(entitySaved.getKey());
    assertTrue(entityUpdated.getDefinition().isEmpty());

    // delete
    assertEquals(0, baseMapper.countDeleted());
    assertEquals(0, baseMapper.deleted(DEFAULT_PAGE).size());

    baseMapper.delete(entityUpdated.getKey());
    assertEquals(1, baseMapper.countDeleted());
    assertEquals(1, baseMapper.deleted(DEFAULT_PAGE).size());

    T entityDeleted = baseMapper.get(entityUpdated.getKey());
    assertNotNull(entityDeleted.getDeleted());

    // restore deleted
    entityDeleted.setDeleted(null);
    baseMapper.update(entityDeleted);
    assertEquals(0, baseMapper.countDeleted());
    assertEquals(0, baseMapper.deleted(DEFAULT_PAGE).size());
  }

  @Test
  public void suggestTest() {
    // create entities for the test
    T entity1 = createNewEntity("suggest111");
    baseMapper.create(entity1);
    assertNotNull(entity1.getKey());

    T entity2 = createNewEntity("suggest222");
    baseMapper.create(entity2);
    assertNotNull(entity2.getKey());

    // check result values
    List<KeyNameResult> result = baseMapper.suggest("suggest1");
    assertEquals("suggest111", result.get(0).getName());
    assertEquals(entity1.getKey().intValue(), result.get(0).getKey());

    // assert expected number of results
    assertEquals(2, baseMapper.suggest("su").size());
    assertEquals(2, baseMapper.suggest("gge").size());
    assertEquals(1, baseMapper.suggest("22").size());
    assertEquals(0, baseMapper.suggest("zz").size());
    assertEquals(0, baseMapper.suggest(null).size());
  }

  @Test
  public void deleteNonExixstingEntityTest() {
    assertDoesNotThrow(() -> baseMapper.delete(Integer.MAX_VALUE));
  }

  @Test
  public void getNonExistingEntityTest() {
    assertNull(baseMapper.get(Integer.MAX_VALUE));
  }

  abstract T createNewEntity(String name);
}
