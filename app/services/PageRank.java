package services;

import org.jblas.DoubleMatrix;

public class PageRank {

	private static final double EPS = 1e-6;

	public PageRank() {

	}

	public DoubleMatrix pageRank(DoubleMatrix graph, double c){

		final int n = graph.columns;
		final double invN = 1.0 / n;
		double e = (1.0 - c) / n;

		final DoubleMatrix x = new DoubleMatrix(n);
		
		final int[] indices = new int[n];
		for( int i = 0; i < n; i++ )
			indices[i] = i;
		
		for( int i = 0; i < n; i++ ){

			boolean isSink = true;
			graph.getRow(i,x);

			for( int j = 0; j < n; j++){
				if ( x.get(j) != 0){
					isSink = false;
					break;
				}
			}
			if (isSink)
				x.put(indices, c * invN + e);
			else{
				x.muli(c).addi(e);
			}
			
			graph.putRow(i, x);
		}

		DoubleMatrix graphT = graph.transpose();

		DoubleMatrix rank = new DoubleMatrix(n);
		rank.put(indices, invN);

		while(true){
			DoubleMatrix lastRank = rank.dup();
			rank = graphT.mmul(rank);
			DoubleMatrix diff = rank.sub(lastRank);
			double norm1 = diff.norm1();
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

		DoubleMatrix graph = new DoubleMatrix(data);

		PageRank pageRank = new PageRank();

		DoubleMatrix rank = pageRank.pageRank(graph, 0.85);

		for(int i = 0; i < 11; i++)
			System.out.println(String.format("%.6f", rank.get(i)));

	}
}
