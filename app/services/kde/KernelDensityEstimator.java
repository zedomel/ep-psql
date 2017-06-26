/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package services.kde;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JFrame;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.GrayPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

/**
 * Kernel Density Estimator:
 * 
 *
 */
public class KernelDensityEstimator {

	private final double cellSize;

	private final KernelFunction kernel;

	private double[][] table;

	public KernelDensityEstimator(KernelFunction kernel, double cellSize) {
		this.kernel = kernel;
		this.cellSize = cellSize;
	}

	public void compute(double[][] data){

		double bounds[] = selectBounds(data, 0.25);

		//Construct table of KDE values along a grid.
		//Note that the grid may exceed bounds.
		//Samples of the PDF are taken at each grid corner.
		final int cols = (int)Math.ceil( (bounds[2] - bounds[0]) / cellSize ) + 1;
		final int rows = (int)Math.ceil( (bounds[3] - bounds[1]) / cellSize ) + 1;
		table = new double[rows][cols];

		//Iterate through each corner of cell.
		for( int y = 0; y < rows; y++ ) {
			for( int x = 0; x < cols; x++ ) {
				final double px = bounds[0] + cellSize * x + cellSize/2;
				final double py = bounds[1] + cellSize * y + cellSize/2;
				double sum = 0.0;

				//Compute contribution of each point.
				// TODO: optimize using QuadTree
				for( int p = 0; p < data.length; p++ ) {
					sum += kernel.apply( px, py, data[p][0], data[p][1]);
				}

				//Insert value into table.
				table[y][x] = sum;
			}
		}

		//Compute volume of grid.
		double tableVolume = 0.0;
		double cellArea = cellSize * cellSize;

		for( int y = 0; y < rows - 1; y++ ) {
			for( int x = 0; x < cols - 1; x++ ) {
				double v0 = table[y][x];
				double v1 = table[y+1][x];
				double v2 = table[y][x + 1];
				double v3 = table[y][x + 1];

				tableVolume += cellArea * (v0 + v1 + v2 + v3) * 0.25;
			}
		}

		//Normalize table.
		if ( tableVolume != 0){
			for( int i = 0; i < rows; i++ ) {
				for(int j = 0; j < cols; j++)
					table[i][j] /= tableVolume;
			}
		}
		
		double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
		double[][] values = new double[rows*cols][3];
		for(int i = 0; i < values.length; i++){
			int x = i % cols, y = i / cols;
			values[i][0] = bounds[0] + cellSize * x;
			values[i][1] = bounds[1] + cellSize * y;
			values[i][2] = table[y][x];
			
			if ( values[i][2] < min )
				min = values[i][2];
			if ( values[i][2] > max)
				max = values[i][2];
		}
		
		XYZDataset dataset = createDataset(values); 
		JFreeChart chart = createChart(dataset, min, max);
		ChartPanel panel = new ChartPanel(chart);
		panel.setPreferredSize(new java.awt.Dimension(500, 270));
		JFrame frame = new JFrame("KDE");
        frame.setContentPane(panel);
        frame.pack();
        frame.setVisible(true);
	}
	
