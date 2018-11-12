package models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import csvutils.DataSet;
import trigger.Trigger;

public abstract class PredictionModel {

	private int timePeriod;

	private String output;
	private ArrayList<String> inputs;
	private String name;
	private boolean regression;

	public String getOutputFeature() { return output; }
	public ArrayList<String> getInputFeatures() { return inputs; }

	protected PredictionModel(int timePeriod, boolean regression) {
		setTimePeriod(timePeriod);
		setRegression(regression);
	}

	public PredictionModel(String name) throws Exception {
		this.name = name;
		unserialize(name);
	}

	public String getName() { return name; }

	public abstract int getFolds();

	public void outputScoredFolds(DataSet ds, Trigger t) throws Exception {
		outputScoredFolds(ds, t.getName());
	}

	public void outputScoredFolds(DataSet ds, String name) throws Exception {
		double[] outputs = new double[ds.getVectorCount()];
		ConfusionMatrix cm = new ConfusionMatrix(ds, getFolds());
		for (int i = 0; i < getFolds(); i ++) {
			double[][] getInputTest = ds.getInputs(getFolds(), i, false);
			double[] out = getTrainedPrediction(getInputTest);
			cm.calculateAdditional(out, i);
			for (int x = 0; x < out.length; x ++) {
				outputs[(x * getFolds()) + i] = out[x];
			}
		}

		ArrayList<Double> scores = new ArrayList<Double>();
		for (double d : outputs) { scores.add(d); }
		if (getRegression()) {
			ds.outputCorrelation(scores);
		} else {
			cm.outputMatrix();
			cm.outputF1Score();
		}

		ds.outputToFile(new File("output-scored-" + name + ".csv"), scores);
	}

	public void serialize(String name, String output, ArrayList<String> inputs) throws Exception {
		new File(name).mkdir();
		ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(new File(name + "/time_period")));
		outStream.writeInt(this.getTimePeriod());
		outStream.close();

		int folds = _serialize(name);

		outStream = new ObjectOutputStream(new FileOutputStream(new File(name + "/fold_count")));
		outStream.writeInt(folds);
		outStream.close();

		outStream = new ObjectOutputStream(new FileOutputStream(new File(name + "/fold_count")));
		outStream.writeInt(folds);
		outStream.close();

		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(name + "/output")));
		bw.write(output);
		bw.close();

		bw = new BufferedWriter(new FileWriter(new File(name + "/regression")));
		bw.write(regression + "");
		bw.close();

		bw = new BufferedWriter(new FileWriter(new File(name + "/inputs")));
		for (String s : inputs) {
			bw.write(s);
			bw.newLine();
		}
		bw.close();
	}

	protected abstract int _serialize(String name) throws Exception;

	public final void unserialize(String name) throws Exception {
		setTimePeriod(_unserialize(name));
		BufferedReader br = new BufferedReader(new FileReader(new File(name + "/output")));
		output = br.readLine();
		br.close();

		br = new BufferedReader(new FileReader(new File(name + "/regression")));
		regression = Boolean.parseBoolean(br.readLine());
		br.close();

		br = new BufferedReader(new FileReader(new File(name + "/inputs")));
		inputs = new ArrayList<String>();
		String line;
		while ((line = br.readLine()) != null) {
			inputs.add(line);
		}
		br.close();
	}

	protected abstract int _unserialize(String name) throws Exception;

	public abstract double getTrainedPrediction(double[] input) throws Exception;

	public abstract double[] getTrainedPrediction(double[][] input) throws Exception;

	public final int getTimePeriod() { return timePeriod; }

	private void setTimePeriod(int timePeriod) { this.timePeriod = timePeriod; }
	private void setRegression(boolean regression) { this.regression = regression; }

	public boolean getRegression() { return regression; }

	public static double[][] stack(double[] input) {
		double[][] stack = new double[input.length][1];
		for (int i = 0; i < input.length; i ++) {
			stack[i][0] = input[i];
		}
		return stack;
	}
}