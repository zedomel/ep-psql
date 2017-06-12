package services.knn;

public interface DistanceFunction {
	
	
	public double distance(double x1, double y1, double x2, double y2);

	public double minDist(double x, double y, double x2, double y2, double w, double h);

}