	private static JFreeChart createChart(XYZDataset dataset, double min, double max) {
        NumberAxis xAxis = new NumberAxis("X");
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        xAxis.setLowerMargin(0.0);
        xAxis.setUpperMargin(0.0);
        xAxis.setAxisLinePaint(Color.white);
        xAxis.setTickMarkPaint(Color.white);

        NumberAxis yAxis = new NumberAxis("Y");
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        yAxis.setLowerMargin(0.0);
        yAxis.setUpperMargin(0.0);
        yAxis.setAxisLinePaint(Color.white);
        yAxis.setTickMarkPaint(Color.white);
        
        XYBlockRenderer renderer = new XYBlockRenderer();
        PaintScale scale = new GrayPaintScale(Math.floor(min), Math.ceil(max));
        renderer.setPaintScale(scale);
        
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setOutlinePaint(Color.blue);
        
        JFreeChart chart = new JFreeChart("XYBlockChartDemo1", plot);
        chart.removeLegend();
        NumberAxis scaleAxis = new NumberAxis("Scale");
        scaleAxis.setAxisLinePaint(Color.white);
        scaleAxis.setTickMarkPaint(Color.white);
        scaleAxis.setTickLabelFont(new Font("Dialog", Font.PLAIN, 7));
        PaintScaleLegend legend = new PaintScaleLegend(new GrayPaintScale(),
                scaleAxis);
        legend.setStripOutlineVisible(false);
        legend.setSubdivisionCount(20);
        legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        legend.setAxisOffset(5.0);
        legend.setMargin(new RectangleInsets(5, 5, 5, 5));
        legend.setFrame(new BlockBorder(Color.red));
        legend.setPadding(new RectangleInsets(10, 10, 10, 10));
        legend.setStripWidth(10);
        legend.setPosition(RectangleEdge.LEFT);
        //legend.setBackgroundPaint(new Color(120, 120, 180));
        chart.addSubtitle(legend);
        //chart.setBackgroundPaint(new Color(180, 180, 250));
        ChartUtilities.applyCurrentTheme(chart);
        return chart;
    }
	
	


	private XYZDataset createDataset(final double[][] data) {
		return new XYZDataset() {
            public int getSeriesCount() {
                return 1;
            }
            public int getItemCount(int series) {
                return table.length;
            }
            public Number getX(int series, int item) {
                return new Double(data[item][0]);
            }
            
            public Number getY(int series, int item) {
                return new Double(data[item][1]);
            }
            public Number getZ(int series, int item) {
            	return new Double(data[item][2]);
            }
			public void addChangeListener(DatasetChangeListener listener) {
                // ignore - this dataset never changes
            }
            public void removeChangeListener(DatasetChangeListener listener) {
                // ignore
            }
            public DatasetGroup getGroup() {
                return null;
            }
            public void setGroup(DatasetGroup group) {
                // ignore
            }
            public Comparable getSeriesKey(int series) {
                return "xyz";
            }
            public int indexOf(Comparable seriesKey) {
                return 0;
            }
            public DomainOrder getDomainOrder() {
                return DomainOrder.ASCENDING;
            }
			@Override
			public double getXValue(int series, int item) {
				return data[item][0];
			}
			@Override
			public double getYValue(int series, int item) {
				return data[item][1];
			}
			@Override
			public double getZValue(int series, int item) {
				return data[item][2];
			}
        };
	}

	/**
	 * Selects bounds for a set of points.  Recommended margin is 0.25.
	 *
	 * @param points    Array containing points: [x0, y0, x1, y1...]
	 * @param off       Offset into point array.
	 * @param numPoints Number of points to use.
	 * @param margin    Margin around points, in units of the span of the points.
	 * @return 4x1 matrix [minX, minY, maxX, maxY]
	 */
	public double[] selectBounds( double[][] data, double margin ) {
		double x0 = Double.POSITIVE_INFINITY;
		double x1 = Double.NEGATIVE_INFINITY;
		double y0 = Double.POSITIVE_INFINITY;
		double y1 = Double.NEGATIVE_INFINITY;

		for( int i = 0; i < data.length; i++ ) {
			// x
			double v = data[i][0];

			if( v < x0 ) {
				x0 = v;
			}
			if( v > x1 ) {
				x1 = v;
			}

			// y
			v = data[i][1];

			if( v < y0 ) {
				y0 = v;
			}
			if( v > y1 ) {
				y1 = v;
			}
		}

		double mx = (x1 - x0) * margin;
		double my = (y1 - y0) * margin;

		return new double[]{ x0 - mx, y0 - my, x1 + mx, y1 + my };
	}

	public double[][] getTable() {
		return table;
	}
}