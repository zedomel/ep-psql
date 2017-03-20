package services.search;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import model.Document;
import play.db.Database;
import services.database.DatabaseSearcherService;

public class LocalDocumentSearcher implements DocumentSearcher {

	private static final Pattern TERM_PATTERN = Pattern.compile(".*(\".*?\").*");

	private DatabaseSearcherService dbService;

	@Inject
	public LocalDocumentSearcher(Database db) {
		this.dbService = new DatabaseSearcherService(db);
	}

	@Override
	public String search(String terms) throws Exception {
		return search(terms, false, 0);
	}

	@Override
	public String search(String terms, int count) throws Exception {
		return search(terms, false, count);
	}

	@Override
	public String search(String terms, boolean fetchNumberOfCitations) throws Exception {
		return search(terms, fetchNumberOfCitations, 0);
	}

	@Override
	public String search(String terms, boolean fetchNumberOfCitations, int count) throws Exception {
		StringBuilder query = new StringBuilder();
		String aux = terms;

		// Checa consulta usando padrão para expressões entre aspas:
		// busca por frases
		Matcher m = TERM_PATTERN.matcher(aux);
		while( m.matches() ){
			// Extrai grupo entre aspas;
			String term = m.group(1);
			System.out.println(term);
			// Atualiza String de busca removendo grupo
			// extraido
			String f = aux.substring(0, m.start(1));
			String l = aux.substring(m.end(1)); 
			aux =  f + l;

			// Remove aspas do termo
			term = term.substring(1, term.length()-1);

			// Divide o term em tokens e adiciona na query (ts_query)
			// utilizando operador FOLLOWED BY (<->)
			String[] split = term.split("\\s+");

			// Caso não seja o primeiro termo
			// adiciona operador OR (|)
			if ( query.length() > 0)
				query.append(" | ");

			query.append("(");
			query.append(split[0]);
			for(int i = 1; i < split.length; i++){
				query.append(" <-> ");
				query.append(split[i]);
			}
			query.append(")");

			// Atualize Matcher
			m = TERM_PATTERN.matcher(aux);
		}

		// Ainda tem termos a serem processados?
		if ( aux.length() > 0){

			// Caso não seja o primeiro termo
			// adiciona operador OR (|)
			if ( query.length() > 0)
				query.append(" | ");

			String[] split = aux.split("\\s+");

			// O sinal negativo indica exclusão na busca, 
			// operador negação ! em SQL

			if ( split[0].charAt(0) == '-')
				query.append("!");
			query.append(split[0]);

			for(int i = 1; i < split.length; i++){
				query.append(" | ");
				if ( split[i].charAt(0) == '-')
					query.append("!");
				query.append(split[i]);
			}
		}

		final String querySearch = query.toString();
		List<Document> docs = dbService.getDocuments(querySearch, count);

		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(docs);
	}

	public static void main(String[] args) {
		LocalDocumentSearcher searcher = new LocalDocumentSearcher(null);
		try {
			searcher.search("rat \"cat mouse\" dog \"duck horse\"");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
