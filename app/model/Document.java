package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo simplicado de um documento
 * utilizado para enviar dados para o cliente (visualização)
 * serializado como JSON.
 * @version 1.0
 * @since 2017
 */
public class Document implements Serializable{
	
	/**
	 * Default serial id
	 */
	private static final long serialVersionUID = 2325490618164049148L;
	
	/**
	 * Id do documento.
	 */
	private long docId;
	
	/**
	 * DOI
	 */
	private String DOI;

	/**
	 * Score do documento (PostgreSQL rank)
	 */
	private double score;
	
	/**
	 * Relevância.
	 */
	private double relevance;
	
	/**
	 * Título
	 */
	private String title;
	
	/**
	 * Autores
	 */
	private String authors;
	
	/**
	 * Palavras-chaves
	 */
	private String keywords;
	
	/**
	 * Date de publicação (ano)
	 */
	private String publicationDate;
	
	/**
	 * Coordenada x da projeção
	 */
	private double x;
	
	/**
	 * Coordenada y da projeção
	 */
	private double y;
	
	/**
	 * Número do cluster
	 */
	private int cluster;

	/**
	 * Número total de citações.
	 */
	private long numberOfCitations;
	
	/**
	 * Lista com id's dos documentos citados
	 */
	private List<Long> references;

	private List<Long> neighbors;
	
	/**
	 * Cria um novo documento em branco.
	 */
	public Document() {
		references = new ArrayList<>();
	}
	
	/**
	 * Retorna lista de referências.
	 * @return lista com id's dos documentos citados.
	 */
	public List<Long> getReferences() {
		return references;
	}
	
	/**
	 * Atribui lista de referências.
	 * @param references lista com id's das citações.
	 */
	public void setReferences(List<Long> references) {
		this.references = references;
	}
	
	/**
	 * Adiciona um referência a lista de documentos
	 * citados.
	 * @param refId id do documento a ser adicionado 
	 * a lista de referências.
	 */
	public void addReference(long refId){
		references.add(refId);
	}
	
	/**
	 * Retorna número de cluster desde documento.
	 * @return número do cluster.
	 */
	public int getCluster() {
		return cluster;
	}
	
	/**
	 * Atribui número do cluster.
	 * @param cluster número do cluster.
	 */
	public void setCluster(int cluster) {
		this.cluster = cluster;
	}
	
	/**
	 * Score do documento (PostgreSQL rank).
	 * @return score do documento segundo resulta da busca.
	 */
	public double getScore() {
		return score;
	}

	/**
	 * Atribui score do documento.
	 * @param score do documento.
	 */
	public void setScore(double score) {
		this.score = score;
	}

	/**
	 * Retorna título do documento.
	 * @return título.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Atribui título do documento.
	 * @param title título.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Retorna autores do documento.
	 * @return  autores.
	 */
	public String getAuthors() {
		return authors;
	}

	/** 
	 * Atribui autores do documento.
	 * @param authors autores.
	 */
	public void setAuthors(String authors) {
		this.authors = authors;
	}

	/**
	 * Retorna palavras-chaves.
	 * @return palavras-chaves.
	 */
	public String getKeywords() {
		return keywords;
	}

	/**
	 * Atribui palavras-chaves.
	 * @param keywords palavras-chaves.
	 */
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	/**
	 * Get document's total number of citations.
	 * @return number of citations.
	 */
	public long getNumberOfCitations() {
		return numberOfCitations;
	}
	
	/**
	 * Set document's number of citation.
	 * @param numberOfCitations
	 */
	public void setNumberOfCitations(long numberOfCitations) {
		this.numberOfCitations = numberOfCitations;
	}

	/**
	 * Retorna id do documento.
	 * @return id do documento.
	 */
	public long getDocId() {
		return docId;
	}

	/**
	 * Atribui id do documento.
	 * @param docId id do documento.
	 */
	public void setDocId(long docId) {
		this.docId = docId;
	}

	/**
	 * Retorna DOI do documento.
	 * @return DOI.
	 */
	public String getDOI() {
		return DOI;
	}

	/**
	 * Atribui DOI.
	 * @param doi DOI
	 */
	public void setDOI(String dOI) {
		DOI = dOI;
	}

	/**
	 * Retorna relavância do documento
	 * calculada a partir do algoritmo {@link PageRank}.
	 * @return relevância do documento em relação a toda a base 
	 * (no. de citações).
	 */
	public double getRelevance() {
		return relevance;
	}

	/**
	 * Atribui relevância do documento.
	 * @param relevance relevância.
	 */
	public void setRelevance(double relevance) {
		this.relevance = relevance;
	}

	/**
	 * Retorna data de publicação do documento.
	 * @return data de publicação (geralmente somente
	 * ano).
	 */
	public String getPublicationDate() {
		return publicationDate;
	}

	/**
	 * Atribui data de publicação.
	 * @param publicationDate data de publicação.
	 */
	public void setPublicationDate(String publicationDate) {
		this.publicationDate = publicationDate;
	}

	/**
	 * Retorna coordenada x da projeção.
	 * @return coordenada x.
	 */
	public double getX() {
		return x;
	}

	/**
	 * Atribui coordenada x da projeção.
	 * @param x coordenda x.
	 */
	public void setX(double x) {
		this.x = x;
	}

	/**
	 * Retorna coordenada y da projeção.
	 * @return coordenada y.
	 */
	public double getY() {
		return y;
	}

	/**
	 * Atribui coordenada y da projeção.
	 * @param y coordenada y
	 */
	public void setY(double y) {
		this.y = y;
	}

	public void addNeighbor(long docId) {
		if ( neighbors == null)
			neighbors = new ArrayList<Long>();
		neighbors.add(docId);
	}
	
	public List<Long> getNeighbors() {
		return neighbors;
	}
}
