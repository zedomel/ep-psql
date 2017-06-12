package services.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import model.Document;
import play.db.Database;

public class DatabaseService {
	
	public static float minimumPercentOfTerms = 0.01f;

	private static final String SEARCH_SQL = "SELECT d.doc_id, d.doi, d.title, d.keywords, d.publication_date, "
			+ "dd.x, dd.y, dd.relevance, ts_rank(tsv, query, 32) rank , authors_name FROM documents d "
			+ "INNER JOIN documents_data dd ON d.doc_id = dd.doc_id LEFT JOIN "
			+ "(SELECT doc_id, string_agg(a.aut_name,';') authors_name FROM document_authors da INNER JOIN authors a "
			+ "ON da.aut_id = a.aut_id GROUP BY da.doc_id) a ON d.doc_id = a.doc_id, "
			+ "to_tsquery(?) query WHERE query @@ tsv ORDER BY doc_id LIMIT ?";

	private static final String SEARCH_SQL_ALL = "SELECT d.doc_id, d.doi, d.title, d.keywords, d.publication_date, "
			+ "dd.x, dd.y, dd.relevance , dd.relevance rank, authors_name FROM documents d "
			+ "INNER JOIN documents_data dd ON d.doc_id = dd.doc_id LEFT JOIN "
			+ "(SELECT doc_id, string_agg(a.aut_name,';') authors_name FROM document_authors da INNER JOIN authors a "
			+ "ON da.aut_id = a.aut_id GROUP BY da.doc_id) a ON d.doc_id = a.doc_id "
			+ "ORDER BY doc_id";

	private static final String ADVANCED_SEARCH_SQL = "SELECT d.doc_id, d.doi, d.title, d.keywords, d.publication_date, "
			+ "dd.x, dd.y, dd.relevance %rank, authors_name FROM documents d "
			+ "INNER JOIN documents_data dd ON d.doc_id = dd.doc_id LEFT JOIN "
			+ "(SELECT doc_id, string_agg(a.aut_name,';') authors_name, array_to_tsvector2(array_agg(aut_name_tsv)) aut_name_tsv FROM document_authors da INNER JOIN authors a "
			+ "ON da.aut_id = a.aut_id GROUP BY da.doc_id) a ON d.doc_id = a.doc_id "
			+ "%advanced_query ORDER BY doc_id LIMIT ?";

	private static final String GRAPH_SQL = "with nodes as (select row_number() over(order by doc_id ) as row_number, "
			+ "doc_id n from documents) "
			+ "select doc_id as source,row_number as target from citations c "
			+ "inner join nodes ON c.ref_id = nodes.n order by doc_id, row_number";

	private Database db;

	public DatabaseService(Database db) {
		this.db = db;
	}

