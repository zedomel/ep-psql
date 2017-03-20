package services.search;

public interface DocumentSearcher {
	
	public String search(String terms) throws Exception;
	
	public String search(String terms, int count) throws Exception;
	
	public String search(String terms, boolean fetchNumberOfCitations) throws Exception;
	
	public String search(String terms, boolean fetchNumberOfCitations, int count) throws Exception;
}
