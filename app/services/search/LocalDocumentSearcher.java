package services.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import cern.colt.matrix.DoubleMatrix2D;
import model.SimpleDocument;
import play.db.Database;
import services.clustering.KMeans;
import services.database.DatabaseService;
import views.formdata.QueryData;

public class LocalDocumentSearcher implements DocumentSearcher {

	private static final Pattern TERM_PATTERN = Pattern.compile(".*(\".*?\").*");

	private DatabaseService dbService;

	private int clusters = 10;

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
		
		List<SimpleDocument> docs;
		if ( ! queryData.getAuthor().isEmpty() || ! queryData.getYearStart().isEmpty() || ! queryData.getYearEnd().isEmpty() )
			docs = dbService.getAdvancedSimpleDocuments(query, queryData.getAuthor(), queryData.getYearStart(), 
					queryData.getYearEnd(), count);
		else
			docs = dbService.getSimpleDocuments(query, count);

		long docIds[] = new long[docs.size()];
		int i = 0;
		for( SimpleDocument doc :  docs){
			docIds[i] = doc.getDocId();
			++i;
		}

		// Referencias
		Map<Long, List<Long>> references = dbService.getReferences(docIds);
		for(SimpleDocument doc : docs){
			doc.setReferences(references.get(doc.getDocId()));
		}

		DoubleMatrix2D matrix = dbService.buildFrequencyMatrix(docIds);

		//Clustering
		KMeans kmeans = new KMeans();
		kmeans.cluster(matrix, clusters );
		DoubleMatrix2D partition = kmeans.getPartition();

		for(i = 0; i < partition.rows(); i++){
			for(int j = 0; j < partition.columns(); j++)
				if ( partition.getQuick(i, j) != 0){
					docs.get(i).setCluster(j);
				}
		}

		Map<String, Object> result = new HashMap<>();
		result.put("documents", docs);
		result.put("nclusters", partition.columns());

		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(result);
	}

	private String buildQuery(QueryData queryData) {
		String terms = queryData.getTerms();
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
