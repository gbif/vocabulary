/*
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
package org.gbif.vocabulary.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Definition;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.UserRoles;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.VocabularySearchParams;
import org.gbif.vocabulary.model.utils.PostPersist;
import org.gbif.vocabulary.model.utils.PrePersist;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;
import org.gbif.vocabulary.service.VocabularyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import static java.util.Objects.requireNonNull;

import static org.gbif.vocabulary.model.normalizers.StringNormalizer.NAME_FORMAT_PATTERN;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.isValidName;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeName;

import static com.google.common.base.Preconditions.checkArgument;

/** Default implementation for {@link VocabularyService}. */
@Service
@Validated
public class DefaultVocabularyService implements VocabularyService {

  private final VocabularyMapper vocabularyMapper;
  private final ConceptMapper conceptMapper;

  @Autowired
  public DefaultVocabularyService(VocabularyMapper vocabularyMapper, ConceptMapper conceptMapper) {
    this.vocabularyMapper = vocabularyMapper;
    this.conceptMapper = conceptMapper;
  }

  @Override
  public Vocabulary get(long key) {
    return vocabularyMapper.get(key);
  }

  @Override
  public Vocabulary getByName(@NotBlank String name) {
    return vocabularyMapper.getByName(name);
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Validated({PrePersist.class, Default.class})
  @Transactional
  @Override
  public long create(@NotNull @Valid Vocabulary vocabulary) {
    checkArgument(vocabulary.getKey() == null, "Can't create a vocabulary which already has a key");

    // checking the format of the name
    checkArgument(
        isValidName(vocabulary.getName()),
        "Entity name has to match the regex " + NAME_FORMAT_PATTERN.pattern());

    // checking if there are other vocabs with the same name
    List<KeyNameResult> similarities =
        vocabularyMapper.findSimilarities(normalizeName(vocabulary.getName()), null);
    if (!similarities.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot create entity because it conflicts with other entities, e.g.: " + similarities);
    }

    vocabularyMapper.create(vocabulary);

    return vocabulary.getKey();
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Validated({PostPersist.class, Default.class})
  @Transactional
  @Override
  public void update(@NotNull @Valid Vocabulary vocabulary) {
    requireNonNull(vocabulary.getKey());

    Vocabulary oldVocabulary = vocabularyMapper.get(vocabulary.getKey());
    requireNonNull(oldVocabulary, "Couldn't find vocabulary with key: " + vocabulary.getKey());

    if (!Objects.equals(oldVocabulary.getName(), vocabulary.getName())) {
      throw new IllegalArgumentException("Cannot modify the name of a vocabulary.");
    }

    checkArgument(oldVocabulary.getDeprecated() == null, "Cannot update a deprecated vocabulary");
    checkArgument(
        Objects.equals(oldVocabulary.getDeprecated(), vocabulary.getDeprecated()),
        "Cannot deprecate or restore a deprecated vocabulary while updating");
    checkArgument(Objects.equals(oldVocabulary.getDeprecatedBy(), vocabulary.getDeprecatedBy()));
    checkArgument(Objects.equals(oldVocabulary.getReplacedByKey(), vocabulary.getReplacedByKey()));

    // update the vocabulary
    vocabularyMapper.update(vocabulary);
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Validated({PrePersist.class, Default.class})
  @Transactional
  @Override
  public long addDefinition(long entityKey, Definition definition) {
    checkArgument(definition.getKey() == null, "Can't add a definition that has a key");
    vocabularyMapper.addDefinition(entityKey, definition);
    return definition.getKey();
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Validated({PostPersist.class, Default.class})
  @Transactional
  @Override
  public void updateDefinition(long entityKey, @NotNull @Valid Definition definition) {
    requireNonNull(definition.getKey());
    vocabularyMapper.updateDefinition(entityKey, definition);
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Transactional
  @Override
  public void deleteDefinition(long entityKey, long key) {
    vocabularyMapper.deleteDefinition(entityKey, key);
  }

  @Override
  public Definition getDefinition(long entityKey, long key) {
    return vocabularyMapper.getDefinition(entityKey, key);
  }

  @Override
  public List<Definition> listDefinitions(
      long entityKey, @Nullable List<LanguageRegion> languageRegions) {
    return vocabularyMapper.listDefinitions(entityKey, languageRegions);
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Validated({PrePersist.class, Default.class})
  @Transactional
  @Override
  public long addLabel(long entityKey, @NotNull @Valid Label label) {
    checkArgument(label.getKey() == null, "Can't add a label that has a key");
    vocabularyMapper.addLabel(entityKey, label);
    return label.getKey();
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Transactional
  @Override
  public void deleteLabel(long entityKey, long key) {
    vocabularyMapper.deleteLabel(entityKey, key);
  }

  @Override
  public List<Label> listLabels(long entityKey, @Nullable List<LanguageRegion> languageRegions) {
    return vocabularyMapper.listLabels(entityKey, languageRegions);
  }

  @Override
  public PagingResponse<Vocabulary> list(VocabularySearchParams params, Pageable page) {
    page = page != null ? page : new PagingRequest();
    params = params != null ? params : VocabularySearchParams.empty();

    return new PagingResponse<>(
        page, vocabularyMapper.count(params), vocabularyMapper.list(params, page));
  }

  @Override
  public List<KeyNameResult> suggest(String query, @Nullable LanguageRegion languageRegion) {
    query = query != null ? query : "";
    return vocabularyMapper.suggest(query, languageRegion);
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Override
  public void deprecate(
      long key,
      @NotBlank String deprecatedBy,
      @Nullable Long replacementKey,
      boolean deprecateConcepts) {
    List<Long> concepts = findConceptsKeys(key, false);
    if (!concepts.isEmpty()) {
      if (!deprecateConcepts) {
        throw new IllegalArgumentException(
            "A vocabulary that has concepts cannot be deprecated unless its concepts are deprecated too");
      }

      // deprecate concepts
      conceptMapper.deprecateInBulk(concepts, deprecatedBy, null);
    }

    vocabularyMapper.deprecate(key, deprecatedBy, replacementKey);
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Override
  public void deprecateWithoutReplacement(
      long key, @NotBlank String deprecatedBy, boolean deprecateConcepts) {
    deprecate(key, deprecatedBy, null, deprecateConcepts);
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Override
  public void restoreDeprecated(long key, boolean restoreDeprecatedConcepts) {
    vocabularyMapper.restoreDeprecated(key);

    if (restoreDeprecatedConcepts) {
      conceptMapper.restoreDeprecatedInBulk(findConceptsKeys(key, true));
    }
  }

  @Secured(UserRoles.VOCABULARY_ADMIN)
  @Override
  public void deleteVocabulary(long vocabularyKey) {
    conceptMapper.deleteAllConcepts(vocabularyKey);
    vocabularyMapper.delete(vocabularyKey);
  }

  private List<Long> findConceptsKeys(long vocabularyKey, boolean deprecated) {
    return conceptMapper
        .list(
            ConceptSearchParams.builder()
                .vocabularyKey(vocabularyKey)
                .deprecated(deprecated)
                .build(),
            null)
        .stream()
        .map(Concept::getKey)
        .collect(Collectors.toList());
  }
}
