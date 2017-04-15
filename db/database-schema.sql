DROP TRIGGER IF EXISTS tsvector_doc_update ON documents;
DROP TRIGGER IF EXISTS tsvector_doc_update_freq ON documents;

DROP TABLE IF EXISTS citations;
DROP TABLE IF EXISTS documents_data;
DROP TABLE IF EXISTS documents;
DROP TABLE IF EXISTS options;

CREATE TABLE options(
	id		bigserial PRIMARY KEY,
	option_name varchar(30) UNIQUE,
	option_value varchar(30)
);

CREATE INDEX option_index ON options(option_name);

CREATE TABLE documents (
	doc_id				bigserial PRIMARY KEY,
	doi					varchar(100) UNIQUE,
	title				text,
	authors 			text,
	keywords 			text,
	abstract 			text,
	publication_date	int,
	volume				varchar(100),
	pages				varchar(100),
	issue				varchar(100),
	container			varchar(255),
	container_issn		varchar(100),
	language			regconfig,
	tsv					tsvector,
	freqs				jsonb
);

CREATE TABLE documents_data (
	doc_id				bigint PRIMARY KEY REFERENCES documents(doc_id) ON UPDATE CASCADE ON DELETE CASCADE,
	x					real,
	y					real,
	relevance			real
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
    
 CREATE OR REPLACE FUNCTION documents_freqs() RETURNS TRIGGER AS $documents_freqs_trigger$
 	DECLARE
 		freq	jsonb;
 	BEGIN 
	 	
	 	IF new.tsv IS NOT NULL THEN
		 	BEGIN
		   		SELECT array_to_json(array_agg(row)) INTO freq FROM (SELECT word, nentry 
		   		FROM ts_stat( format('SELECT %s::tsvector', quote_literal(new.tsv) ) ) ) row;
		    EXCEPTION
		    	WHEN NO_DATA_FOUND THEN
		    		freq := NULL;
		    END;
		END IF;
	    
	    new.freqs := freq;
	    
	  	return new;
	END;
$documents_freqs_trigger$ LANGUAGE plpgsql;

CREATE TRIGGER tsvector_doc_update_freq BEFORE INSERT OR UPDATE
    ON documents FOR EACH ROW EXECUTE PROCEDURE documents_freqs();
    
CREATE OR REPLACE FUNCTION documents_data() RETURNS TRIGGER AS $documents_data_trigger$
	BEGIN
		INSERT INTO documents_data(doc_id,x,y,relevance) VALUES (new.doc_id,0.0,0.0,0.0);
		return new;
	END;
$documents_data_trigger$ LANGUAGE plpgsql;

CREATE TRIGGER document_data_insert AFTER INSERT ON documents
	FOR EACH ROW EXECUTE PROCEDURE documents_data();
	