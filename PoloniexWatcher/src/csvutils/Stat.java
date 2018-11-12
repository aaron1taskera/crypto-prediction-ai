package csvutils;

import java.io.Serializable;

public class Stat implements Serializable {
	public double mean;
	public double std;
	public double min;
	public double max;

	public void fillStat(double[] nums) {
		mean = 0D;
		min = Double.MAX_VALUE;
		max = -Double.MAX_VALUE;
		for (double d : nums) {
			mean += d;
			if (d > max) {
				max = d;
			}
			if (d < min) {
				min = d;
			}
		}
		mean /= (double) nums.length;

		for(double d : nums) {
			std += (d-mean)*(d-mean);
		}
		std = Math.pow(std/(double)(nums.length-1), 0.5);

		if (Double.isNaN(std)) { std = 0; }
		if (Double.isNaN(mean)) { mean = 0; }
	}

	public double normalise(double input) {
		double normalised = (input - mean) / std;
		return lockRange(-3.5, normalised, 3.5);
	}

	public double unnormalise(double input) {
		return (input * std) + mean;
	}

	public static double squash(double input, double upper, double lower, double max, double min) {
		double localmax = upper;
		double localmin = lower;
		double factor;
		double diff;
		if (max - min != 0) {
			factor = (max - min) / (localmax - localmin);
			diff = (max / factor) - localmax;
		} else {
			factor = 1;
			diff = max;
		}
		return (input / factor) - diff;
	}
	
	public double squash(double input) {
		return squash(input, 1, -1, max, min);
	}

	public double unsquash(double input) {
		return unsquash(input, 1, -1, max, min);
	}
	
	public static double unsquash(double input, double upper, double lower, double max, double min) {
		double localmax = upper;
		double localmin = lower;
		double factor;
		double diff;
		if (max - min != 0) {
			factor = (max - min) / (localmax - localmin);
			diff = (max / factor) - localmax;
		} else {
			factor = 1;
			diff = max;
		}
		return (input + diff)*factor;
	}

	public static double lockRange(double lower, double num, double upper) {
		if (Double.isNaN(num)) { return (upper + lower) / 2D; }
		return Math.min(upper, Math.max(lower, num));
	}
}