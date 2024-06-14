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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Definition;
import org.gbif.vocabulary.model.HiddenLabel;
import org.gbif.vocabulary.model.Label;
import org.gbif.vocabulary.model.LanguageRegion;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.model.UserRoles;
import org.gbif.vocabulary.model.search.ChildrenResult;
import org.gbif.vocabulary.model.search.ConceptSearchParams;
import org.gbif.vocabulary.model.search.KeyNameResult;
import org.gbif.vocabulary.model.search.SuggestResult;
import org.gbif.vocabulary.model.utils.PostPersist;
import org.gbif.vocabulary.model.utils.PrePersist;
import org.gbif.vocabulary.persistence.dto.ParentDto;
import org.gbif.vocabulary.persistence.dto.SuggestDto;
import org.gbif.vocabulary.persistence.mappers.ConceptMapper;
import org.gbif.vocabulary.persistence.mappers.VocabularyMapper;
import org.gbif.vocabulary.service.ConceptService;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.NAME_FORMAT_PATTERN;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.isValidName;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeLabel;
import static org.gbif.vocabulary.model.normalizers.StringNormalizer.normalizeName;

/** Default implementation for {@link ConceptService}. */
@Service
@Validated
public class DefaultConceptService implements ConceptService {

  private static final int DEFAULT_SUGGEST_LIMIT = 20;
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

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Validated({PrePersist.class, Default.class})
  @Transactional
  @Override
  public long create(@NotNull @Valid Concept concept) {
    checkArgument(concept.getKey() == null, "Can't create a concept which already has a key");

    // checking the format of the name
    checkArgument(
        isValidName(concept.getName()),
        "Entity name has to match the regex " + NAME_FORMAT_PATTERN.pattern());

    // checking if there are other vocabs with the same name
    List<KeyNameResult> similarities =
        conceptMapper.findSimilarities(
            normalizeName(concept.getName()), null, concept.getVocabularyKey(), null);
    if (!similarities.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot create entity because it conflicts with other entities, e.g.: " + similarities);
    }

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

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
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
  public List<SuggestResult> suggest(
      String query,
      long vocabularyKey,
      @Nullable LanguageRegion languageRegion,
      @Nullable LanguageRegion fallbackLanguageRegion,
      Integer limit) {
    query = query != null ? query : "";
    limit = limit != null ? Math.max(limit, DEFAULT_SUGGEST_LIMIT) : DEFAULT_SUGGEST_LIMIT;
    List<SuggestDto> dtos =
        conceptMapper.suggest(query, vocabularyKey, languageRegion, fallbackLanguageRegion, limit);
    return convertSuggestResults(dtos);
  }

  private static List<SuggestResult> convertSuggestResults(List<SuggestDto> dtos) {
    return dtos.stream()
        .map(
            dto -> {
              SuggestResult suggestResult = new SuggestResult();
              suggestResult.setName(dto.getName());
              suggestResult.setLabel(dto.getLabel());
              suggestResult.setLabelLanguage(dto.getLabelLang());
              suggestResult.setParents(
                  dto.getParentDtos().stream()
                      .sorted(Comparator.comparing(ParentDto::getDepth))
                      .map(
                          p -> {
                            SuggestResult.Parent parent = new SuggestResult.Parent();
                            parent.setName(p.getName());
                            parent.setLabel(
                                p.getLabel() != null ? p.getLabel() : p.getFallbackLabel());
                            parent.setLabelLanguage(
                                p.getLabelLanguage() != null
                                    ? p.getLabelLanguage()
                                    : p.getFallbackLabelLanguage());
                            return parent;
                          })
                      .collect(Collectors.toList()));
              return suggestResult;
            })
        .collect(Collectors.toList());
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
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

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
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

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
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

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Override
  public void addTag(long conceptKey, int tagKey) {
    conceptMapper.addTag(conceptKey, tagKey);
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Override
  public void removeTag(long conceptKey, int tagKey) {
    conceptMapper.removeTag(conceptKey, tagKey);
  }

  @Override
  public List<Tag> listTags(long conceptKey) {
    return conceptMapper.listTags(conceptKey);
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Validated({PrePersist.class, Default.class})
  @Transactional
  @Override
  public long addDefinition(long entityKey, @NotNull @Valid Definition definition) {
    checkArgument(definition.getKey() == null, "Can't add a definition that has a key");
    checkArgument(!Strings.isNullOrEmpty(definition.getValue()), "Definition is required");
    conceptMapper.addDefinition(entityKey, definition);
    return definition.getKey();
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Validated({PostPersist.class, Default.class})
  @Transactional
  @Override
  public void updateDefinition(long entityKey, @NotNull @Valid Definition definition) {
    requireNonNull(definition.getKey());
    checkArgument(!Strings.isNullOrEmpty(definition.getValue()), "Definition is required");
    conceptMapper.updateDefinition(entityKey, definition);
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Transactional
  @Override
  public void deleteDefinition(long entityKey, long key) {
    conceptMapper.deleteDefinition(entityKey, key);
  }

  @Override
  public Definition getDefinition(long entityKey, long key) {
    return conceptMapper.getDefinition(entityKey, key);
  }

  @Override
  public List<Definition> listDefinitions(
      long entityKey, @Nullable List<LanguageRegion> languageRegions) {
    return conceptMapper.listDefinitions(entityKey, languageRegions);
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Validated({PrePersist.class, Default.class})
  @Transactional
  @Override
  public long addLabel(long entityKey, @NotNull @Valid Label label) {
    checkArgument(label.getKey() == null, "Can't add a label that has a key");
    checkArgument(!Strings.isNullOrEmpty(label.getValue()), "Label is required");

    // checking if there are other vocabs with the same label
    checkSimilarLabels(entityKey, label);

    conceptMapper.addLabel(entityKey, label);
    return label.getKey();
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Transactional
  @Override
  public void deleteLabel(long entityKey, long key) {
    conceptMapper.deleteLabel(entityKey, key);
  }

  @Override
  public List<Label> listLabels(long entityKey, @Nullable List<LanguageRegion> languageRegions) {
    return conceptMapper.listLabels(entityKey, languageRegions);
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Validated({PrePersist.class, Default.class})
  @Transactional
  @Override
  public long addAlternativeLabel(long entityKey, @NotNull @Valid Label label) {
    checkArgument(label.getKey() == null, "Can't add a label that has a key");
    checkArgument(!Strings.isNullOrEmpty(label.getValue()), "Label is required");

    // checking if there are other vocabs with the same label
    checkSimilarLabels(entityKey, label);

    conceptMapper.addAlternativeLabel(entityKey, label);
    return label.getKey();
  }

  private void checkSimilarLabels(long entityKey, Label label) {
    long vocabularyKey = conceptMapper.getVocabularyKey(entityKey);
    List<KeyNameResult> similarities =
        conceptMapper.findSimilarities(
            normalizeLabel(label.getValue()), label.getLanguage(), vocabularyKey, entityKey);
    if (!similarities.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot create entity because it conflicts with other entities, e.g.: " + similarities);
    }
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Transactional
  @Override
  public void deleteAlternativeLabel(long entityKey, long key) {
    conceptMapper.deleteAlternativeLabel(entityKey, key);
  }

  @Override
  public PagingResponse<Label> listAlternativeLabels(
      long entityKey, @Nullable List<LanguageRegion> languageRegions, @Nullable Pageable page) {
    page = page != null ? page : new PagingRequest();
    return new PagingResponse<>(
        page,
        conceptMapper.countAlternativeLabels(entityKey, languageRegions),
        conceptMapper.listAlternativeLabels(entityKey, languageRegions, page));
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Validated({PrePersist.class, Default.class})
  @Transactional
  @Override
  public long addHiddenLabel(long entityKey, @NotNull @Valid HiddenLabel label) {
    checkArgument(label.getKey() == null, "Can't add a label that has a key");
    checkArgument(!Strings.isNullOrEmpty(label.getValue()), "Label is required");

    // checking if there are other vocabs with the same label
    checkSimilarHiddenLabels(entityKey, label);

    conceptMapper.addHiddenLabel(entityKey, label);
    return label.getKey();
  }

  private void checkSimilarHiddenLabels(long entityKey, HiddenLabel label) {
    long vocabularyKey = conceptMapper.getVocabularyKey(entityKey);
    List<KeyNameResult> similarities =
        conceptMapper.findSimilarities(
            normalizeName(label.getValue()), null, vocabularyKey, entityKey);
    if (!similarities.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot create entity because it conflicts with other entities, e.g.: " + similarities);
    }
  }

  @Secured({UserRoles.VOCABULARY_ADMIN, UserRoles.VOCABULARY_EDITOR})
  @Transactional
  @Override
  public void deleteHiddenLabel(long entityKey, long key) {
    conceptMapper.deleteHiddenLabel(entityKey, key);
  }

  @Override
  public PagingResponse<HiddenLabel> listHiddenLabels(long entityKey, @Nullable Pageable page) {
    page = page != null ? page : new PagingRequest();
    return new PagingResponse<>(
        page,
        conceptMapper.countHiddenLabels(entityKey),
        conceptMapper.listHiddenLabels(entityKey, page));
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

  @Override
  public boolean existsLatestReleaseView(String vocabularyName) {
    return conceptMapper.existsReleaseView(vocabularyName.toLowerCase());
  }

  @Override
  public void createLatestReleaseView(String vocabularyName, long vocabularyKey) {
    conceptMapper.createLatestReleaseView(vocabularyName, vocabularyKey);
  }

  @Override
  public void updateLatestReleaseView(String vocabularyName) {
    conceptMapper.updateReleaseViews(vocabularyName);
  }

  @Override
  public PagingResponse<Concept> listLatestRelease(
      ConceptSearchParams params, Pageable page, String vocabularyName) {
    checkArgument(!Strings.isNullOrEmpty(vocabularyName));
    checkArgument(conceptMapper.existsReleaseView(vocabularyName.toLowerCase()));

    page = page != null ? page : new PagingRequest();
    params = params != null ? params : ConceptSearchParams.empty();

    vocabularyName = vocabularyName.toLowerCase();

    return new PagingResponse<>(
        page,
        conceptMapper.countLatestRelease(params, vocabularyName),
        conceptMapper.listLatestRelease(params, page, vocabularyName));
  }

  @Override
  public List<SuggestResult> suggestLatestRelease(
      String query,
      long vocabularyKey,
      LanguageRegion languageRegion,
      LanguageRegion fallbackLanguageRegion,
      String vocabularyName,
      Integer limit) {
    checkArgument(!Strings.isNullOrEmpty(vocabularyName));
    checkArgument(conceptMapper.existsReleaseView(vocabularyName.toLowerCase()));

    query = query != null ? query : "";
    limit = limit != null ? Math.max(limit, DEFAULT_SUGGEST_LIMIT) : DEFAULT_SUGGEST_LIMIT;
    List<SuggestDto> dtos =
        conceptMapper.suggestLatestRelease(
            query,
            vocabularyKey,
            languageRegion,
            fallbackLanguageRegion,
            vocabularyName.toLowerCase(),
            limit);

    return convertSuggestResults(dtos);
  }

  @Override
  public Concept getByNameLatestRelease(String name, String vocabularyName) {
    checkArgument(!Strings.isNullOrEmpty(vocabularyName));
    checkArgument(conceptMapper.existsReleaseView(vocabularyName.toLowerCase()));
    return conceptMapper.getByNameLatestRelease(name, vocabularyName.toLowerCase());
  }

  @Override
  public List<String> findParentsLatestRelease(long conceptKey, String vocabularyName) {
    checkArgument(!Strings.isNullOrEmpty(vocabularyName));
    checkArgument(conceptMapper.existsReleaseView(vocabularyName.toLowerCase()));
    return conceptMapper.findParentsLatestRelease(conceptKey, vocabularyName.toLowerCase());
  }

  @Override
  public List<ChildrenResult> countChildrenLatestRelease(
      List<Long> conceptParents, String vocabularyName) {
    checkArgument(!Strings.isNullOrEmpty(vocabularyName));
    checkArgument(conceptMapper.existsReleaseView(vocabularyName.toLowerCase()));
    Preconditions.checkArgument(
        conceptParents != null && !conceptParents.isEmpty(), "concept parents are required");
    return conceptMapper.countChildrenLatestRelease(conceptParents, vocabularyName.toLowerCase());
  }

  @Override
  public List<Definition> listDefinitionsLatestRelease(
      long entityKey, List<LanguageRegion> languageRegions, String vocabularyName) {
    return conceptMapper.listDefinitionsLatestRelease(entityKey, languageRegions, vocabularyName);
  }

  @Override
  public List<Label> listLabelsLatestRelease(
      long entityKey, List<LanguageRegion> languageRegions, String vocabularyName) {
    return conceptMapper.listLabelsLatestRelease(entityKey, languageRegions, vocabularyName);
  }

  @Override
  public PagingResponse<Label> listAlternativeLabelsLatestRelease(
      long entityKey, List<LanguageRegion> languageRegions, Pageable page, String vocabularyName) {
    page = page != null ? page : new PagingRequest();
    return new PagingResponse<>(
        page,
        conceptMapper.countAlternativeLabelsLatestRelease(
            entityKey, languageRegions, vocabularyName),
        conceptMapper.listAlternativeLabelsLatestRelease(
            entityKey, languageRegions, page, vocabularyName));
  }

  @Override
  public PagingResponse<HiddenLabel> listHiddenLabelsLatestRelease(
      long entityKey, Pageable page, String vocabularyName) {
    page = page != null ? page : new PagingRequest();
    return new PagingResponse<>(
        page,
        conceptMapper.countHiddenLabelsLatestRelease(entityKey, vocabularyName),
        conceptMapper.listHiddenLabelsLatestRelease(entityKey, page, vocabularyName));
  }
}
