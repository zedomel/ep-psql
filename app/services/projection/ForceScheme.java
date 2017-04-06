package services.projection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;

public class ForceScheme {

	private final int maxIter;

	private final float tol;

	private final float fraction;

	private final float eps;

	private final Random rng = new Random();

	public ForceScheme() {
		this(50, 0f, 8.0f, 1e-5f);
	}

	public ForceScheme(int maxIter, float tol, float fraction, float eps) {
		this.maxIter = maxIter;
		this.tol = tol;
		this.fraction = fraction;
		this.eps = eps;
	}

	public DoubleMatrix2D project(DoubleMatrix2D xs) {
		// Gera matrix y aleatoriamente
		int n = xs.rows();
		DoubleMatrix2D y = new DenseDoubleMatrix2D(n, 2);
		for(int i = 0; i < n; i++)
			for(int j = 0; j < 2; j++)
				y.setQuick(i, j, rng.nextDouble());

		// Listas para iteração aleatoria nas linhas e colunas
		List<Integer> i = new ArrayList<>(n), j = new ArrayList<>(n);
		for (int k = 0; k < n; k++){
			i.add(k);
			j.add(k);
		}

		// Gradiente Descendente
		final Algebra alg = new Algebra();
		double prevDeltaSum = Double.POSITIVE_INFINITY;
		for (int iter = 0; iter < maxIter; iter++) {
			double deltaSum = 0;

			Collections.shuffle(i);
			for (int a : i) {
				Collections.shuffle(j);
				for (int b : j) {
					if (a == b)
						continue;

					DoubleMatrix1D direction = y.viewRow(b).copy().assign(y.viewRow(a), Functions.minus);
					double d2 = Math.max(alg.norm2(direction), eps);
					double delta = (xs.getQuick(a, b) - d2) / fraction;
					deltaSum += Math.abs(delta);
					direction.assign(Functions.mult(delta/d2));
					y.viewRow(b).assign(direction, Functions.plus);
				}
			}

			if (Math.abs(prevDeltaSum - deltaSum) < tol)
				break;
			prevDeltaSum = deltaSum;
		}

		return y;
	}

}
