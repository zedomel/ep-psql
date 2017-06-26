package services.mskde;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.io.BufferedWriter;
import java.io.FileWriter;

import javax.inject.Inject;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.google.common.collect.ImmutableMap;

import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import ep.db.database.DatabaseService;
import play.Logger;
import play.db.Database;
import play.db.Databases;
import services.database.PlayDatabaseWrapper;
import services.kde.QuarticKernelFunction;

public class MSKDEService {

	private DatabaseService dbService;

	private double cellSize = 0.5;

	private double bandwidth = 3;

	private int numClusters = 4;

	@Inject
	public MSKDEService(Database db) {
		dbService = new DatabaseService(new PlayDatabaseWrapper(db));
	}


	public void calculate(){
		DoubleMatrix2D xy = new DenseDoubleMatrix2D(299,2);
		try {
//			xy = dbService.getProjections(null, null);
		} catch (Exception e) {
			Logger.error("Error retriving projection from DB", e);
			return;
		}
		
//		File file = new File("geyser.csv");
//		try (BufferedReader br = new BufferedReader(new FileReader(file))){
//			String line = null;
//			line = br.readLine();
//			
//			int i = 0;
//			while( line != null){
//				String[] f = line.split(",");
//				xy.setQuick(i, 0, Double.parseDouble(f[0]));
//				xy.setQuick(i, 1, Double.parseDouble(f[1]));
//				line = br.readLine();
//				++i;
//			}
//		}catch (Exception e) {
//			
//		}
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter("docs_xy.csv"))){
			for(int i = 0; i< xy.rows(); i++){
				bw.write(String.format("%.4f,%.4f", xy.get(i, 0), xy.get(i, 1)));
				bw.newLine();
			}
		}catch (Exception e) {
			
		}

		MSKDE mskde = new MSKDE(new QuarticKernelFunction(bandwidth), cellSize, numClusters);
		GeneralPath[] contours = mskde.compute(xy);

		final Color[] colors = generatePallete(contours.length);

		JFrame frame = new JFrame("MSKDE");
		JPanel panel = new JPanel();

		frame.getContentPane().add(panel);
		frame.pack();
		frame.setSize(800, 600);
		frame.setVisible(true);

		Graphics2D g2 = (Graphics2D) panel.getGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		AffineTransform xf = new AffineTransform();
		xf.translate(-1, -1);
		xf.scale(3, 3);
		for (int i = 0; i < contours.length; i++) {
			Shape iso = xf.createTransformedShape(contours[i]); // Remapped every pan & zoom.
			g2.setColor(colors[i]);
			g2.fill(iso);
			g2.setColor(Color.gray);
			g2.draw(iso);
		}
	}

	private Color[] generatePallete(int n) {
		Color[] cols = new Color[n];
		for (int i = 0; i < n; i++)
			cols[i] = Color.getHSBColor((float) i / n, 1, 1);
		return cols;
	}


	public static void main(String[] args) {

		play.db.Database db = Databases.createFrom("org.postgresql.Driver", "jdbc:postgresql://localhost/petrica_db", 
				ImmutableMap.of(
						"username", "postgres",
						"password", "kurt1234"
						));
		MSKDEService mskdeService = new MSKDEService(db);
		mskdeService.calculate();
	}
}
