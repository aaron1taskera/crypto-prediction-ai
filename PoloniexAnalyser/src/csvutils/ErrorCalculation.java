package csvutils;

import java.util.ArrayList;

public class ErrorCalculation {
	
	public static double computeAverageError(double[][] actual, double[][] predictions) {
		return computeError(actual, predictions) / actual.length;
	}
	
	//Computes the total error of 2, 2d arrays
	public static double computeError(double[][] actual, double[][] predictions) {
		double total = 0;
		for (int i = 0; i < actual.length; i ++) {
			for (int i2 = 0; i2 < actual[i].length; i2 ++) {
				total += Math.abs(predictions[i][i2] - actual[i][i2]);
			}
		}
		return total;
	}
	
	public static double computeCorrelation(double[][] actual, double[][] predictions) {
		double[] correlations = new double[actual[0].length];
		
		for (int i = 0; i < actual[0].length; i ++) {
			ArrayList<Double> x = new ArrayList<Double>();
			ArrayList<Double> y = new ArrayList<Double>();
			for (int i2 = 0; i2 < actual.length; i2 ++) {
				x.add(actual[i2][i]);
				y.add(predictions[i2][i]);
			}
			correlations[i] = pearsons(x, y);
		}
		
		double mean = 0D;
		for (int i = 0; i < correlations.length; i ++) { mean += correlations[i]; }
		return mean / (double) correlations.length;
	}

	//Computes the total error of two 1d arrays
	public static double computeError(double[] actual, double[] predictions) {
		double total = 0;
		for (int i = 0; i < actual.length; i ++) {
			total += Math.abs(actual[i] - predictions[i]);
		}
		return total;
	}
	
	public static double pearsons(ArrayList<Double> x, ArrayList<Double> y) {
		double meanX = mean(x);
		double meanY = mean(y);
		double rXY = 0;
		double rXX = 0;
		double rYY = 0;
		for (int i = 0; i < x.size(); i ++) {
			rXY += (x.get(i) - meanX) * (y.get(i) - meanY);
			rXX += (x.get(i) - meanX) * (x.get(i) - meanX);
			rYY += (y.get(i) - meanY) * (y.get(i) - meanY);
			if (Double.isNaN(rYY)) {
				System.out.println(y.get(i) + " Y:" + meanY);
			}
			if (Double.isNaN(rXX)) {
				//System.out.println(x.get(i) + " X:" + meanX);
			}
		}

		rXX = Math.pow(rXX, 0.5);
		rYY = Math.pow(rYY, 0.5);

		return rXY / (rXX * rYY);
	}
	
	public static double mean(ArrayList<Double> input) {
		double sum = 0;
		for (Double d : input) { sum += d; }
		return sum / input.size();
	}
}