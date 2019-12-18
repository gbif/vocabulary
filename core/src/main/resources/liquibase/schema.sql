CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE OR REPLACE FUNCTION assert_min_length(input text, minlength integer) RETURNS boolean AS $$ DECLARE length integer; BEGIN length := char_length(trim(input)); IF (length IS NULL) OR (length >= minlength) THEN RETURN TRUE; ELSE RETURN FALSE; END IF; END; $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_json_values(input jsonb)
  RETURNS text[] AS
$func$
  DECLARE
    rec record;
    v text;
   	vals text[];
    res text[];
  BEGIN
      IF input IS NOT NULL THEN
        FOR rec IN SELECT * FROM jsonb_each(input) LOOP
           IF jsonb_typeof(rec.value) = 'array' then
            vals := array(select jsonb_array_elements(rec.value));
           else
           	vals := array[rec.value];
           END IF;
           FOREACH v IN ARRAY vals LOOP
              res := array_append(res, trim('"' FROM v));
            END LOOP;
        END LOOP;
      END IF;

      RETURN res;
  END
$func$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION normalize_name(name text)
  RETURNS text AS
$func$
  BEGIN
    RETURN lower(regexp_replace(name, '(_|-|\s)', '', 'g'));
  END
$func$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION normalize_label(label text)
  RETURNS text AS
$func$
  BEGIN
    RETURN lower(regexp_replace(label, '(\s)', '', 'g'));
  END
$func$ LANGUAGE plpgsql;

CREATE TABLE vocabulary(
  key bigserial NOT NULL PRIMARY KEY,
  namespace text CHECK (assert_min_length(namespace, 1)),
  name text NOT NULL UNIQUE CHECK (assert_min_length(name, 1)),
  label jsonb,
  definition jsonb,
  external_definition_urls text[],
  editorial_notes text[],
  replaced_by_key bigint REFERENCES vocabulary(key),
  deprecated_by varchar NULL CHECK (assert_min_length(created_by, 1)),
  deprecated timestamp with time zone NULL,
  created_by varchar NOT NULL CHECK (assert_min_length(created_by, 1)),
  modified_by varchar NOT NULL CHECK (assert_min_length(modified_by, 1)),
  created timestamp with time zone NOT NULL DEFAULT now(),
  modified timestamp with time zone NOT NULL DEFAULT now(),
  deleted timestamp with time zone NULL,
  fulltext_search tsvector,
  normalized_values text[]
);

CREATE INDEX vocabulary_fulltext_search_idx ON vocabulary USING gin(fulltext_search);

CREATE OR REPLACE FUNCTION vocabulary_change_trigger()
RETURNS TRIGGER AS
$vocabchange$
    DECLARE
      labels text[];
      definitions text[];
      v text;
    BEGIN
      labels := COALESCE(get_json_values(NEW.label), array[]::text[]);
      definitions := COALESCE(get_json_values(NEW.definition), array[]::text[]);

      NEW.fulltext_search :=
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.namespace,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.name,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(labels, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(definitions, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.external_definition_urls, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.editorial_notes, ' '),'')));

       NEW.normalized_values := array_append(NEW.normalized_values, normalize_name(NEW.name));
       FOREACH v IN ARRAY labels LOOP
          NEW.normalized_values := array_append(NEW.normalized_values, normalize_label(v));
       END LOOP;

      RETURN NEW;
    END;
$vocabchange$
LANGUAGE plpgsql;

CREATE TRIGGER vocabulary_fulltext_update
  BEFORE INSERT OR UPDATE ON vocabulary
  FOR EACH ROW EXECUTE PROCEDURE vocabulary_change_trigger();


CREATE TABLE concept(
  key bigserial NOT NULL PRIMARY KEY,
  vocabulary_key bigint NOT NULL REFERENCES vocabulary(key),
  parent_key bigint REFERENCES concept(key),
  replaced_by_key bigint REFERENCES concept(key),
  name text NOT NULL CHECK (assert_min_length(name, 1)),
  label jsonb,
  alternative_labels jsonb,
  misapplied_labels jsonb,
  definition jsonb,
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
  fulltext_search tsvector,
  normalized_values text[],
  CONSTRAINT unique_concept UNIQUE(vocabulary_key,name)
);

CREATE INDEX concept_fulltext_search_idx ON concept USING gin(fulltext_search);

CREATE OR REPLACE FUNCTION concept_change_trigger()
RETURNS TRIGGER AS
$conceptchange$
    DECLARE
      labels text[];
      alternative_labels text[];
      misapplied_labels text[];
      definitions text[];
      v text;
    BEGIN
      labels := COALESCE(get_json_values(NEW.label), array[]::text[]);
      alternative_labels := COALESCE(get_json_values(NEW.alternative_labels), array[]::text[]);
      misapplied_labels := COALESCE(get_json_values(NEW.misapplied_labels), array[]::text[]);
      definitions := COALESCE(get_json_values(NEW.definition), array[]::text[]);

      NEW.fulltext_search :=
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.name,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(labels, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(alternative_labels, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(misapplied_labels, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(definitions, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.external_definition_urls, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.same_as_uris, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.editorial_notes, ' '),'')));

       NEW.normalized_values := array_append(NEW.normalized_values, normalize_name(NEW.name));
       FOREACH v IN ARRAY labels||alternative_labels||misapplied_labels LOOP
          NEW.normalized_values := array_append(NEW.normalized_values, normalize_label(v));
       END LOOP;

      RETURN NEW;
    END;
$conceptchange$
LANGUAGE plpgsql;

CREATE TRIGGER concept_fulltext_update
  BEFORE INSERT OR UPDATE ON concept
  FOR EACH ROW EXECUTE PROCEDURE concept_change_trigger();