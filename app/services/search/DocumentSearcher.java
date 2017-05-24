package services.search;

import views.formdata.QueryData;

public interface DocumentSearcher {

	public String search(QueryData queryData) throws Exception;
	
	public String search(QueryData queryData, int count) throws Exception;
	
	public String search(QueryData queryData, boolean fetchNumberOfCitations) throws Exception;
	
	public String search(QueryData queryData, boolean fetchNumberOfCitations, int count) throws Exception;
}
