package services.projection;

import java.io.IOException;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.jblas.Singular;
import org.jblas.ranges.RangeUtils;

public class Lamp {

	private static final double TOL = 1e-6;

	public Lamp() {

	}

	public DoubleMatrix project(DoubleMatrix x, DoubleMatrix xs, DoubleMatrix ys){

		int ninst = x.rows,
				dim = x.columns;
		int k = xs.rows,
				a = xs.columns;
		int p = ys.columns;

		assert dim == a;

		DoubleMatrix Y = DoubleMatrix.zeros(ninst, p);
		for (int pt = 0; pt < ninst; pt++){
			// Computes alphas
			DoubleMatrix alpha = DoubleMatrix.zeros(k);
			for( int i = 0; i < k; i++){
				// Verify if the point to be projected is a control point
				// avoids division by zero
				double norm2 = xs.getRow(i).sub(x.getRow(pt)).norm2();
				if ( norm2 < TOL )
					alpha.put(i, Float.MAX_VALUE);
				else
					alpha.put(i, 1.0 / (norm2*norm2));
			}

			// Computes x~ and y~ (eq. 3)
			DoubleMatrix xtilde = DoubleMatrix.zeros(dim);
			DoubleMatrix ytilde = DoubleMatrix.zeros(p);

			xtilde.addi(alpha.transpose().mmul(xs));
			ytilde.addi(alpha.transpose().mmul(ys));

			xtilde.divi(alpha.sum());
			ytilde.divi(alpha.sum());

			DoubleMatrix A = DoubleMatrix.zeros(k, dim),
					B = DoubleMatrix.zeros(k, p);

			DoubleMatrix xhat = DoubleMatrix.zeros(k, dim),
					yhat = DoubleMatrix.zeros(k, p);

			// Computation of x^ and y^ (eq. 6)
			xhat = xs.subRowVector(xtilde);
			yhat = ys.subRowVector(ytilde);
			A = xhat.mulColumnVector(MatrixFunctions.sqrt(alpha));
			B = yhat.mulColumnVector(MatrixFunctions.sqrt(alpha));

			DoubleMatrix[] udv = Singular.fullSVD( A.transpose().mmul(B) );
			DoubleMatrix U = udv[0], V = udv[2];

			// VV is the matrix V filled with zeros
			DoubleMatrix VV = DoubleMatrix.zeros(dim, p); // Size of U = dim, by SVD
			//Size of V = p, by SVD
			final int[] rangep = new int[p];
			for( int i = 0; i < p; i++)
				rangep[i] = i;

			for( int i = 0; i < p; i++)
				VV.putRow(i, V.getRow(i));

			DoubleMatrix M = U.mmul(VV);

			Y.putRow(pt, x.getRow(pt).sub(xtilde).mmul(M).add(ytilde)); // eq. 8
		}

		return Y;
	}

	public static void main(String[] args) throws IOException {

		DoubleMatrix data = DoubleMatrix.loadCSVFile("/Users/jose/Documents/freelancer/petricaep/lamp-python/iris.data");
		DoubleMatrix x = data.getColumns(RangeUtils.interval(0, data.columns-1));
		
		int[] indices = new int[]{47,   3,  31,  25,  15, 118,  89,   6, 103,  65,  88,  38,  92};
		DoubleMatrix xs = x.getRows(indices);

		DoubleMatrix ys = new DoubleMatrix(new double[][]{{ 0.64594878, 0.21303289},
			{ 0.71731767,  0.396145  },
			{ 0.70414944, 0.65089645},
			{ 0.57139458,  0.4722532 },
			{ 0.76340806,  0.25250587},
			{ 0.61347666,  0.8632922 },
			{ 0.56565112,  0.54291614},
			{ 0.80551708, -0.02531856},
			{-0.08270801,  0.57582274},
			{ 0.56379192,  0.22470327},
			{ 0.82288279,  0.21620781},
			{ 0.89253817,  0.46421933},
			{-0.02987608,  0.6828974 }
		});
		
		Lamp lamp = new Lamp();
		DoubleMatrix y = lamp.project(x, xs, ys);
		
		for( int i = 0; i < y.rows; i++){
			for(int j = 0; j < y.columns; j++)
				System.out.print(String.format("%e ", y.get(i,j)));
			System.out.println();
		}
	}

}
