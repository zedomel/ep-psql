package utils;

import javax.inject.Inject;

import play.db.Database;
import services.database.DatabaseService;

public class Options {
	
	public static final String LAST_UPDATE_MDP = "last_update_mdp";
	
	public static final String LAST_UPDATE_PAGERANK = "last_update_pagerank";
	
	private DatabaseService dbService;

	@Inject
	public Options(Database db) {
		dbService = new DatabaseService(db);
	}
	
	public String getOption(String op){
		try {
			return dbService.getOption(op);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public void setOption(String op, String value){
		try {
			dbService.setOption(op, value);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
