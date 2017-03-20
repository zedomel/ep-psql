package services.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import model.Document;
import play.Configuration;
import play.db.Database;
import play.db.DefaultDatabase;

public class DatabaseSearcherService {

	private static final String SEARCH_SQL = "SELECT d.*, ts_rank(tsv, query, 32) FROM documents, ts_query(?) query "
			+ "WHERE query @@ tsv ORDER BY rank DESC";

	private static final String GRAPH_SQL = "with nodes as (select row_number() over(order by n ), n from "
			+ "(select doc_id n from citations union select ref_id n from citations) a ) "
			+ "select doc_id as source,row_number as target from citations c "
			+ "inner join nodes n ON c.ref_id = n.n order by doc_id, row_number";
			
//			"WITH nodes AS (SELECT doc_id AS n FROM citations UNION SELECT ref_id AS n "
//			+ "FROM citations ORDER BY n) SELECT n,ref_id  FROM nodes n LEFT JOIN citations c ON c.doc_id = n.n "
//			+ "ORDER BY n, ref_id;";

	private static final String COUNT_REFERENCES = "WITH nodes AS (SELECT doc_id as n FROM citations UNION SELECT ref_id FROM citations) "
			+ "SELECT count(*) FROM nodes;";

	private Database db;

	public DatabaseSearcherService(Database db) {
		this.db = db;
	}

	public List<Document> getDocuments(String querySearch, int limit) throws Exception {
		try ( Connection conn = db.getConnection();){
			String sql = SEARCH_SQL;
			if ( limit > 0)
				sql += " LIMIT " + limit;
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, querySearch);

			try (ResultSet rs = stmt.executeQuery()){
				List<Document> docs = new ArrayList<>();
				while ( rs.next() ){
					Document doc = newDocument( rs );
					docs.add(doc);
				}
				return docs;
			}catch (SQLException e) {
				throw e;
			}
		}catch( Exception e){
			throw e;
		}

	}

	public DoubleMatrix2D getGraph() throws Exception{
		try ( Connection conn = db.getConnection();){
			int size = 0;
			// Recupera numero de citacoes para pre-alocacao
			// da matriz de adjacencia.
			PreparedStatement stmt = conn.prepareStatement(COUNT_REFERENCES);
			try (ResultSet rs = stmt.executeQuery()){
				if ( rs.next()) {
					size = rs.getInt(1); 
				}
			}catch (SQLException e) {
				throw e;
			}

			if ( size == 0)
				return null;

			// Recupera citacoes para construção do 
			// grafo
			stmt = conn.prepareStatement(GRAPH_SQL);
			try (ResultSet rs = stmt.executeQuery()){
				DoubleMatrix2D graph = new SparseDoubleMatrix2D(size,size);
				graph.assign(0.0);
				
				int i = 0;
				long lastSource = 0;
				while ( rs.next() ){
					long source = rs.getLong(1);
					int target = rs.getInt(2);
					if ( lastSource != i)
						++i;
					graph.set(i, target-1, 1.0);
					lastSource = source;
				}
				
				return graph;

			}catch (SQLException e) {
				throw e;
			}

		}catch( Exception e){
			throw e;
		}
	}

	private Document newDocument(ResultSet rs) throws SQLException {
		Document doc = new Document();
		doc.setDocId( rs.getLong(1) );
		doc.setDOI( rs.getString(2) );
		doc.setTitle( rs.getString(3) );
		doc.setAuthors(rs.getString(4));
		doc.setKeywords(rs.getString(5));
		doc.setAbstract(rs.getString(6));
		doc.setPublicationDate(rs.getString(7));
		doc.setVolume(rs.getString(8));
		doc.setPages(rs.getString(9));
		doc.setIssue(rs.getString(10));
		doc.setContainer(rs.getString(11));
		doc.setISSN(rs.getString(12));
		doc.setLanguage(rs.getString(13));
		return doc;
	}

	public static void main(String[] args) throws Exception {
		
		Config conf = ConfigFactory.parseFile(new File("conf/application.conf"));
		Configuration configuration = new Configuration(conf);
		
		DefaultDatabase db = new DefaultDatabase("db.default", configuration.getConfig("db.default"));
		db.getConnection();
		
		DatabaseSearcherService ss = new DatabaseSearcherService(db);
		DoubleMatrix2D graph = ss.getGraph();
		for(int i = 0; i < graph.rows(); i++){
			for(int j = 0; j < graph.columns(); j++)
				System.out.print(String.format("%d ", (int) graph.getQuick(i, j)));
			System.out.println();
		}
	}



}
