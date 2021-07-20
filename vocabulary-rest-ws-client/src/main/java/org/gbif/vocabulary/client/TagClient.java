package org.gbif.vocabulary.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Tag;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("vocabularyTags")
public interface TagClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  PagingResponse<Tag> listTags(@SpringQueryMap Pageable page);

  @GetMapping(value = "{name}", produces = MediaType.APPLICATION_JSON_VALUE)
  Tag getTag(@PathVariable("name") String tagName);

  @PostMapping(
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Tag create(@RequestBody Tag tag);

  default Tag update(@RequestBody Tag tag) {
    return update(tag.getName(), tag);
  }

  @PutMapping(
      value = "{name}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Tag update(@PathVariable("name") String tagName, @RequestBody Tag tag);

  @DeleteMapping("{name}")
  void delete(@PathVariable("name") String tagName);
}
