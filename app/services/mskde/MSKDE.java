package services.mskde;

import java.awt.geom.GeneralPath;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import cern.colt.list.tdouble.DoubleArrayList;
import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D;
import cern.jet.math.tdouble.DoubleFunctions;
import play.Logger;
import services.clustering.KMeans;
import services.kde.KernelDensityEstimator;
import services.kde.KernelFunction;
import services.marchingsquares.MarchingSquares;

public class MSKDE {
	
	private final double cellSize;

	private final KernelFunction kernel;

	private int numClusters;

	private KernelDensityEstimator kde;
	
	
	public MSKDE(KernelFunction kernel, double cellSize, int numClusters) {
		super();
		this.kernel = kernel;
		this.cellSize = cellSize;
		this.numClusters = numClusters;
	}


	public GeneralPath[] compute(DoubleMatrix2D xy){
		
		final double[][] data = xy.toArray();
		
		// Computa KDE
		kde = new KernelDensityEstimator(kernel, cellSize);
		kde.compute(data);
			
		double[][] densities = kde.getTable();
		double[] values = Arrays.stream(densities).flatMapToDouble(Arrays::stream).toArray();
		KMeans kmeans = new KMeans();
		DoubleMatrix1D valuesMatrix = new DenseDoubleMatrix1D(values);
		kmeans.cluster(valuesMatrix, numClusters);
		DoubleMatrix2D clusters = kmeans.getPartition();
		
		double[] levels = new double[clusters.columns()];
		for( int k = 0; k < clusters.columns(); k++ ){
			IntArrayList indices = new IntArrayList();
			clusters.viewColumn(k).getNonZeros(indices, new DoubleArrayList());
			double c = valuesMatrix.viewSelection(indices.elements()).aggregate(DoubleFunctions.max, DoubleFunctions.identity);
			levels[k] = c;
		}
		
		Arrays.sort(levels);
		
		// Marching Squares
		MarchingSquares ms = new MarchingSquares();
		GeneralPath[] contours;
		try {
			contours = ms.buildContours(kde.getTable(), levels);
		} catch (InterruptedException | ExecutionException e) {
			Logger.error("Error executing Marching Squares algorithm.", e);
			return null;
		}
		
		return contours;
	}
	
	
	public int cellsX(){
		return kde.getTable().length;
	}
	
	public int cellsY(){
		return kde.getTable()[0].length;
	}

}
