package models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.util.ArrayList;

public class LinearRegressionModel {
	public double[] maxCoefficients;
	public double maxScoreCoefficient;

	public double[] coefficients;
	public double scoreCoefficient;
	public double strong;
	public double good;
	public double buyBoundary;

	private LinearRegressionModel() {  }

	public static LinearRegressionModel unserialize(String name) throws Exception {
		LinearRegressionModel model = new LinearRegressionModel();

		BufferedReader br = new BufferedReader(new FileReader(new File(name + "/coefficients.csv")));
		String line;
		ArrayList<Double> tempCos = new ArrayList<Double>();
		while ((line = br.readLine()) != null) {
			tempCos.add(Double.parseDouble(line));
		}
		model.coefficients = new double[tempCos.size()];
		for (int i = 0; i < tempCos.size(); i ++) { model.coefficients[i] = tempCos.get(i); }
		br.close();

		br = new BufferedReader(new FileReader(new File(name + "/max-coefficients.csv")));
		tempCos = new ArrayList<Double>();
		while ((line = br.readLine()) != null) {
			tempCos.add(Double.parseDouble(line));
		}
		model.maxCoefficients = new double[tempCos.size()];
		for (int i = 0; i < tempCos.size(); i ++) { model.maxCoefficients[i] = tempCos.get(i); }
		br.close();

		br = new BufferedReader(new FileReader(new File(name + "/reg.dat")));
		model.scoreCoefficient = Double.parseDouble(br.readLine());
		model.strong = Double.parseDouble(br.readLine());
		model.good = Double.parseDouble(br.readLine());
		model.buyBoundary = Double.parseDouble(br.readLine());
		model.maxScoreCoefficient = Double.parseDouble(br.readLine());
		br.close();

		return model;
	}

	public double score(double[] input) {
		double score = 0;
		for (int i = 0; i < input.length; i ++) {
			score += coefficients[i] * input[i];
		}
		return score;
	}

	public double scoreMax(double[] input) {
		double score = 0;
		for (int i = 0; i < input.length; i ++) {
			score += maxCoefficients[i] * input[i];
		}
		return score;
	}

	public double getStrength(double[] input) {
		return score(input) / scoreCoefficient;
	}

	public double getMaxStrength(double[] input) {
		return scoreMax(input) * maxScoreCoefficient;
	}

	public boolean buy(double[] input) {
		return score(input) > buyBoundary;
	}

	public String getEmailSubject(double[] input, String ticker) {
		return ticker + " " + getStrengthString(input) + " " + getStrength(input);
	}

	public String getEmailContent(double[] input, String ticker, double curPrice) {
		return "Recommended buy " + ticker + " @ " + Instant.now() + "\r\n" +
				"Cur price: 					" + curPrice + "\r\n" +
				"Predicted increase by sell: 	" + getStrength(input) + "% (" + curPrice * (100 + getStrength(input)) + "\r\n" +
				"Predicted max increase:		" + getMaxStrength(input) + "% (" + curPrice * (100 + getMaxStrength(input));
	}

	public String getStrengthString(double[] input) {
		double score = getStrength(input);
		if (score > strong) {
			return "Strong";
		} else if (score > good) {
			return "Good";
		} else {
			return "Meh";
		}
	}
}