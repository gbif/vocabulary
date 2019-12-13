package org.gbif.vocabulary.service.impl;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.normalizers.StringNormalizer;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;
import org.gbif.vocabulary.service.ConceptService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static org.gbif.vocabulary.service.validator.EntityValidator.validateEntity;

import static java.util.Objects.requireNonNull;

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
  public Concept get(int key) {
    return conceptMapper.get(key);
  }

  @Override
  public Concept getByNameAndVocabulary(@NotBlank String name, @NotBlank String vocabularyName) {
    return conceptMapper.getByNameAndVocabulary(name, vocabularyName);
  }

  @Transactional
  @Override
  public int create(@NotNull @Valid Concept concept) {
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

    if (!Objects.equals(oldConcept.getParentKey(), concept.getParentKey())) {
      // parent is being updated
      if (concept.getParentKey() != null) {
        checkArgument(
            concept
                .getVocabularyKey()
                .equals(conceptMapper.getVocabularyKey(concept.getParentKey())),
            "A concept and its parent must belong to the same vocabulary");
        checkArgument(
            !conceptMapper.isDeprecated(concept.getParentKey()),
            "Cannot update a concept to a deprecated parent");
      }
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
        page,
        conceptMapper.count(
            params.getQuery(),
            params.getVocabularyKey(),
            params.getParentKey(),
            params.getReplacedByKey(),
            params.getName(),
            params.getDeprecated(),
            params.getKey(),
            params.getHasParent(),
            params.getHasReplacement()),
        conceptMapper.list(
            params.getQuery(),
            params.getVocabularyKey(),
            params.getParentKey(),
            params.getReplacedByKey(),
            params.getName(),
            params.getDeprecated(),
            params.getKey(),
            params.getHasParent(),
            params.getHasReplacement(),
            page));
  }

  @Override
  public List<KeyNameResult> suggest(@NotNull String query, int vocabularyKey) {
    return conceptMapper.suggest(query, vocabularyKey);
  }

  @Transactional
  @Override
  public void deprecate(
      int key,
      @NotBlank String deprecatedBy,
      @Nullable Integer replacementKey,
      boolean deprecateChildren) {

    if (replacementKey == null) {
      deprecateWithoutReplacement(key, deprecatedBy, deprecateChildren);
      return;
    }

    checkArgument(
        conceptMapper.getVocabularyKey(key).equals(conceptMapper.getVocabularyKey(replacementKey)),
        "A concept and its replacement must belong to the same vocabulary");

    // find children
    List<Integer> children = findChildrenKeys(key, false);
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
      int key, @NotBlank String deprecatedBy, boolean deprecateChildren) {
    List<Integer> children = findChildrenKeys(key, false);
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
  public void restoreDeprecated(int key, boolean restoreDeprecatedChildren) {
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
  public List<String> findParents(int conceptKey) {
    return conceptMapper.findParents(conceptKey);
  }

  /** Returns the keys of all the children of the given concept. */
  private List<Integer> findChildrenKeys(int parentKey, boolean deprecated) {
    return conceptMapper.list(null, null, parentKey, null, null, deprecated, null, null, null, null)
        .stream()
        .map(Concept::getKey)
        .collect(Collectors.toList());
  }

  private Supplier<List<KeyNameResult>> createSimilaritiesExtractor(
      Concept concept, boolean update) {
    return () -> {
      List<String> valuesToCheck =
          ImmutableList.<String>builder()
              .add(StringNormalizer.normalizeName(concept.getName()))
              .addAll(StringNormalizer.normalizeLabels(concept.getLabel().values()))
              .addAll(
                  Stream.concat(
                          concept.getAlternativeLabels().values().stream(),
                          concept.getMisappliedLabels().values().stream())
                      .flatMap(Collection::stream)
                      .map(StringNormalizer::normalizeLabel)
                      .collect(Collectors.toList()))
              .build();

      return conceptMapper.findSimilarities(
          valuesToCheck, concept.getVocabularyKey(), update ? concept.getKey() : null);
    };
  }
}