	public List<Document> getSimpleDocuments(String querySearch, int limit) throws Exception {
		try ( Connection conn = db.getConnection();){
			PreparedStatement stmt = conn.prepareStatement(SEARCH_SQL);
			stmt.setString(1, querySearch);
			if ( limit > 0)
				stmt.setInt(2, limit);
			else
				stmt.setNull(2, java.sql.Types.INTEGER);

			try (ResultSet rs = stmt.executeQuery()){
				List<Document> docs = new ArrayList<>();
				while ( rs.next() ){
					Document doc = newSimpleDocument( rs );
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

	public List<Document> getAllSimpleDocuments() throws Exception {
		try ( Connection conn = db.getConnection();){
			PreparedStatement stmt = conn.prepareStatement(SEARCH_SQL_ALL);

			try (ResultSet rs = stmt.executeQuery()){
				List<Document> docs = new ArrayList<>();
				while ( rs.next() ){
					Document doc = newSimpleDocument( rs );
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

	public List<Document> getAdvancedSimpleDocuments(String querySearch, String authors, String yearStart, 
			String yearEnd, int limit) throws Exception {
		try ( Connection conn = db.getConnection();){
			
			StringBuilder sql = new StringBuilder();
			StringBuilder rankSql = new StringBuilder();
			
			if ( querySearch != null ){
				sql.append(", to_tsquery(?) query");
				rankSql.append(", ts_rank(tsv, query, 32) ");
			}
			if ( !authors.isEmpty() ){
				sql.append( ", to_tsquery(?) aut_query");
				if ( querySearch != null)
					rankSql.append(" + ts_rank(aut_name_tsv, aut_query, 32)");
				else
					rankSql.append(", ts_rank(aut_name_tsv, aut_query, 32)");
			}
			
			if ( querySearch != null || !authors.isEmpty())
				rankSql.append(" rank ");
			else
				rankSql.append(", dd.relevance rank");
			
			sql.append(" WHERE ");
			if ( querySearch != null )
				sql.append("query @@ tsv AND ");
			if ( !authors.isEmpty() )
				sql.append( "aut_query @@ aut_name_tsv AND ");
			if ( !yearStart.isEmpty() )
				sql.append("publication_date >= ? AND ");
			if ( !yearEnd.isEmpty() )
				sql.append("publication_date <= ? AND ");
			sql.append("TRUE");
			
			PreparedStatement stmt = conn.prepareStatement(
					ADVANCED_SEARCH_SQL
					.replaceAll("%rank", rankSql.toString())
					.replaceAll("%advanced_query", sql.toString())
					);
			
			int index = 1;
			if (querySearch != null)
				stmt.setString(index++, querySearch);
			if ( !authors.isEmpty() )
				stmt.setString(index++, authors);
			if ( !yearStart.isEmpty() )
				stmt.setInt(index++, Integer.parseInt(yearStart));
			if ( !yearEnd.isEmpty() )
				stmt.setInt(index++, Integer.parseInt(yearEnd));
			
			if ( limit > 0 )
				stmt.setInt(index, limit);
			else
				stmt.setNull(index, java.sql.Types.INTEGER);

			try (ResultSet rs = stmt.executeQuery()){
				List<Document> docs = new ArrayList<>();
				while ( rs.next() ){
					Document doc = newSimpleDocument( rs );
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

	public int getNumberOfDocuments() throws Exception {
		try ( Connection conn = db.getConnection();){
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT count(*) FROM documents");
			if ( rs.next() ){
				return rs.getInt(1);
			}
			return 0;
		}catch( Exception e){
			throw e;
		}
	}

	public DoubleMatrix2D getGraph() throws Exception{
		try ( Connection conn = db.getConnection();){
			// Recupera numero de citacoes para pre-alocacao
			// da matriz de adjacencia.
			int size = getNumberOfDocuments();
			if ( size == 0)
				return null;

			// Recupera citacoes para construção do 
			// grafo
			PreparedStatement stmt = conn.prepareStatement(GRAPH_SQL);
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

	private Document newSimpleDocument(ResultSet rs) throws SQLException {
		//d.doc_id, d.doi, d.title, d.keywords, d.publication_date,
		//dd.x, dd.y, dd.relevance , dd.relevance rank, authors_name
		Document doc = new Document();
		doc.setDocId( rs.getLong(1) );
		doc.setDOI( rs.getString(2) );
		doc.setTitle( rs.getString(3) );
		doc.setKeywords(rs.getString(4));
		doc.setPublicationDate(rs.getString(5));
		doc.setX(rs.getDouble(6));
		doc.setY(rs.getDouble(7));
		doc.setRelevance(rs.getDouble(8));
		doc.setScore(rs.getDouble(9));
		doc.setAuthors(rs.getString(10));
		
		return doc;
	}

	public DoubleMatrix2D buildFrequencyMatrix(long[] docIds) throws Exception {
		// Retorna numero de documentos e ocorrencia total dos termos
		int numberOfDocuments;
		if ( docIds == null )
			numberOfDocuments = getNumberOfDocuments();
		else
			numberOfDocuments = docIds.length;

		// Constroi consulta caso docIds != null
		StringBuilder sql = new StringBuilder();
		if ( docIds != null ){
			sql.append(" WHERE doc_id IN (");
			sql.append(docIds[0]);
			for(int i = 1; i < docIds.length; i++){
				sql.append(",");
				sql.append(docIds[i]);
			}
			sql.append(")");
		}

		float minNumberOfTerms = DatabaseService.minimumPercentOfTerms;
		int numOfTerms = (int) Math.ceil(numberOfDocuments * minNumberOfTerms);
		
		String where = sql.toString();
		final Map<String, Integer> termsFreq = getTermsCounts(where,numOfTerms);
		final Map<String, Integer> termsToColumnMap = new HashMap<>();

		// Mapeamento termo -> coluna na matriz (bag of words)
		int c = 0;
		for(String key : termsFreq.keySet()){
			termsToColumnMap.put(key, c);
			++c;
		}

		DoubleMatrix2D matrix = new SparseDoubleMatrix2D(numberOfDocuments, termsFreq.size());

		// Popula matriz com frequencia dos termos em cada documento
		buildFrequencyMatrix(matrix, termsFreq, termsToColumnMap, where, true );

		return matrix;
	}

	private TreeMap<String, Integer> getTermsCounts(String where, int minNumberOfTerms) throws Exception {
		try ( Connection conn = db.getConnection();){

			String sql = "SELECT word,ndoc FROM ts_stat('SELECT tsv FROM documents";
			if ( where != null && !where.isEmpty() )
				sql += where;
			sql += String.format("') WHERE nentry > 1 AND ndoc > %d", minNumberOfTerms);

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			TreeMap<String,Integer> termsFreq = new TreeMap<>();
			while( rs.next() ){
				String term = rs.getString("word");
				int freq = rs.getInt("ndoc");
				termsFreq.put(term, freq);
			}
			return termsFreq;

		}catch( Exception e){
			throw e;
		}
	}

	public void buildFrequencyMatrix(DoubleMatrix2D matrix, Map<String, Integer> termsFreq,
			Map<String, Integer> termsToColumnMap, String where, boolean normalize) throws Exception {
		try ( Connection conn = db.getConnection();){

			String sql = "SELECT freqs FROM documents";
			if ( where != null)
				sql += where;
			sql += " ORDER BY doc_id";

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			
			int doc = 0;
			int n = matrix.rows();
			ObjectMapper mapper = new ObjectMapper();
			
			while( rs.next() ){
				String terms = rs.getString("freqs");
				if ( terms != null && !terms.isEmpty() ){
					
					List<Map<String,Object>> t = mapper.readValue(terms, 
							new TypeReference<List<Map<String,Object>>>(){});
					
					for(Map<String,Object> o : t){
						String term = (String) o.get("word");
						if ( termsToColumnMap.containsKey(term)){
							double freq = ((Number) o.get("freq")).doubleValue();
							
							// 1 + log f(t,d)
							double tfidf = 1;
							if ( freq != 0 )
								tfidf += Math.log(freq);
							
							// ( 1 + log f(t,d) ) * ( log N / ndoc(t) )
							tfidf *= Math.log(n/(1.0 + termsFreq.get(term)));
							
							int col = termsToColumnMap.get(term);
							matrix.setQuick(doc, col, tfidf);
						}
					}
				}
				++doc;
			}
		}catch( Exception e){
			throw e;
		}
	}

	public DoubleMatrix2D getCitationGraph() throws Exception {

		final Map<Long, Integer> docIndexMap = getDocumentsIndexMapping();
		int n = docIndexMap.size();

		try ( Connection conn = db.getConnection();){
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT doc_id, ref_id FROM citations ORDER BY doc_id, ref_id");
			DoubleMatrix2D graph = new SparseDoubleMatrix2D(n, n);
			while( rs.next() ){
				long docId = rs.getLong(1);
				long refId = rs.getLong(2);

				int source = docIndexMap.get(docId);
				int target = docIndexMap.get(refId);

				graph.setQuick(source, target, 1.0);
			}
			return graph;

		}catch( Exception e){
			throw e;
		}
	}


	public Map<Long, Integer> getDocumentsIndexMapping() throws Exception {
		try ( Connection conn = db.getConnection();){
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(" SELECT row_number() OVER(order by doc_id) - 1, doc_id FROM documents "
					+ "ORDER BY doc_id;");
			Map<Long, Integer> map = new HashMap<>();
			while( rs.next() ){
				int index = rs.getInt(1);
				long docId = rs.getLong(2);
				map.put(docId, index);
			}
			return map;
		}catch( Exception e){
			throw e;
		}
	}

	public Map<Long, List<Long>> getReferences(long[] docIds) throws Exception {
		try ( Connection conn = db.getConnection();){
			String sql = "SELECT doc_id, ref_id FROM citations";
			if ( docIds != null ){
				StringBuilder sb = new StringBuilder();
				sb.append(docIds[0]);
				for(int i = 1; i < docIds.length; i++){
					sb.append(",");
					sb.append(docIds[i]);
				}
				sql = sql + " WHERE doc_id IN(" + sb.toString() + ") AND ref_id IN(" + sb.toString() + ")";
			}
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql.toString());

			Map<Long, List<Long>> map = new HashMap<>();
			while( rs.next() ){
				long docId = rs.getInt(1);
				long refId = rs.getLong(2);
				List<Long> refs = map.get(docId);
				if ( refs == null){
					refs = new ArrayList<>();
					map.put(docId, refs);
				}
				refs.add(refId);
			}
			return map;
		}catch( Exception e){
			throw e;
		}
	}
}
