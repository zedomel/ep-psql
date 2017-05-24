package services.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import model.Document;
import play.db.Database;
import services.clustering.KMeans;
import services.database.DatabaseService;
import views.formdata.QueryData;

public class LocalDocumentSearcher implements DocumentSearcher {

	private static final Pattern TERM_PATTERN = Pattern.compile(".*(\".*?\").*");

	private DatabaseService dbService;

	@Inject
	public LocalDocumentSearcher(Database db) {
		this.dbService = new DatabaseService(db);
	}

	@Override
	public String search(QueryData queryData) throws Exception {
		return search(queryData, false, 0);
	}

	@Override
	public String search(QueryData queryData, int count) throws Exception {
		return search(queryData, false, count);
	}

	@Override
	public String search(QueryData queryData, boolean fetchNumberOfCitations) throws Exception {
		return search(queryData, fetchNumberOfCitations, 0);
	}

	@Override
	public String search(QueryData queryData, boolean fetchNumberOfCitations, int count) throws Exception {

		String query = buildQuery(queryData);
		Map<String, Object> result = new HashMap<>();

		List<Document> docs;
		
		if ( ! queryData.getAuthor().isEmpty() || ! queryData.getYearStart().isEmpty() || ! queryData.getYearEnd().isEmpty() )
			docs = dbService.getAdvancedSimpleDocuments(query, queryData.getAuthor(), queryData.getYearStart(), 
					queryData.getYearEnd(), count);
		else if ( query != null )
			docs = dbService.getSimpleDocuments(query, count);
		else
			docs = dbService.getAllSimpleDocuments();
		
		if( docs.size() > 0){

			long docIds[] = new long[docs.size()];
			int i = 0;
			double maxX = docs.get(0).getX(), minX = docs.get(0).getX(), 
					maxY = docs.get(0).getY() , minY = docs.get(0).getY();

			for( Document doc :  docs){
				docIds[i] = doc.getDocId();

				//Update X max/min coordinates
				if ( doc.getX() > maxX )
					maxX = doc.getX();
				if ( doc.getX() < minX )
					minX = doc.getX();

				//Update Y max/min coordinates
				if ( doc.getY() > maxY )
					maxY = doc.getY();
				if ( doc.getY() < minY )
					minY = doc.getY();

				++i;
			}
			
			// References
			Map<Long, List<Long>> references = dbService.getReferences(docIds);
			for(Document doc : docs){
				doc.setReferences(references.get(doc.getDocId()));
			}
			
			DoubleMatrix2D matrix = dbService.buildFrequencyMatrix(docIds);
			
			assert docs.size() == matrix.rows();

			//Clustering
			final int numClusters = queryData.getNumClusters();
			
			KMeans kmeans = new KMeans();
			kmeans.cluster(matrix, numClusters);
			DoubleMatrix1D clusters = kmeans.getClusterAssignments();

			//Normalize <x,y> coordinates to [-1,1]
			i = 0;
			for( Document doc : docs){
				doc.setX( 2 * (doc.getX() - minX)/(maxX-minX) - 1 );
				doc.setY( 2 * (doc.getY() - minY)/(maxY-minY) - 1 );
				doc.setCluster((int)clusters.get(i));
				++i;
			}

			// Sort by relevance (descending)
			docs.sort(Comparator.comparing((Document d) -> d.getRelevance()).reversed());

			result.put("documents", docs);
			result.put("nclusters", numClusters);
		}
		else{
			result.put("documents", new ArrayList<Document>(0));
			result.put("nclusters", 0);
		}

		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(result);
	}

	private String buildQuery(QueryData queryData) {
		String terms = queryData.getTerms();
		if ( terms.trim().isEmpty() )
			return null;

		String op;
		switch (queryData.getOperator()) {
		case "or":		
			op = "|";
			break;
		case "and":
			op = "&";
			break;
		default:
			op = "|";
			break;
		}

		StringBuilder query = new StringBuilder();
		// Checa consulta usando padrão para expressões entre aspas:
		// busca por frases
		Matcher m = TERM_PATTERN.matcher(terms);
		while( m.matches() ){
			// Extrai grupo entre aspas;
			String term = m.group(1);
			// Atualiza String de busca removendo grupo
			// extraido
			String f = terms.substring(0, m.start(1));
			String l = terms.substring(m.end(1)); 
			terms =  f + l;

			// Remove aspas do termo
			term = term.substring(1, term.length()-1);

			// Divide o term em tokens e adiciona na query (ts_query)
			// utilizando operador FOLLOWED BY (<->)
			String[] split = term.split("\\s+");

			// Caso não seja o primeiro termo
			// adiciona operador OR (|)
			if ( query.length() > 0)
				query.append(op);

			query.append("(");
			query.append(split[0]);
			for(int i = 1; i < split.length; i++){
				query.append(" <-> ");
				query.append(split[i]);
			}
			query.append(")");

			// Atualize Matcher
			m = TERM_PATTERN.matcher(terms);
		}

		// Ainda tem termos a serem processados?
		if ( terms.length() > 0){

			// Caso não seja o primeiro termo
			// adiciona operador OR (|)
			if ( query.length() > 0)
				query.append(op);

			String[] split = terms.split("\\s+");

			// O sinal negativo indica exclusão na busca, 
			// operador negação ! em SQL

			if ( split[0].charAt(0) == '-')
				query.append("!");
			query.append(split[0]);

			for(int i = 1; i < split.length; i++){
				query.append(op);
				if ( split[i].charAt(0) == '-')
					query.append("!");
				query.append(split[i]);
			}
		}

		return query.toString();
	}

	public static void main(String[] args) {
		LocalDocumentSearcher searcher = new LocalDocumentSearcher(null);
		try {
			QueryData queryData  = new QueryData();
			queryData.setTerms("rat \"cat mouse\" dog \"duck horse\"");
			queryData.setOperator("or");
			searcher.search(queryData);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
