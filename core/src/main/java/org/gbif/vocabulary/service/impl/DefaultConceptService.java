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
package org.gbif.vocabulary.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.enums.LanguageRegion;
import org.gbif.vocabulary.model.search.ChildrenResult;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.utils.PostPersist;
import org.gbif.vocabulary.model.utils.PrePersist;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;
import org.gbif.vocabulary.persistence.parameters.NormalizedValuesParam;
import org.gbif.vocabulary.service.ConceptService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import static java.util.Objects.requireNonNull;

import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeLabels;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeName;
import static org.gbif.vocabulary.persistence.parameters.NormalizedValuesParam.ALL_NODE;
import static org.gbif.vocabulary.persistence.parameters.NormalizedValuesParam.HIDDEN_NODE;
import static org.gbif.vocabulary.persistence.parameters.NormalizedValuesParam.NAME_NODE;
import static org.gbif.vocabulary.service.validator.EntityValidator.validateEntity;

import static com.google.common.base.Preconditions.checkArgument;

/** Default implementation for {@link ConceptService}. */
@Service
@Validated
public class DefaultConceptService implements ConceptService {

  private final ConceptMapper conceptMapper;
  private final VocabularyMapper vocabularyMapper;

  @Autowired
  public DefaultConceptService(ConceptMapper conceptMapper, VocabularyMapper vocabularyMapper) {
    this.conceptMapper = conceptMapper;
    this.vocabularyMapper = vocabularyMapper;
  }

  @Override
  public Concept get(long key) {
    return conceptMapper.get(key);
  }

  @Override
  public Concept getByNameAndVocabulary(@NotBlank String name, @NotBlank String vocabularyName) {
    return conceptMapper.getByNameAndVocabulary(name, vocabularyName);
  }

  @Validated({PrePersist.class, Default.class})
  @Transactional
  @Override
  public long create(@NotNull @Valid Concept concept) {
    checkArgument(concept.getKey() == null, "Can't create a concept which already has a key");

    // checking the validity of the concept.
    validateEntity(concept, createSimilaritiesExtractor(concept, false));

    checkArgument(
        !vocabularyMapper.isDeprecated(concept.getVocabularyKey()),
        "Cannot create a concept for a deprecated vocabulary");

    if (concept.getParentKey() != null) {
      checkArgument(
          concept.getVocabularyKey().equals(conceptMapper.getVocabularyKey(concept.getParentKey())),
          "A concept and its parent must belong to the same vocabulary");
      checkArgument(
          !conceptMapper.isDeprecated(concept.getParentKey()),
          "Cannot create a concept with a deprecated parent");
    }

    conceptMapper.create(concept);

    return concept.getKey();
  }

  @Validated({PostPersist.class, Default.class})
  @Transactional
  @Override
  public void update(@NotNull @Valid Concept concept) {
    requireNonNull(concept.getKey());

    Concept oldConcept = conceptMapper.get(concept.getKey());
    requireNonNull(oldConcept, "Couldn't find concept with key: " + concept.getKey());

    if (!Objects.equals(oldConcept.getVocabularyKey(), concept.getVocabularyKey())) {
      throw new IllegalArgumentException("A concept cannot be transferred to another vocabulary");
    }

    if (!Objects.equals(oldConcept.getName(), concept.getName())) {
      throw new IllegalArgumentException("Cannot modify the name of a concept");
    }

    if (concept.getParentKey() != null
        && !Objects.equals(oldConcept.getParentKey(), concept.getParentKey())) {
      // parent is being updated
      checkArgument(
          concept.getVocabularyKey().equals(conceptMapper.getVocabularyKey(concept.getParentKey())),
          "A concept and its parent must belong to the same vocabulary");
      checkArgument(
          !conceptMapper.isDeprecated(concept.getParentKey()),
          "Cannot update a concept to a deprecated parent");
    }

    checkArgument(oldConcept.getDeprecated() == null, "Cannot update a deprecated entity");
    checkArgument(
        Objects.equals(oldConcept.getDeprecated(), concept.getDeprecated()),
        "Cannot deprecate or restore a deprecated concept while updating");
    checkArgument(Objects.equals(oldConcept.getDeprecatedBy(), concept.getDeprecatedBy()));
    checkArgument(Objects.equals(oldConcept.getReplacedByKey(), concept.getReplacedByKey()));

    checkArgument(oldConcept.getDeleted() == null, "Cannot update a deleted entity");
    checkArgument(
        Objects.equals(oldConcept.getDeleted(), concept.getDeleted()),
        "Cannot delete or restore an entity while updating");

    // validity check
    validateEntity(concept, createSimilaritiesExtractor(concept, true));

    // update the concept
    conceptMapper.update(concept);
  }

  @Override
  public PagingResponse<Concept> list(
      @Nullable ConceptSearchParams params, @Nullable Pageable page) {
    page = page != null ? page : new PagingRequest();
    params = params != null ? params : ConceptSearchParams.empty();

    return new PagingResponse<>(
        page, conceptMapper.count(params), conceptMapper.list(params, page));
  }

  @Override
  public List<KeyNameResult> suggest(@NotNull String query, long vocabularyKey) {
    return conceptMapper.suggest(query, vocabularyKey);
  }

