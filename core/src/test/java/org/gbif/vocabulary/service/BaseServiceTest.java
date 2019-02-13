package org.gbif.vocabulary.service;

import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.persistence.mapper.BaseMapper;

import java.time.LocalDateTime;
import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
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

  private final BaseService<T> baseService;
  private final BaseMapper<T> baseMapper;

  BaseServiceTest(BaseService<T> baseService, BaseMapper<T> baseMapper) {
    this.baseService = baseService;
    this.baseMapper = baseMapper;
  }

  @Test
  public void createTest() {
    T entity = createNewEntity("name");

    // mock
    mockCreateEntity(entity);

    baseService.create(entity);

    Assertions.assertEquals(TEST_KEY, entity.getKey().intValue());
  }

  @Test
  public void createNullEntityTest() {
    assertThrows(ConstraintViolationException.class, () -> baseService.create(null));
  }

  @Test
  public void createEntityWithKeyTest() {
    T entity = createNewEntity("name");
    entity.setKey(TEST_KEY);
    assertThrows(IllegalArgumentException.class, () -> baseService.create(entity));
  }

  @Test
  public void getTest() {
    // mock
    when(baseMapper.get(TEST_KEY)).thenReturn(createNewEntity("name"));

    T entity = baseService.get(TEST_KEY);
    assertEquals("name", entity.getName());
  }

  @Test
  public void updateTest() {
    T entity = createNewEntity("name");
    entity.setKey(TEST_KEY);

    // mock
    when(baseMapper.get(TEST_KEY)).thenReturn(entity).thenReturn(entity);
    doNothing().when(baseMapper).update(entity);

    assertEquals(entity, baseService.update(entity));
  }

  @Test
  public void updateNullEntityTest() {
    assertThrows(ConstraintViolationException.class, () -> baseService.update(null));
    // null key
    assertThrows(NullPointerException.class, () -> baseService.update(createNewEntity("name")));
  }

  @Test
  public void updateDeletedEntityTest() {
    T deletedEntity = createNewEntity("name");
    deletedEntity.setKey(TEST_KEY);
    deletedEntity.setDeleted(LocalDateTime.now());

    // mock
    when(baseMapper.get(TEST_KEY)).thenReturn(deletedEntity);

    T newEntity = createNewEntity("name");
    BeanUtils.copyProperties(deletedEntity, newEntity);
    assertThrows(IllegalArgumentException.class, () -> baseService.update(newEntity));
  }

  @Test
  public void deleteWhenUpdatingTest() {
    T entity = createNewEntity("name");
    entity.setKey(TEST_KEY);

    // mock
    when(baseMapper.get(TEST_KEY)).thenReturn(entity);
    doNothing().when(baseMapper).update(entity);

    entity.setDeleted(LocalDateTime.now());
    assertThrows(IllegalArgumentException.class, () -> baseService.update(entity));
  }

  @Test
  public void restoreDeletedEntityTest() {
    T deletedEntity = createNewEntity("name");
    deletedEntity.setKey(TEST_KEY);
    deletedEntity.setDeleted(LocalDateTime.now());

    T newEntity = createNewEntity("name");
    BeanUtils.copyProperties(deletedEntity, newEntity);
    newEntity.setDeleted(null);

    // mock
    when(baseMapper.get(TEST_KEY)).thenReturn(deletedEntity).thenReturn(newEntity);
    doNothing().when(baseMapper).update(newEntity);

    assertEquals(newEntity, baseService.update(newEntity));
  }

  @Test
  public void deleteTest() {
    // mock
    doNothing().when(baseMapper).delete(TEST_KEY);

    assertDoesNotThrow(() -> baseService.delete(TEST_KEY));
  }

  abstract T createNewEntity(String name);

  protected void mockCreateEntity(T entity) {
    doAnswer(
      invocation -> {
        T createdEntity = invocation.getArgument(0);
        createdEntity.setKey(TEST_KEY);
        return createdEntity;
      })
      .when(baseMapper)
      .create(entity);
  }
}
