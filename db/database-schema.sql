DROP TRIGGER IF EXISTS tsvector_doc_update ON documents;

DROP TABLE IF EXISTS citations;
DROP TABLE IF EXISTS documents;

CREATE TABLE documents (
	doc_id				bigserial PRIMARY KEY,
	doi					varchar(100) UNIQUE,
	title				text,
	authors 			varchar(255),
	keywords 			text,
	abstract 			text,
	publication_date	varchar(255),
	volume				varchar(100),
	pages				varchar(100),
	issue				varchar(100),
	container			varchar(255),
	container_issn		varchar(100),
	language			regconfig,
	tsv					tsvector
);

CREATE TABLE citations (
	id			bigserial PRIMARY KEY,
	doc_id		bigint REFERENCES documents(doc_id) ON UPDATE CASCADE ON DELETE CASCADE,
	ref_id		bigint REFERENCES documents(doc_id) ON UPDATE CASCADE ON DELETE CASCADE,
	UNIQUE( doc_id, ref_id)
);

CREATE INDEX source_idx ON citations(doc_id);
CREATE INDEX target_idx ON citations(ref_id);

ALTER TABLE citations ADD CONSTRAINT no_self_loops_chk CHECK (doc_id <> ref_id);

CREATE OR REPLACE FUNCTION documents_trigger() RETURNS TRIGGER AS $documents_trigger$
	BEGIN
  		new.tsv :=
	     setweight(to_tsvector(new.language, coalesce(new.title,'')), 'D') ||
	     setweight(to_tsvector(new.language, coalesce(new.keywords,'')), 'B') ||
	     setweight(to_tsvector(new.language, coalesce(new.abstract,'')), 'C');
  	return new;
	END;
$documents_trigger$ LANGUAGE plpgsql;

CREATE TRIGGER tsvector_doc_update BEFORE INSERT OR UPDATE
    ON documents FOR EACH ROW EXECUTE PROCEDURE documents_trigger();
