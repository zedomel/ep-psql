package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jose
 *
 */
public class SimpleDocument implements Serializable{
	
	/**
	 * Default serial id
	 */
	private static final long serialVersionUID = 2325490618164049148L;
	
	private long docId;
	
	private String DOI;

	/**
	 * Document score
	 */
	private double score;
	
	private double relevance;
	
	/**
	 * Document title
	 */
	private String title;
	
	/**
	 * Document list of authors
	 */
	private String authors;
	
	/**
	 * Document keywords
	 */
	private String keywords;
	
	private String publicationDate;
	
	private double x;
	
	private double y;
	
	private int cluster;

	/**
	 * Total number of citations
	 */
	private long numberOfCitations;
	
	private List<Long> references;
	
	/**
	 * Creates a empty {@link ResultDocument}
	 */
	public SimpleDocument() {
		references = new ArrayList<>();
	}
	
	public List<Long> getReferences() {
		return references;
	}
	
	public void setReferences(List<Long> references) {
		this.references = references;
	}
	
	public void addReference(long refId){
		references.add(refId);
	}
	
	public int getCluster() {
		return cluster;
	}
	
	public void setCluster(int cluster) {
		this.cluster = cluster;
	}
	
	/**
	 * Get document's score
	 * @return document's score as a float
	 */
	public double getScore() {
		return score;
	}

	/**
	 * Set document's score
	 * @param score as float
	 */
	public void setScore(double score) {
		this.score = score;
	}

	/**
	 * Get document's title
	 * @return title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set document's title
	 * @param title 
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Get document's authors as 
	 * extracted from document file.
	 * @return authors.
	 */
	public String getAuthors() {
		return authors;
	}

	/**
	 * Set document's authors.
	 * @param authors string.
	 */
	public void setAuthors(String authors) {
		this.authors = authors;
	}

	/**
	 * Get document's keywords.
	 * @return keywords
	 */
	public String getKeywords() {
		return keywords;
	}

	/**
	 * Set document's keywords.
	 * @param keywords
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

	public long getDocId() {
		return docId;
	}

	public void setDocId(long docId) {
		this.docId = docId;
	}

	public String getDOI() {
		return DOI;
	}

	public void setDOI(String dOI) {
		DOI = dOI;
	}

	public double getRelevance() {
		return relevance;
	}

	public void setRelevance(double relevance) {
		this.relevance = relevance;
	}

	public String getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(String publicationDate) {
		this.publicationDate = publicationDate;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}
}
