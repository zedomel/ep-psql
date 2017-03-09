package model;

/**
 * Classe representando um documento armanzeado
 * no banco de dados
 * @author jose
 *
 */
public class Document {

	private long _docId;

	private String _title;

	private String _keywords;

	private String _authors;

	private String _abstract;

	private String _publicationDate;
	
	private String _language;

	/**
	 * Cria um novo documento
	 */
	public Document() {

	}

	public long getDocId() {
		return _docId;
	}

	public void setDocId(long _docId) {
		this._docId = _docId;
	}

	public String getTitle() {
		return _title;
	}
	public void setTitle(String _title) {
		this._title = _title;
	}
	public String getKeywords() {
		return _keywords;
	}
	public void setKeywords(String _keywords) {
		this._keywords = _keywords;
	}
	public String getAuthors() {
		return _authors;
	}
	public void setAuthors(String _authors) {
		this._authors = _authors;
	}
	public String getAbstract() {
		return _abstract;
	}
	public void setAbstract(String _abstract) {
		this._abstract = _abstract;
	}
	public String getPublicationDate() {
		return _publicationDate;
	}
	public void setPublicationDate(String _publicationDate) {
		this._publicationDate = _publicationDate;
	}
	public String getLanguage(){
		return _language;
	}
	public void setLanguage(String _language) {
		this._language = _language;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if ( getAuthors() != null)
			sb.append(getAuthors() + " ");
		if ( getTitle() != null )
			sb.append(getTitle() + " ");
		if (getPublicationDate() != null)
			sb.append(getPublicationDate() + " ");
		return sb.toString().trim(); 
	}
}
