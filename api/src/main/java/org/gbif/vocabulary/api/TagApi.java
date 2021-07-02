package org.gbif.vocabulary.api;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Tag;

public interface TagApi {

  PagingResponse<Tag> listTags(Pageable page);

  Tag getTag(String tagName);

  Tag create(Tag tag);

  Tag update(String tagName, Tag tag);

  void delete(String tagName);
}
