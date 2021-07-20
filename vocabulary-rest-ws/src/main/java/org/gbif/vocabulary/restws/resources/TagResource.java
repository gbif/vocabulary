package org.gbif.vocabulary.restws.resources;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Tag;
import org.gbif.vocabulary.service.TagService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.gbif.vocabulary.restws.utils.Constants.TAGS_PATH;

import static com.google.common.base.Preconditions.checkArgument;

@RestController
@RequestMapping(value = TAGS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class TagResource {

  private final TagService tagService;

  TagResource(TagService tagService) {
    this.tagService = tagService;
  }

  @GetMapping
  public PagingResponse<Tag> listTags(Pageable page) {
    return tagService.list(page);
  }

  @GetMapping("{name}")
  public Tag getTag(@PathVariable("name") String tagName) {
    return tagService.getByName(tagName);
  }

  @PostMapping
  public Tag create(@RequestBody Tag tag) {
    int key = tagService.create(tag);
    return tagService.get(key);
  }

  @PutMapping("{name}")
  public Tag update(@PathVariable("name") String tagName, @RequestBody Tag tag) {
    Tag oldTag = tagService.getByName(tagName);
    checkArgument(oldTag != null, "Tag not found for name " + tagName);
    checkArgument(
        oldTag.getKey().equals(tag.getKey()), "Key of the tag different than the persisted one");

    tagService.update(tag);
    return tagService.get(tag.getKey());
  }

  @DeleteMapping("{name}")
  public void delete(@PathVariable("name") String tagName) {
    Tag tag = tagService.getByName(tagName);
    checkArgument(tag != null, "Tag not found for name " + tagName);
    tagService.delete(tag.getKey());
  }
}
