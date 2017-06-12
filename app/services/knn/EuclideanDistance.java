package services.knn;

public class EuclideanDistance implements DistanceFunction {

	@Override
	public double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
	}

	@Override
	public double minDist(double x, double y, double x1, double y1, double x2 , double y2) {
		double dx1 = x - x1, dx2 = x - x2, dy1 = y - y1, dy2 = y - y2;

		if (dx1*dx2 < 0) { // x is between x1 and x2
			if (dy1*dy2 < 0) { // (x,y) is inside the rectangle
				return 0; // return 0 as point is in rect
			}
			return Math.min(Math.abs(dy1),Math.abs(dy2));
		}
		if (dy1*dy2 < 0) { // y is between y1 and y2
			// we don't have to test for being inside the rectangle, it's already tested.
			return Math.min(Math.abs(dx1),Math.abs(dx2));
		}
		return Math.min( Math.min(distance(x,y,x1,y1),distance(x,y,x2,y2)), 
				Math.min(distance(x,y,x1,y2),distance(x,y,x2,y1)) );
	}

}
