package services.database;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import model.Document;
import play.Configuration;
import play.db.Database;
import play.db.DefaultDatabase;

/**
 * 
 * @author jose
 *
 */
public class DatabaseParserService {

	private static final String INSERT_DOC = "INSERT INTO documents AS d (title, doi, authors, keywords, abstract, "
			+ "publication_date, volume, pages, issue, container, container_issn, language ) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::regconfig) ON CONFLICT (doi) DO UPDATE "
			+ "SET title = coalesce(d.title, excluded.title),"
			+ "authors = coalesce(d.authors, excluded.authors),"
			+ "keywords=coalesce(d.keywords, excluded.keywords), "
			+ "abstract = coalesce(d.abstract, excluded.abstract),"
			+ "publication_date = coalesce(d.publication_date, excluded.publication_date), "
			+ "volume = coalesce(d.volume, excluded.volume), "
			+ "pages = coalesce(d.pages, excluded.pages), "
			+ "issue = coalesce(d.issue, excluded.issue), "
			+ "container = coalesce(d.container, excluded.container), "
			+ "container_issn = coalesce(d.container_issn, excluded.container_issn), "
			+ "language = coalesce(d.language, excluded.language) ";

	private static final String INSERT_REFERENCE = "INSERT INTO citations(doc_id, ref_id) VALUES (?, ?) "
			+ "ON CONFLICT (doc_id, ref_id) DO NOTHING";

	private static final String DELETE_DOC = "DELETE FROM documents WHERE doc_id = ?";

	private Database db;
	
	public DatabaseParserService(Database db) {
		this.db = db;
	}

	public long addDocument(Document doc) throws Exception {
		try ( Connection conn = db.getConnection();){
			PreparedStatement stmt = conn.prepareStatement(INSERT_DOC, Statement.RETURN_GENERATED_KEYS);
			stmt.setString(1, doc.getTitle());
			stmt.setString(2, doc.getDOI());
			stmt.setString(3, doc.getAuthors());
			stmt.setString(4, doc.getKeywords());
			stmt.setString(5, doc.getAbstract());
			stmt.setString(6, doc.getPublicationDate());
			stmt.setString(7, doc.getVolume());
			stmt.setString(8, doc.getPages());
			stmt.setString(9, doc.getIssue());
			stmt.setString(10, doc.getContainer());
			stmt.setString(11, doc.getISSN());
			stmt.setString(12, doc.getLanguage());
			stmt.executeUpdate();
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next())
				return rs.getLong(1);
			return -1;
		}catch( Exception e){
			throw e;
		}
	}

	public long[] addDocuments(List<Document> bibs) throws Exception {
		try ( Connection conn = db.getConnection();){
			PreparedStatement stmt = conn.prepareStatement(INSERT_DOC, Statement.RETURN_GENERATED_KEYS);
			for( Document bib : bibs ){
				stmt.setString(1, bib.getTitle());
				stmt.setString(2, bib.getDOI());
				stmt.setString(3, bib.getAuthors());
				stmt.setString(4, bib.getKeywords());
				stmt.setString(5, bib.getAbstract());
				stmt.setString(6, bib.getPublicationDate());
				stmt.setString(7, bib.getVolume());
				stmt.setString(8, bib.getPages());
				stmt.setString(9, bib.getIssue());
				stmt.setString(10, bib.getContainer());
				stmt.setString(11, bib.getISSN());
				stmt.setString(12, bib.getLanguage());
				stmt.addBatch();
			}
			stmt.executeBatch();
			ResultSet rs = stmt.getGeneratedKeys();
			long[] ids = new long[bibs.size()];
			int i = 0;
			while (rs.next()){
				ids[i] = rs.getLong(1);
				++i;
			}
			return ids;
		}catch( Exception e){
			throw e;
		}
	}

	//TODO: change argument to int type
	public void deleteDocument(long id) throws Exception {
		try ( Connection conn = db.getConnection();){
			PreparedStatement stmt = conn.prepareStatement(DELETE_DOC);
			stmt.setLong(1, id);
			stmt.executeUpdate();
		}catch( Exception e){
			throw e;
		}
	}

	public void addReference(long docId, Document ref) throws Exception {
		long refId = addDocument(ref);
		if ( refId > 0){
			try ( Connection conn = db.getConnection();){
				PreparedStatement stmt = conn.prepareStatement(INSERT_REFERENCE);
				stmt.setLong(1, docId);
				stmt.setLong(2, refId);
				stmt.executeUpdate();
			}catch( Exception e){
				throw e;
			}
		}
	}

	public void addReferences(long docId, List<Document> refs) throws Exception {
		long[] refIds = addDocuments(refs);
		try ( Connection conn = db.getConnection();){
			PreparedStatement stmt = conn.prepareStatement(INSERT_REFERENCE);
			for(int i = 0; i < refIds.length; i++){
				if ( refIds[i] > 0){
					stmt.setLong(1, docId);
					stmt.setLong(2, refIds[i]);
					stmt.addBatch();
				}
			}
			stmt.executeBatch();
		}catch( Exception e){
			throw e;
		}
	}

	public static void main(String[] args) throws IOException {
		BufferedReader br = new BufferedReader( new FileReader("conf/application.conf"));
		StringBuilder sb = new StringBuilder();
		String line = br.readLine();
		while (line != null){
			sb.append(line);
			line = br.readLine();
		}
		br.close();

		Configuration configuration = new Configuration(sb.toString());
		Database db = new DefaultDatabase("default", configuration);
		db.getConnection();
	}
}
