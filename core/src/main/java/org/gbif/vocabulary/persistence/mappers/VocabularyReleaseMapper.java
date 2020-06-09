package org.gbif.vocabulary.persistence.mappers;

import java.util.List;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.VocabularyRelease;

import org.apache.ibatis.annotations.Param;

import javax.annotation.Nullable;

/** Mapper for {@link VocabularyRelease}. */
public interface VocabularyReleaseMapper {

  void create(VocabularyRelease vocabularyRelease);

  VocabularyRelease get(@Param("key") long key);

  List<VocabularyRelease> list(
      @Nullable @Param("vocabularyKey") Long vocabularyKey,
      @Nullable @Param("version") String version,
      @Nullable @Param("page") Pageable page);

  long count(
      @Nullable @Param("vocabularyKey") Long vocabularyKey,
      @Nullable @Param("version") String version);
}
