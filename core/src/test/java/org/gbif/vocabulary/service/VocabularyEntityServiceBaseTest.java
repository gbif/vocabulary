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
package org.gbif.vocabulary.service;

import org.gbif.vocabulary.model.VocabularyEntity;
import org.gbif.vocabulary.persistence.mappers.BaseMapper;

import java.time.LocalDateTime;

import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Base class to test a {@link BaseService} that can be reused for all the implemenations.
 *
 * @param <T> {@link VocabularyEntity} to parametrize the class.
 */
abstract class VocabularyEntityServiceBaseTest<T extends VocabularyEntity>
    extends MockServiceBaseTest {

  static final long TEST_KEY = 1;

  @Test
  public void createNullEntityTest() {
    assertThrows(ConstraintViolationException.class, () -> getService().create(null));
  }

  @Test
  public void createEntityWithInvalidNameTest() {
    T entity = createNewEntity("na me");
    assertThrows(IllegalArgumentException.class, () -> getService().create(entity));

    entity.setName("na@me!");
    assertThrows(IllegalArgumentException.class, () -> getService().create(entity));
  }

  @Test
  public void createEntityWithKeyTest() {
    T entity = createNewEntity("name");
    entity.setKey(TEST_KEY);
    assertThrows(ConstraintViolationException.class, () -> getService().create(entity));
  }

  @Test
  public void getEntityTest() {
    // mock
    when(getMapper().get(TEST_KEY)).thenReturn(createNewEntity("name"));

    T entity = getService().get(TEST_KEY);
    assertEquals("name", entity.getName());
  }

  @Test
  public void updateNullEntityTest() {
    assertThrows(ConstraintViolationException.class, () -> getService().update(null));
    // null key
    assertThrows(
        ConstraintViolationException.class, () -> getService().update(createNewEntity("name")));
  }

  @Test
  public void updateWithDifferentNameTest() {
    T entityDB = createNewEntity("e1");
    entityDB.setKey(TEST_KEY);
    entityDB.setKey(TEST_KEY);
    entityDB.setDeleted(LocalDateTime.now());

    // mock
    when(getMapper().get(TEST_KEY)).thenReturn(entityDB);

    T newEntity = createNewEntity("name");
    BeanUtils.copyProperties(entityDB, newEntity);
    newEntity.setName("e2");
    assertThrows(IllegalArgumentException.class, () -> getService().update(newEntity));
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
    updatedEntity.setReplacedByKey(2L);

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
