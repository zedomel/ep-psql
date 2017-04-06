package services.projection;

import cern.colt.matrix.DoubleMatrix2D;
import play.db.Database;
import services.database.DatabaseService;

public class MultidimensionalProjection {

	private DatabaseService dbService;

	public MultidimensionalProjection( Database db ) {
		this.dbService = new DatabaseService(db);
	}

	public void project() {

		// Constroi matriz de frequencia de termos
		DoubleMatrix2D matrix = null;
		try {
			 matrix = dbService.buildFrequencyMatrix(null);
		} catch (Exception e) {
			// TODO Log-me
			e.printStackTrace();
		}

		// Realiza projeção multidimensional
		Lamp lamp = new Lamp();
		DoubleMatrix2D y = lamp.project(matrix);
		
		updateProjections(y);
	}
	
	private void updateProjections(DoubleMatrix2D y) {
		try {
			dbService.updateXYProjections(y);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
