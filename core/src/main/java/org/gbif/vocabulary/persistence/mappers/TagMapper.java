package org.gbif.vocabulary.persistence.mappers;

import java.util.List;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.vocabulary.model.Tag;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import javax.annotation.Nullable;

@Mapper
public interface TagMapper {

  Tag get(@Param("key") int key);

  Tag getByName(@Param("name") String name);

  void create(Tag tag);

  void update(Tag tag);

  void delete(@Param("key") int key);

  List<Tag> list(@Nullable @Param("page") Pageable page);

  long count();
}
