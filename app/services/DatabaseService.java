package services;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.inject.Inject;
import javax.sql.DataSource;

import model.Document;
import play.Configuration;
import play.db.Database;
import play.db.DefaultDatabase;

public class DatabaseService {
	
	private static final String INSERT_DOC = "INSERT INTO documents (title, authors, keywords, abstract, publication_date, language ) "
			+ "VALUES (?, ?, ?, ?, ?, ?::regconfig)";
	
	private static final String DELETE_DOC = "DELETE FROM documentos WHERE doc_id = ?";

	private static final String INSERT_REF = "INSERT INTO references_citations (citation_text, citation_title, citation_authors) VALUES (?,?,?)";

	private static final String INSERT_CITATION = "INSERT INTO citations(doc_id, ref_id) VALUES (?, ?)";
	
	private DataSource db;
	
	@Inject
	public DatabaseService(DataSource db) {
		this.db = db;
	}
	
	public long addDocument(Document doc) throws Exception {
		try ( Connection conn = db.getConnection();){
			PreparedStatement stmt = conn.prepareStatement(INSERT_DOC, Statement.RETURN_GENERATED_KEYS);
			stmt.setString(1, doc.getTitle());
			stmt.setString(2, doc.getAuthors());
			stmt.setString(3, doc.getKeywords());
			stmt.setString(4, doc.getAbstract());
			stmt.setString(5, doc.getPublicationDate());
			stmt.setString(6, doc.getLanguage());
			stmt.executeUpdate();
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next())
				return rs.getLong(1);
			return -1;
		}catch( Exception e){
			throw e;
		}
	}

	//TODO: change argument to int type
	public void deleteDocument(String id) throws Exception {
		try ( Connection conn = db.getConnection();){
			PreparedStatement stmt = conn.prepareStatement(DELETE_DOC);
			stmt.setInt(1, Integer.parseInt(id));
			stmt.executeUpdate();
		}catch( Exception e){
			throw e;
		}
	}

	public long addReference(Bibliography bib) throws Exception {
		try ( Connection conn = db.getConnection();){
			PreparedStatement stmt = conn.prepareStatement(INSERT_REF, Statement.RETURN_GENERATED_KEYS);
			stmt.setString(1, Utils.normalizeCitation(bib));
			stmt.setString(2, bib.getTitle().trim());
			stmt.setString(3, bib.getAuthors().trim());
			stmt.executeUpdate();
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next())
				return rs.getLong(1);
			return -1;
		}catch( Exception e){
			throw e;
		}
		
	}

	public void addCitation(long docId, long refId) throws Exception {
		try ( Connection conn = db.getConnection();){
			PreparedStatement stmt = conn.prepareStatement(INSERT_CITATION);
			stmt.setLong(1, docId);
			stmt.setLong(2, refId);
			stmt.executeUpdate();
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
