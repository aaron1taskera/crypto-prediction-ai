package models;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import csvutils.DataSet;
import csvutils.Normalisation;
import dl4jutils.Helpers;

public class TrainedModel extends PredictionModel {
	public Normalisation inputNormalisation;
	public Normalisation outputNormalisation;
	public MultiLayerNetwork[] trained;

	public int getFolds() { return trained.length; }

	public TrainedModel(String name) throws Exception { super(name); }

	public TrainedModel(String name, boolean regression, int timePeriod, DataSet ds, MultiLayerNetwork[] trained) throws Exception {
		super(timePeriod, regression);
		this.inputNormalisation = new Normalisation(ds.getInputs());
		this.outputNormalisation = new Normalisation(ds.getOutputs());
		this.trained = trained;
		serialize(name, ds.getOutputFeature(), ds.getInputsList());
	}

	public void outputScoredFolds(DataSet ds, String name) throws Exception {
		double[] outputs = new double[ds.getVectorCount()];
		ConfusionMatrix cm = new ConfusionMatrix(ds, getFolds());
		for (int i = 0; i < getFolds(); i ++) {
			double[][] getInputTest = inputNormalisation.normalise(ds.getInputs(getFolds(), i, false));
			double[][] out = getRegression() ? outputNormalisation.unnormalise(Helpers.fromINDArray(trained[i].output(Helpers.toINDArray(getInputTest)))) : outputNormalisation.unsquash(Helpers.fromClassificationOutput(Helpers.fromINDArray(trained[i].output(Helpers.toINDArray(getInputTest))))); //unnormalise if not classification, unsquash otherwise
			cm.calculateAdditional(out, i);
			for (int x = 0; x < out.length; x ++) {
				outputs[(x * getFolds()) + i] = out[x][0];
			}
		}
		ArrayList<Double> scores = new ArrayList<Double>();
		for (double d : outputs) { scores.add(d); }
		if (getRegression()) {
			ds.outputCorrelation(scores);
		}

		cm.outputMatrix();
		cm.outputF1Score();

		ds.outputToFile(new File("output-scored-" + name + ".csv"), scores);
	}

	protected int _serialize(String name) throws Exception {
		int folds = trained.length;

		for (int k = 0; k < folds; k ++) {
			File locationToSave = new File(name + "/trained" + k);
			boolean saveUpdater = true;
			ModelSerializer.writeModel(trained[k], locationToSave, saveUpdater);
		}

		ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(new File(name + "/input_normalisation")));
		outStream.writeObject(inputNormalisation);
		outStream.close();

		outStream = new ObjectOutputStream(new FileOutputStream(new File(name + "/output_normalisation")));
		outStream.writeObject(outputNormalisation);
		outStream.close();
		return folds;
	}

	protected int _unserialize(String name) throws Exception {
		ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(new File(name + "/fold_count")));
		int folds = inStream.readInt();
		inStream.close();

		inStream = new ObjectInputStream(new FileInputStream(new File(name + "/time_period")));
		int timePeriod = inStream.readInt();
		inStream.close();

		trained = new MultiLayerNetwork[folds];
		for (int k = 0; k < folds; k ++) {
			File locationToLoad = new File(name + "/trained" + k);
			trained[k] = ModelSerializer.restoreMultiLayerNetwork(locationToLoad);
		}

		inStream = new ObjectInputStream(new FileInputStream(new File(name + "/input_normalisation")));
		inputNormalisation = (Normalisation) inStream.readObject();
		inStream.close();

		inStream = new ObjectInputStream(new FileInputStream(new File(name + "/output_normalisation")));
		outputNormalisation = (Normalisation) inStream.readObject();
		inStream.close();

		return timePeriod;
	}

	public double getTrainedPrediction(double[] input) {
		double[][] normalised = inputNormalisation.normalise(input);
		double[] predictions = new double[trained.length];

		for (int i = 0; i < predictions.length; i ++) {
			predictions[i] = getRegression() ? outputNormalisation.unnormalise(Helpers.fromINDArray(trained[i].output(Helpers.toINDArray(normalised))))[0][0] : outputNormalisation.unsquash(Helpers.fromClassificationOutput(Helpers.fromINDArray(trained[i].output(Helpers.toINDArray(normalised)))))[0][0];
		}

		double sum = 0;
		for (double d : predictions) { sum += d; }
		return sum / (double) predictions.length;
	}

	public double[] getTrainedPrediction(double[][] input) throws Exception {
		double[][] normalised = inputNormalisation.normalise(input);
		double[][] tempPredictions;
		double[] finalPredictions = new double[input.length];

		for (int i = 0; i < trained.length; i ++) {
			tempPredictions =
					getRegression()
						? outputNormalisation.unnormalise(Helpers.fromINDArray(trained[i].output(Helpers.toINDArray(normalised))))
						: outputNormalisation.unsquash(Helpers.fromClassificationOutput(Helpers.fromINDArray(trained[i].output(Helpers.toINDArray(normalised)))));
			for (int x = 0; x < tempPredictions.length; x ++) {
				finalPredictions[x] += tempPredictions[x][0];
			}
		}

		for (int i = 0; i < finalPredictions.length; i ++) { finalPredictions[i] /= (double) trained.length; }
		
		return finalPredictions;
	}
}