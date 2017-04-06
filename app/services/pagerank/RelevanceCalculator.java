package services.pagerank;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import play.db.Database;
import services.PageRank;
import services.database.DatabaseService;

public class RelevanceCalculator {
	
	private static final double C = 0.85;
	
	private final DatabaseService dbService;
	
	private final double c;
	
	public RelevanceCalculator( Database db ) {
		this(db, C);
	}
	
	public RelevanceCalculator( Database db, double c ) {
		this.dbService = new DatabaseService(db);
		this.c = c;
	}
	
	public void updateRelevance() throws Exception {
		
		DoubleMatrix2D graph = null;
		try {
			graph = dbService.getCitationGraph();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
		
		PageRank pageRank = new PageRank();
		DoubleMatrix1D rank = pageRank.pageRank(graph, c);
		
		try {
			dbService.updatePageRank(rank);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
	}

}
