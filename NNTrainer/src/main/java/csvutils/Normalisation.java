package csvutils;

import java.io.Serializable;

public class Normalisation implements Serializable {
	public Stat[] params;

	public Normalisation(double[][] input) {
		params = new Stat[input[0].length];
		double[] feature;
		for (int i = 0; i < input[0].length; i ++) {
			params[i] = new Stat();
			feature = new double[input.length];
			for (int i2 = 0; i2 < input.length; i2 ++) {
				feature[i2] = input[i2][i];
			}
			params[i].fillStat(feature);
		}
	}
	
	public double[][] normalise(double[][] input) {
		double[][] output = new double[input.length][input[0].length];
		for (int i = 0; i < input.length; i ++) {
			for (int i2 = 0; i2 < input[0].length; i2 ++) {
				output[i][i2] = params[i2].normalise(input[i][i2]);
			}
		}
		return output;
	}
	
	public double[][] normalise(double[] input) {
		double[][] inputExpanded = new double[1][input.length];
		for (int i = 0; i < input.length; i ++) {
			inputExpanded[0][i] = input[i];
		}
		return normalise(inputExpanded);
	}
	
	public double[][] squash(double[][] input) {
		for (int i = 0; i < input.length; i ++) {
			for (int i2 = 0; i2 < input[0].length; i2 ++) {
				input[i][i2] = params[i2].squash(input[i][i2]);
			}
		}
		return input;
	}
	
	public double[][] squashalise(double[][] input) {
		for (int i = 0; i < input.length; i ++) {
			for (int i2 = 0; i2 < input[0].length; i2 ++) {
				input[i][i2] = params[i2].squash(params[i2].normalise(input[i][i2]));
			}
		}
		return input;
	}
	
	public double[][] unnormalise(double[][] input) {
		double[][] output = new double[input.length][input[0].length];
		for (int i = 0; i < input.length; i ++) {
			for (int i2 = 0; i2 < input[0].length; i2 ++) {
				output[i][i2] = params[i2].unnormalise(input[i][i2]);
			}
		}
		return output;
	}
	
	public double[][] unsquash(double[][] input) {
		for (int i = 0; i < input.length; i ++) {
			for (int i2 = 0; i2 < input[0].length; i2 ++) {
				input[i][i2] = params[i2].unsquash(input[i][i2]);
			}
		}
		return input;
	}
	
	public double[][] unsquashalise(double[][] input) {
		for (int i = 0; i < input.length; i ++) {
			for (int i2 = 0; i2 < input[0].length; i2 ++) {
				input[i][i2] = params[i2].unnormalise(params[i2].unsquash(input[i][i2]));
			}
		}
		return input;
	}
}