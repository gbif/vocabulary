CREATE EXTENSION unaccent;
CREATE EXTENSION hstore;

CREATE FUNCTION assert_min_length(input text, minlength integer) RETURNS boolean AS $$ DECLARE length integer; BEGIN length := char_length(trim(input)); IF (length IS NULL) OR (length >= minlength) THEN RETURN TRUE; ELSE RETURN FALSE; END IF; END; $$ LANGUAGE plpgsql;


CREATE TABLE vocabulary(
  key serial NOT NULL PRIMARY KEY,
  namespace text CHECK (assert_min_length(namespace, 1)),
  name text NOT NULL UNIQUE CHECK (assert_min_length(name, 1)),
  label hstore,
  definition hstore,
  external_definition_urls text[],
  editorial_notes text[],
  replaced_by_key integer REFERENCES vocabulary(key),
  deprecated_by varchar NULL CHECK (assert_min_length(created_by, 1)),
  deprecated timestamp with time zone NULL,
  created_by varchar NOT NULL CHECK (assert_min_length(created_by, 1)),
  modified_by varchar NOT NULL CHECK (assert_min_length(modified_by, 1)),
  created timestamp with time zone NOT NULL DEFAULT now(),
  modified timestamp with time zone NOT NULL DEFAULT now(),
  deleted timestamp with time zone NULL,
  fulltext_search tsvector
);

CREATE INDEX vocabulary_fulltext_search_idx ON vocabulary USING gin(fulltext_search);

CREATE OR REPLACE FUNCTION vocabulary_change_trigger()
RETURNS TRIGGER AS
$vocabchange$
    BEGIN
      NEW.fulltext_search :=
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.namespace,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.name,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(avals(NEW.label), ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(avals(NEW.definition), ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.external_definition_urls, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.editorial_notes, ' '),'')));
      RETURN NEW;
    END;
$vocabchange$
LANGUAGE plpgsql;

CREATE TRIGGER vocabulary_fulltext_update
  BEFORE INSERT OR UPDATE ON vocabulary
  FOR EACH ROW EXECUTE PROCEDURE vocabulary_change_trigger();


CREATE TABLE concept(
  key serial NOT NULL PRIMARY KEY,
  vocabulary_key integer NOT NULL REFERENCES vocabulary(key),
  parent_key integer REFERENCES concept(key),
  replaced_by_key integer REFERENCES concept(key),
  name text NOT NULL CHECK (assert_min_length(name, 1)),
  label hstore,
  alternative_labels hstore,
  misspelt_labels hstore,
  definition hstore,
  external_definition_urls text[],
  same_as_uris text[],
  editorial_notes text[],
  deprecated_by varchar NULL CHECK (assert_min_length(created_by, 1)),
  deprecated timestamp with time zone NULL,
  created_by varchar NOT NULL CHECK (assert_min_length(created_by, 1)),
  modified_by varchar NOT NULL CHECK (assert_min_length(modified_by, 1)),
  created timestamp with time zone NOT NULL DEFAULT now(),
  modified timestamp with time zone NOT NULL DEFAULT now(),
  deleted timestamp with time zone NULL,
  fulltext_search tsvector
);

CREATE INDEX concept_fulltext_search_idx ON concept USING gin(fulltext_search);

CREATE OR REPLACE FUNCTION concept_change_trigger()
RETURNS TRIGGER AS
$conceptchange$
    BEGIN
      NEW.fulltext_search :=
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.name,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(avals(NEW.label), ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(avals(NEW.alternative_labels), ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(avals(NEW.misspelt_labels), ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(avals(NEW.definition), ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.external_definition_urls, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.same_as_uris, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.editorial_notes, ' '),'')));
      RETURN NEW;
    END;
$conceptchange$
LANGUAGE plpgsql;

CREATE TRIGGER concept_fulltext_update
  BEFORE INSERT OR UPDATE ON concept
  FOR EACH ROW EXECUTE PROCEDURE concept_change_trigger();