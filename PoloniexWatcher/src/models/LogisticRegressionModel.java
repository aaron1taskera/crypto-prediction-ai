package models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import csvutils.DataSet;
import csvutils.Stat;

public class LogisticRegressionModel extends PredictionModel {
	private static final double LEARNING_RATE = 0.1D;
	private static final double ITERATIONS = 100;

	public double[][] coefficients;
	
	public int getFolds() { return coefficients.length; }
	
	public LogisticRegressionModel(String name) throws Exception { super(name); }
	
	public LogisticRegressionModel(String name, int timePeriod, DataSet ds, int k) throws Exception {
		super(timePeriod, false);
		coefficients = new double[k][ds.getInputCount() + 1];
		train(ds, k);
		serialize(name, ds.getOutputFeature(), ds.getInputsList());
	}

	public int _unserialize(String name) throws Exception {
		ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(new File(name + "/fold_count")));
		int folds = inStream.readInt();
		inStream.close();
		
		coefficients = new double[folds][];

		inStream = new ObjectInputStream(new FileInputStream(new File(name + "/time_period")));
		int timePeriod = inStream.readInt();
		inStream.close();
		
		for (int fold = 0; fold < folds; fold ++) {
			File locationToLoad = new File(name + "/trained" + fold);
			ArrayList<Double> parameters = new ArrayList<Double>();
			BufferedReader br = new BufferedReader(new FileReader(locationToLoad));
			String line;
			while ((line = br.readLine()) != null) {
				parameters.add(Double.parseDouble(line));
			}
			br.close();
			coefficients[fold] = new double[parameters.size()];
			for (int i = 0; i < parameters.size(); i ++) {
				coefficients[fold][i] = parameters.get(i);
			}
		}
		
		return timePeriod;
	}

	protected int _serialize(String name) throws Exception {
		for (int fold = 0; fold < coefficients.length; fold ++) {
			File locationToSave = new File(name + "/trained" + fold);
			BufferedWriter bw = new BufferedWriter(new FileWriter(locationToSave));
			boolean first = false;
			for (double d : coefficients[fold]) {
				if (first) { bw.newLine(); }
				bw.write(d + "");
				first = true;
			}
			bw.close();
		}
		return coefficients.length;
	}

	private void train(DataSet ds, int k) throws Exception {
		for (int i = 0; i < k; i ++) {
			double[][] inputs = ds.getInputs(k, i, true);
			double[][] outputs = ds.getOutputs(k, i, true);
			for (int x = 0; x < outputs.length; x ++) {
				outputs[x][0] = Stat.squash(outputs[x][0], 1, 0, 1, -1);
			}
			train(i, inputs, outputs);
		}
	}
	
	private void train(int fold, double[][] inputs, double[][] outputs) throws Exception {
		double[] actualCoefficients = coefficients[fold];
		double min_error = Double.MAX_VALUE;
		double REGULARIZATION_CONSTANT = 0.0D;
		for (int iter = 0; iter < ITERATIONS; iter ++) {
			double squared_error = 0;
			for (int i = 0; i < inputs.length; i ++) {
				double logit = logit(inputs[i], fold);
				double error = outputs[i][0] - logit;
				coefficients[fold][0] = coefficients[fold][0] + error * LEARNING_RATE - 2 * logit * REGULARIZATION_CONSTANT;
				for (int x = 1; x < coefficients[fold].length; x ++) {
					coefficients[fold][x] = coefficients[fold][x] + error * LEARNING_RATE * inputs[i][x-1] - 2 * logit * REGULARIZATION_CONSTANT * inputs[i][x-1];
				}
				squared_error += error * error;
			}
			squared_error /= (double) inputs.length;
			if (squared_error < min_error) {
				min_error = squared_error;
				actualCoefficients = coefficients[fold];
			}
			System.out.println(iter + "\t" +  squared_error + "\t" + min_error);
		}
		coefficients[fold] = actualCoefficients;
	}

	public double[] getTrainedPrediction(double[][] input) throws Exception {
		double[] output = new double[input.length];
		for(int i = 0; i < input.length; i ++) {
			output[i] = getTrainedPrediction(input[i]);
		}
		return output;
	}
	
	public double getTrainedPrediction(double[] input) throws Exception {
		double[] prediction = new double[coefficients.length];
		for (int i = 0; i < prediction.length; i ++) {
			prediction[i] = Stat.unsquash(logit(input, i), 1, 0, 1, -1);
		}
		double sum = 0;
		for (double d : prediction) { sum += d; }
		return sum / (double) prediction.length;
	}

	private double logit(double[] input, int fold) throws Exception {
		double coeff = coefficients[fold][0];
		if (input.length + 1 != coefficients[fold].length) { throw new Exception("Not sized correctly. Expecting input of size " + (coefficients[fold].length - 1) + " got size " + input.length); }
		for (int i = 0; i < input.length; i ++) {
			coeff += input[i] * coefficients[fold][i + 1];
		}
		return 1D / (1D + Math.exp(-coeff));
	}
}