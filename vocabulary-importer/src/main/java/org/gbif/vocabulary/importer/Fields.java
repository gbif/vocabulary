package org.gbif.vocabulary.importer;

import java.util.Arrays;
import java.util.List;

public class Fields {

  static final String CONCEPT = "CONCEPT";
  static final String PARENT = "PARENT";
  static final String LABEL_PREFIX = "LABEL_";
  static final String ALT_LABELS_PREFIX = "ALTERNATIVELABELS_";
  static final String DEFINITION_PREFIX = "DEFINITION_";
  static final String SAME_AS_URIS = "SAMEASURIS";
  static final String EXTERNAL_DEFINITIONS = "EXTERNALDEFINITIONS";
  static final String TAGS = "TAGS";

  static final List<String> CONCEPT_FIELDS =
      Arrays.asList(CONCEPT, PARENT, SAME_AS_URIS, EXTERNAL_DEFINITIONS);
}
