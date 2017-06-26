package services.kde;

public class QuarticKernelFunction implements KernelFunction {

	private final double h;

	public QuarticKernelFunction(double bandwidth) {
		this.h = bandwidth;
	}

	@Override
	public double apply(double x, double y, double xi, double yi) {
		double dx = xi - x;
		double dy = yi - y;
		double u[] = {dx/h,dy/h};
		double normu = u[0]*u[0] + u[1]*u[1];
		if ( Math.sqrt(normu) > 1)
			return 0;
		
		return 15.0/16.0 * ( 1.0 - normu) * (1.0 - normu);
	}
}