  @Transactional
  @Override
  public void deprecate(
      long key,
      @NotBlank String deprecatedBy,
      @Nullable Long replacementKey,
      boolean deprecateChildren) {

    if (replacementKey == null) {
      deprecateWithoutReplacement(key, deprecatedBy, deprecateChildren);
      return;
    }

    checkArgument(
        conceptMapper.getVocabularyKey(key).equals(conceptMapper.getVocabularyKey(replacementKey)),
        "A concept and its replacement must belong to the same vocabulary");

    // find children
    List<Long> children = findChildrenKeys(key, false);
    if (!children.isEmpty()) {
      if (deprecateChildren) {
        // deprecate children without replacement
        conceptMapper.deprecateInBulk(children, deprecatedBy, null);
      } else {
        // reassigning children to the replacement
        conceptMapper.updateParent(children, replacementKey);
      }
    }

    // deprecate concept
    conceptMapper.deprecate(key, deprecatedBy, replacementKey);
  }

  @Transactional
  @Override
  public void deprecateWithoutReplacement(
      long key, @NotBlank String deprecatedBy, boolean deprecateChildren) {
    List<Long> children = findChildrenKeys(key, false);
    if (!children.isEmpty()) {
      if (!deprecateChildren) {
        throw new IllegalArgumentException(
            "A concept can be deprecated without replacement only if it has no children");
      }

      // deprecate children
      conceptMapper.deprecateInBulk(children, deprecatedBy, null);
    }

    conceptMapper.deprecate(key, deprecatedBy, null);
  }

  @Transactional
  @Override
  public void restoreDeprecated(long key, boolean restoreDeprecatedChildren) {
    // get the concept
    Concept concept = requireNonNull(conceptMapper.get(key));

    // check if the vocabulary is not deprecated
    if (vocabularyMapper.isDeprecated(concept.getVocabularyKey())) {
      throw new IllegalArgumentException("Cannot restore a concept whose vocabulary is deprecated");
    }

    // restore the concept
    conceptMapper.restoreDeprecated(key);

    // set the parent. If the parent is deprecated we look for its replacement
    Optional.ofNullable(concept.getParentKey())
        .ifPresent(p -> concept.setParentKey(conceptMapper.findReplacement(p)));

    if (restoreDeprecatedChildren) {
      conceptMapper.restoreDeprecatedInBulk(findChildrenKeys(key, true));
    }
  }

  @Override
  public List<String> findParents(long conceptKey) {
    return conceptMapper.findParents(conceptKey);
  }

  @Override
  public List<ChildrenResult> countChildren(List<Long> conceptParents) {
    Preconditions.checkArgument(
        conceptParents != null && !conceptParents.isEmpty(), "concept parents are required");
    return conceptMapper.countChildren(conceptParents);
  }

  /** Returns the keys of all the children of the given concept. */
  private List<Long> findChildrenKeys(long parentKey, boolean deprecated) {
    return conceptMapper
        .list(
            ConceptSearchParams.builder().parentKey(parentKey).deprecated(deprecated).build(), null)
        .stream()
        .map(Concept::getKey)
        .collect(Collectors.toList());
  }

  /**
   * Supplies the required method to the DB that checks if the name or labels of the concept
   * received are already present in any other concept.
   *
   * <p><b>NOTICE that the normalization of the name and labels has to be the same as the one the DB
   * does.</b>
   */
  private Supplier<List<KeyNameResult>> createSimilaritiesExtractor(
      Concept concept, boolean update) {
    return () -> {
      List<NormalizedValuesParam> valuesToCheck = new ArrayList<>();

      // add name
      valuesToCheck.add(
          NormalizedValuesParam.from(
              ALL_NODE, Collections.singletonList(normalizeName(concept.getName()))));

      BiFunction<LanguageRegion, Set<String>, List<NormalizedValuesParam>> normalizer =
          (lang, labels) -> {
            List<String> normalizedLabels = normalizeLabels(labels);

            return Arrays.asList(
                NormalizedValuesParam.from(lang.getLocale(), normalizedLabels),
                NormalizedValuesParam.from(NAME_NODE, normalizedLabels),
                NormalizedValuesParam.from(HIDDEN_NODE, normalizedLabels));
          };

      // add labels
      valuesToCheck.addAll(
          concept.getLabel().entrySet().stream()
              .map(e -> normalizer.apply(e.getKey(), Collections.singleton(e.getValue())))
              .flatMap(Collection::stream)
              .collect(Collectors.toList()));

      // add alternative labels
      valuesToCheck.addAll(
          concept.getAlternativeLabels().entrySet().stream()
              .map(e -> normalizer.apply(e.getKey(), e.getValue()))
              .flatMap(Collection::stream)
              .collect(Collectors.toList()));

      // add hidden labels
      valuesToCheck.add(
          NormalizedValuesParam.from(ALL_NODE, normalizeLabels(concept.getHiddenLabels())));

      return conceptMapper.findSimilarities(
          valuesToCheck, concept.getVocabularyKey(), update ? concept.getKey() : null);
    };
  }
}
