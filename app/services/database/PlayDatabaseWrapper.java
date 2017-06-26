package services.database;

import java.sql.Connection;
import java.sql.SQLException;

import ep.db.database.Database;

public class PlayDatabaseWrapper implements Database{

	
	private play.db.Database db;

	public PlayDatabaseWrapper(play.db.Database db) {
		this.db = db;
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		return db.getConnection();
	}
	
	
	

}
