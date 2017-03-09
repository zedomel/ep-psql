package services;

import java.util.List;

/**
 * Interface de processadores de documentos (parsers)
 * @author jose
 *
 */
public interface DocumentParser {
	
	/**
	 * Processa cabeçalho do documento especificado
	 * @param filename nome do arquivo (caminho) para o documento.
	 */
	public void parseHeader(String filename) throws Exception;
	
	/**
	 * Processa referências do documento.
	 * @param filename nome do arquivo (caminho) para o documento.
	 */
	public void parseReferences(String filename) throws Exception;

	public String getAuthors();
	
	public String getTitle();
	
	public String getAffiliation();
	
	public String getDOI();
	
	public String getPublicationDate();
	
	public String getAbstract();
	
	public String getJournal();
	
	public String getKeywords();
	
	/**
	 * Languages are encoded in ISO 3166
	 * @return
	 */
	public String getLanguage();
	
	public List<Bibliography> getReferences();
}
