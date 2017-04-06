package services.projection;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.jblas.DoubleMatrix;
import org.jblas.ranges.RangeUtils;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.SingularValueDecomposition;
import cern.jet.math.Functions;

public class Lamp {

	private static final double TOL = 1e-6;
	
	private final Random rng;

	public Lamp() {
		rng = new Random();
	}
	
	public DoubleMatrix2D project(DoubleMatrix2D x){
		DoubleMatrix2D xs, ys;
		
		// Seleciona control points aleatoriamente
		int n = (int) Math.sqrt( x.rows() );
		Set<Integer> sample = new HashSet<>(n);
		while( sample.size() < n ){
			Integer next = rng.nextInt( x.rows() );
			sample.add(next);
		}
		
		int[] cpoints = new int[n];
		Iterator<Integer> iter = sample.iterator();
		for(int j = 0; j < n && iter.hasNext(); j++){
			cpoints[j] = iter.next();
		}
		
		// Projeta control points usando MDS
		ForceScheme forceScheme = new ForceScheme();
		xs = x.viewSelection(cpoints, null).copy();
		ys = forceScheme.project(xs);
		
		// Projeta restante dos pontos
		return project(x, cpoints, ys);
	}

	public DoubleMatrix2D project(DoubleMatrix2D x, int[] cpoints, DoubleMatrix2D ys){

		DoubleMatrix2D xs = x.viewSelection(cpoints, null).copy();
		
		int ninst = x.rows(),
				dim = x.columns();
		int k = cpoints.length,
				a = xs.columns();
		int p = ys.columns();

		assert dim == a;

		final Algebra alg = new Algebra();
		DoubleMatrix2D Y = DoubleFactory2D.sparse.make(ninst, p, 0.0);
		
		for (int pt = 0; pt < ninst; pt++){
			// Computes alphas
			DoubleMatrix1D alpha = DoubleFactory1D.sparse.make(k, 0.0);
			boolean skip = false;
			for( int i = 0; i < k; i++){
				// Verify if the point to be projected is a control point
				// avoids division by zero
				double norm2 = alg.norm2( xs.viewRow(i).copy().assign(x.viewRow(pt), Functions.minus)); 
				if ( norm2 < TOL ){
					// point is too close to sample point; position them equally
					Y.viewRow(pt).assign(ys.viewRow(i));
					skip = true;
					break;
				}
				
				alpha.setQuick(i, 1.0 / norm2);
			}
			
			if ( skip )
				continue;

			double alphaSum = alpha.zSum();
			
			// Computes x~ and y~ (eq. 3)
			DoubleMatrix1D xtilde = DoubleFactory1D.dense.make(dim, 0.0);
			DoubleMatrix1D ytilde = DoubleFactory1D.dense.make(p, 0.0);

			xtilde = alg.mult(xs.viewDice(), alpha).assign(Functions.div(alphaSum));
			ytilde = alg.mult(ys.viewDice(), alpha).assign(Functions.div(alphaSum));

			DoubleMatrix2D xhat = xs.copy(), yhat = ys.copy();

			// Computation of x^ and y^ (eq. 6)
			for( int i = 0; i < xs.rows(); i++){
				xhat.viewRow(i).assign(xtilde, Functions.minus);
				yhat.viewRow(i).assign(ytilde, Functions.minus);
			}
			
			DoubleMatrix2D At, B;
			
			// Sqrt(alpha)
			alpha.assign(Functions.sqrt);
			for(int i = 0; i < xhat.columns(); i++ )
				xhat.viewColumn(i).assign(alpha, Functions.mult);
			for(int i = 0; i < yhat.columns(); i++ )
				yhat.viewColumn(i).assign(alpha, Functions.mult);
			
			At = xhat.viewDice();
			B = yhat;

			SingularValueDecomposition svd = new SingularValueDecomposition( At.zMult(B, null) );
			DoubleMatrix2D U = svd.getU(), V = svd.getV();

			// eq. 7: M = UV
			DoubleMatrix2D M = U.zMult(V.viewDice(), null); 
			
			//eq. 8: y = (x - xtil) * M + ytil
			DoubleMatrix1D rowX = x.viewRow(pt).copy();
			rowX = M.viewDice().zMult(rowX.assign(xtilde, Functions.minus),null).assign(ytilde, Functions.plus);
			Y.viewRow(pt).assign(rowX);
		}

		return Y;
	}

	public static void main(String[] args) throws IOException {

		DoubleMatrix data = DoubleMatrix.loadCSVFile("/Users/jose/Documents/freelancer/petricaep/lamp-python/iris.data");
		
		DoubleMatrix2D x = new DenseDoubleMatrix2D(data.getColumns(RangeUtils.interval(0, data.columns-1)).toArray2());
		
		int[] indices = new int[]{47,   3,  31,  25,  15, 118,  89,   6, 103,  65,  88,  38,  92};

		DoubleMatrix2D ys = new DenseDoubleMatrix2D(new double[][]{
			{ 0.64594878, 0.21303289},
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
		DoubleMatrix2D y = lamp.project(x, indices, ys);
		
		for( int i = 0; i < y.rows(); i++){
			for(int j = 0; j < y.columns(); j++)
				System.out.print(String.format("%e ", y.get(i,j)));
			System.out.println();
		}
	}

}
