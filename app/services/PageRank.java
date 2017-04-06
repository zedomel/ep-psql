package services;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;

public class PageRank {

	private static final double EPS = 1e-6;

	public PageRank() {

	}

	public DoubleMatrix1D pageRank(DoubleMatrix2D graph, double c){

		final int n = graph.columns();
		final double invN = 1.0 / n;
		double e = (1.0 - c) / n;

		DoubleMatrix1D x = new SparseDoubleMatrix1D(n);
		
		final int[] indices = new int[n];
		for( int i = 0; i < n; i++ )
			indices[i] = i;
		
		for( int i = 0; i < n; i++ ){

			boolean isSink = true;
			x = graph.viewRow(i);

			for( int j = 0; j < n; j++){
				if ( x.getQuick(j) != 0){
					isSink = false;
					break;
				}
			}
			if (isSink)
				x.viewSelection(indices).assign(c * invN + e);
			else{
				x.assign(Functions.mult(c)).assign(Functions.plus(e));
			}
			
//			graph.putRow(i, x);
		}

		DoubleMatrix2D graphT = graph.viewDice();

		DoubleMatrix1D rank = new DenseDoubleMatrix1D(n);
		rank.viewSelection(indices).assign(invN);

		Algebra alg = new Algebra();
		while(true){
			DoubleMatrix1D lastRank = rank.copy();
			rank = alg.mult(graphT, rank);
			DoubleMatrix1D diff = rank.copy().assign(lastRank, Functions.minus);
			double norm1 = alg.norm1(diff);
			if ( norm1 <= EPS)
				break;
		}

		return rank;
	}

	public static void main(String[] args) {

		double[][] data = {
				{    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,    0 },
				{    0,    0,    1,    0,    0,    0,    0,    0,    0,    0,    0 },
				{    0,    1,    0,    0,    0,    0,    0,    0,    0,    0,    0 },
				{ 1./2, 1./2,    0,    0,    0,    0,    0,    0,    0,    0,    0 },
				{    0, 1./3,    0, 1./3,    0, 1./3,    0,    0,    0,    0,    0 },
				{    0, 1./2,    0,    0, 1./2,    0,    0,    0,    0,    0,    0 },
				{    0, 1./2,    0,    0, 1./2,    0,    0,    0,    0,    0,    0 },
				{    0, 1./2,    0,    0, 1./2,    0,    0,    0,    0,    0,    0 },
				{    0, 1./2,    0,    0, 1./2,    0,    0,    0,    0,    0,    0 },
				{    0,    0,    0,    0,    1,    0,    0,    0,    0,    0,    0 },
				{    0,    0,    0,    0,    1,    0,    0,    0,    0,    0,    0 },
		};

		DoubleMatrix2D graph = new SparseDoubleMatrix2D(data);

		PageRank pageRank = new PageRank();

		DoubleMatrix1D rank = pageRank.pageRank(graph, 0.85);

		for(int i = 0; i < 11; i++)
			System.out.println(String.format("%.6f", rank.get(i)));

	}
}
