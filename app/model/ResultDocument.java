package model;

import java.io.Serializable;

/**
 * Class to hold document returned from 
 * a search in the lucene index.
 * It is convenient to encode/decode 
 * into/from Json.
 * @author jose
 *
 */
public class ResultDocument implements Serializable{
	
	/**
	 * Default serial id
	 */
	private static final long serialVersionUID = 2325490618164049148L;

	/**
	 * Document score
	 */
	private float score;
	
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
	
	/**
	 * Document filename
	 */
	private String filename;
	
	/**
	 * String list of document's references
	 */
	private String references;

	/**
	 * Total number of citations
	 */
	private long numberOfCitations;
	
	/**
	 * Creates a empty {@link ResultDocument}
	 */
	public ResultDocument() {
	
	}
	
	/**
	 * Get document's score
	 * @return document's score as a float
	 */
	public float getScore() {
		return score;
	}

	/**
	 * Set document's score
	 * @param score as float
	 */
	public void setScore(float score) {
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
	 * Get document's filename
	 * @return filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Set document's filename.
	 * @param filename
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Get document's references
	 * @return references
	 */
	public String getReferences() {
		return references;
	}

	/**
	 * Set document's references
	 * @param references
	 */
	public void setReferences(String references) {
		this.references = references;
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
}
