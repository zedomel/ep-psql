DROP TRIGGER IF EXISTS tsvector_doc_update ON documents;
DROP TRIGGER IF EXISTS tsvector_ref_update ON references_citations;

DROP TABLE IF EXISTS citations;
DROP TABLE IF EXISTS references_citations;
DROP TABLE IF EXISTS documents;

CREATE TABLE documents (
	doc_id		bigserial PRIMARY KEY,
	title		text,
	authors 	varchar(255),
	keywords 	text,
	abstract 	text,
	publication_date	varchar(12),
	language	regconfig,
	tsv			tsvector
);

CREATE TABLE references_citations (
	id			bigserial PRIMARY KEY,
	doc_id		bigint REFERENCES documents(doc_id),
	citation_text	text,
	citation_title	text,
	citation_authors	varchar(255),
	citation_tsv	tsvector
);

CREATE TABLE citations (
	cit_id  bigserial PRIMARY KEY,
	doc_id	bigint REFERENCES documents(doc_id) ,
	ref_id	bigint REFERENCES references_citations(id) ,
	UNIQUE( doc_id, ref_id)
);


CREATE OR REPLACE FUNCTION documents_trigger() RETURNS TRIGGER AS $documents_trigger$
	BEGIN
  		new.tsv :=
	     setweight(to_tsvector(new.language, coalesce(new.title,'')), 'D') ||
	     setweight(to_tsvector(new.language, coalesce(new.keywords,'')), 'B') ||
	     setweight(to_tsvector(new.language, coalesce(new.abstract,'')), 'C');
  	return new;
	END;
$documents_trigger$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION references_trigger() RETURNS TRIGGER AS $$
	DECLARE
		citedBy RECORD;
	BEGIN
  		new.citation_tsv := to_tsvector('english', coalesce(new.citation_text,''));
  		
  		SELECT doc_id, ts_rank(tsv, query, 32) as rank INTO citedBy FROM documents, 
  			to_tsquery( coalesce(new.citation_title, '') || coalesce(new.citation.authors, '')) query
  			WHERE query @@ tsv ORDER BY rank DESC LIMIT 1;
  		IF NOT FOUND THEN
  			new.doc_id = NULL;
  		ELSE
  			IF citeBy.rank > 0.9 THEN
  				new.doc_ic = citeBy.doc_id;
  			END IF;
  		END IF;
  		
  	return new;
	END;
$$ LANGUAGE plpgsql;


/* CREATE OR REPLACE FUNCTION update_citations() AS $$
	BEGIN
		WITH missing_doc_id AS ( SELECT doc_id, ref_id FROM citations c INNER JOIN references_citations r ON c.ref_id = r.id AND r.doc_id = NULL )
		UPDATE WITH references_citations r SET r.doc_id = d.doc_id WHERE r.doc_id IS NULL 
	END 
$$ LANGUAGE plpgsql;
*/

CREATE TRIGGER tsvector_doc_update BEFORE INSERT OR UPDATE
    ON documents FOR EACH ROW EXECUTE PROCEDURE documents_trigger();
    
 CREATE TRIGGER tsvector_ref_update BEFORE INSERT OR UPDATE
    ON references_citations FOR EACH ROW EXECUTE PROCEDURE references_trigger();
