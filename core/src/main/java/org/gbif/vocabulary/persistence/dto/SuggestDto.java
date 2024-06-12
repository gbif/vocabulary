package org.gbif.vocabulary.persistence.dto;

import org.gbif.vocabulary.model.Label;

import java.util.List;

import lombok.Data;

@Data
public class SuggestDto {

  private long key;
  private String name;
  private List<Label> labels;
  private List<ParentDto> parentDtos;
}
