package org.gbif.vocabulary.service;

import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.persistence.mappers.BaseMapper;

import java.time.LocalDateTime;
import javax.sql.DataSource;
import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Base class to test a {@link BaseService} that can be reused for all the implemenations.
 *
 * @param <T>
 */
@TestPropertySource(properties = "spring.liquibase.enabled=false")
@ExtendWith(SpringExtension.class)
@SpringBootTest
abstract class BaseServiceTest<T extends VocabularyEntity> {

  protected static final int TEST_KEY = 1;

  @MockBean private DataSource dataSource;
  @MockBean private PlatformTransactionManager platformTransactionManager;

  @Test
  public void createNullEntityTest() {
    assertThrows(ConstraintViolationException.class, () -> getService().create(null));
  }

  @Test
  public void createEntityWithKeyTest() {
    T entity = createNewEntity("name");
    entity.setKey(TEST_KEY);
    assertThrows(IllegalArgumentException.class, () -> getService().create(entity));
  }

  @Test
  public void getTest() {
    // mock
    when(getMapper().get(TEST_KEY)).thenReturn(createNewEntity("name"));

    T entity = getService().get(TEST_KEY);
    assertEquals("name", entity.getName());
  }

  @Test
  public void updateNullEntityTest() {
    assertThrows(ConstraintViolationException.class, () -> getService().update(null));
    // null key
    assertThrows(NullPointerException.class, () -> getService().update(createNewEntity("name")));
  }

  @Test
  public void updateDeletedEntityTest() {
    T deletedEntity = createNewEntity("e1");
    deletedEntity.setKey(TEST_KEY);
    deletedEntity.setKey(TEST_KEY);
    deletedEntity.setDeleted(LocalDateTime.now());

    // mock
    when(getMapper().get(TEST_KEY)).thenReturn(deletedEntity);

    T newEntity = createNewEntity("name");
    BeanUtils.copyProperties(deletedEntity, newEntity);
    assertThrows(IllegalArgumentException.class, () -> getService().update(newEntity));
  }

  @Test
  public void deletingWhenUpdatingTest() {
    T entityDB = createNewEntity("e1");
    entityDB.setKey(TEST_KEY);
    T updatedEntity = createNewEntity("e1");
    BeanUtils.copyProperties(entityDB, updatedEntity);
    updatedEntity.setDeleted(LocalDateTime.now());

    // mock
    when(getMapper().get(TEST_KEY)).thenReturn(entityDB);

    assertThrows(IllegalArgumentException.class, () -> getService().update(updatedEntity));
  }

  @Test
  public void deprecatingWhenUpdatingTest() {
    T entityDB = createNewEntity("e1");
    entityDB.setKey(TEST_KEY);
    T updatedEntity = createNewEntity("e1");
    BeanUtils.copyProperties(entityDB, updatedEntity);
    updatedEntity.setReplacedByKey(2);

    // mock
    when(getMapper().get(TEST_KEY)).thenReturn(entityDB);

    assertThrows(IllegalArgumentException.class, () -> getService().update(updatedEntity));
  }

  @Test
  public void restoringDeprecatedWhenUpdatingTest() {
    T entityDB = createNewEntity("e1");
    entityDB.setKey(TEST_KEY);
    entityDB.setDeprecated(LocalDateTime.now());
    entityDB.setDeprecatedBy("test");
    T updatedEntity = createNewEntity("e1");
    BeanUtils.copyProperties(entityDB, updatedEntity);
    updatedEntity.setDeprecated(null);

    // mock
    when(getMapper().get(TEST_KEY)).thenReturn(entityDB);

    assertThrows(IllegalArgumentException.class, () -> getService().update(updatedEntity));
  }

  abstract T createNewEntity(String name);

  protected void mockCreateEntity(T entity) {
    doAnswer(
            invocation -> {
              T createdEntity = invocation.getArgument(0);
              createdEntity.setKey(TEST_KEY);
              return createdEntity;
            })
        .when(getMapper())
        .create(entity);
  }

  abstract BaseMapper<T> getMapper();

  abstract BaseService<T> getService();
}
