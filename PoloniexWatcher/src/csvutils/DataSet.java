package csvutils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

//A set of feature vectors which can be used to train a machine
public class DataSet {
	LinkedHashMap<String, Integer> features;
	ArrayList<String> featuresIndex;
	private LinkedList<FeatureVector> vectors;
	private ArrayList<String> outputs;
	private ArrayList<String> unused;
	private boolean inserted;

	private static final int PRUNE_AMOUNT = 140;

	public DataSet() {
		inserted = false;
		outputs = new ArrayList<String>();
		unused = new ArrayList<String>();
		features = new LinkedHashMap<String, Integer>();
		featuresIndex = new ArrayList<String>();
		vectors = new LinkedList<FeatureVector>();
	}

	public String getKeyAt(int value) throws Exception {
		return this.featuresIndex.get(value);
	}

	//Takes a set of outputs and splits them by the score
	public static double[][] classify(double[][] outputs, double score) {
		double[][] classifications = new double[outputs.length][1];
		for (int i = 0; i < outputs.length; i ++) {
			classifications[i][0] = outputs[i][0] < score ? -1 : 1;
		}
		return classifications;
	}

	//Checks if anything has been inserted into the DataSet
	private boolean isInserted() { return inserted || vectors.size() > 0; }

	//Get # of features
	public int getFeatureCount() { return features.size(); }

	//Get # of entries in the DataSet
	public int getVectorCount() { return vectors.size(); }

	//Will throw an exception if the key is either in output or unused
	public void checkUnmarked(String key) throws Exception {
		if (outputs.contains(key)) {
			throw new Exception("Key '" + key + "' already exists in outputs");
		} else if (unused.contains(key)) {
			throw new Exception("Key '" + key + "' already exists in unused");
		}
	}

	//Marks a feature as an output
	public void markOutput(String key) throws Exception {
		if (features.containsKey(key)) {
			checkUnmarked(key);
			outputs.add(key);
		} else {
			throw new FeatureNonExistentException();
		}
	}

	//Clears the marks made to output and unused
	public void wipeMarks() {
		unused.clear();
		outputs.clear();
	}

	//Marks a feature unused so it won't be returned in inputs
	public void markUnused(String key) throws Exception {
		if (features.containsKey(key)) {
			checkUnmarked(key);
			unused.add(key);
		} else {
			throw new FeatureNonExistentException();
		}
	}

	public int getInputCount() { return features.size() - outputs.size() - unused.size(); }
	public int getOutputCount() { return outputs.size(); }

	//Gets all the features of a specific name
	public double[] getFeature(String key) throws Exception { return getFeature(key, 1, 1, true); }

	public double[] getFeature(String key, int k, int fold, boolean train) throws Exception {
		return getFeature(key, k, fold, train, false);
	}

	//Gets all the features of a specific name using cross validation. K partitions, partition # fold is excluded in train = true, all other # are excluded when train = false
	public double[] getFeature(String key, int k, int fold, boolean train, boolean validator) throws Exception {
		ArrayList<Double> outputs = new ArrayList<Double>();
		int i = 0;
		for (FeatureVector vector : vectors) {
			if (shouldSkipFold(k, fold, train, validator, i)) { i++; continue; }
			outputs.add(vector.getFeature(key));
			i ++;
		}
		return primitives(outputs.toArray(new Double[outputs.size()]));
	}

	//Turns a Double[] to a double[]
	public static double[] primitives(Double[] input) {
		double[] output = new double[input.length];
		for (int i = 0; i < input.length; i ++) {
			output[i] = input[i];
		}
		return output;
	}

	//Gets all the outputs to be trained on
	public double[][] getOutputs() throws Exception { return getOutputs(1, 1, true); }

	public double[][] getOutputs(int k, int fold, boolean train) throws Exception { return getOutputs(k, fold, train, false); }

	//Gets the outputs to be trained on using cross validation. K partitions, partition # fold is excluded in train = true, all other # are excluded when train = false
	public double[][] getOutputs(int k, int fold, boolean train, boolean validator) throws Exception {
		ArrayList<Double[]> outputs = new ArrayList<Double[]>();
		int i = 0;
		for (FeatureVector vector : vectors) {
			Double[] output = new Double[getOutputCount()];
			int i2 = 0;
			if (shouldSkipFold(k, fold, train, validator, i)) { i++; continue; }
			for (int x = 0; x < vector.features.length; x ++) {
				if (this.outputs.contains(getKeyAt(x))) {
					output[i2] = vector.features[x];
					i2 ++;
				}
			}
			outputs.add(output);
			i ++;
		}
		return to2DArray(outputs);
	}

	//Gets all the inputs to be trained on
	public double[][] getInputs() throws Exception { return getInputs(1, 1, true); }

	public double[][] getInputs(int k, int fold, boolean train) throws Exception {
		return getInputs(k, fold, train, false);
	}

	//Gets the inputs to be trained on using cross validation. K partitions, partition # fold is excluded in train = true, all other # are excluded when train = false
	public double[][] getInputs(int k, int fold, boolean train, boolean validator) throws Exception {
		ArrayList<Double[]> inputs = new ArrayList<Double[]>();
		int i = 0;
		int z = 0;
		for (FeatureVector vector : vectors) {
			z ++;
			Double[] input = new Double[getInputCount()];
			int i2 = 0;
			if (shouldSkipFold(k, fold, train, validator, i)) { i++; continue; }
			for (int x = 0; x < vector.features.length; x ++) {
				if (!this.outputs.contains(getKeyAt(x)) && !this.unused.contains(getKeyAt(x))) {
					input[i2] = vector.features[x];
					i2 ++;
				}
			}
			inputs.add(input);
			i ++;
		}
		return to2DArray(inputs);
	}

	private boolean shouldSkipFold(int k, int fold, boolean train, boolean validator, int i) {
		if (!validator) {
			if (train ? i % k == fold : i % k != fold) { return true; }
			else { return false; }
		} else {
			if (train ? (i + 1) % k == fold || (i % k == fold) : (i + 1) % k != fold) { return true; }
			else { return false; }
		}
	}

	//Takes a arraylist of Double[] and turns it into a double[][]
	public static double[][] to2DArray(ArrayList<Double[]> input) {
		if (input.size() == 0) { return new double[0][0]; }
		double[][] outputs = new double[input.size()][input.get(0).length];
		for (int i = 0; i < input.size(); i ++) {
			outputs[i] = primitives(input.get(i));
		}
		return outputs;
	}

	//Adds a feature to the DataSet
	public void addFeature(String key) throws Exception {
		if (!isInserted()) {
			if (!features.containsKey(key)) {
				features.put(key, features.size());
				featuresIndex.add(key);
			} else {
				throw new Exception("Feature " + key + " already exists");
			}
		} else {
			throw new Exception("Cannot add features after getting a vector");
		}
	}

	//Loads a DataSet in CSV format from a file
	public static DataSet loadFromFile(File f) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(f));
		DataSet set = load(br);
		br.close();
		return set;
	}

	public void outputCorrelation(double[] scores) throws Exception {
		ArrayList<Double> scoresList = new ArrayList<Double>();
		for (double score : scores) { scoresList.add(score); }
		outputCorrelation(scoresList);
	}

	public void outputCorrelation(ArrayList<Double> scores) throws Exception {
		ArrayList<FeatureCorrelation> fc = new ArrayList<FeatureCorrelation>();
		for (int x = 0; x < this.getFeatureCount(); x ++) {
			if (this.outputs.contains(getKeyAt(x)) || this.unused.contains(getKeyAt(x))) { continue; }
			double[] featureArray = getFeature(getKeyAt(x));
			ArrayList<Double> y = new ArrayList<Double>();
			for (double d : featureArray) { y.add(d); }
			fc.add(new FeatureCorrelation(getKeyAt(x), ErrorCalculation.pearsons(y, scores)));
		}
		Collections.sort(fc, Collections.reverseOrder());
		for (FeatureCorrelation f : fc) {
			System.out.println(f.featureName + "\t" + f.exactCorrelation);
		}

		double[][] featureArray = this.getOutputs();
		ArrayList<Double> y = new ArrayList<Double>();
		for (int i = 0; i < featureArray.length; i ++) { y.add(featureArray[i][0]); }
		System.out.println("Output:\t" + ErrorCalculation.pearsons(y, scores));
	}

	public void markUnusedUnless(String output, ArrayList<String> inputs) throws Exception {
		for (int x = 0; x < this.getFeatureCount(); x ++) {
			if (!inputs.contains(getKeyAt(x)) && !output.equals(getKeyAt(x))) {
				markUnused(getKeyAt(x));
			}
		}
	}

	public String getOutputFeature() throws Exception {
		for (int x = 0; x < this.getFeatureCount(); x ++) {
			if (this.outputs.contains(getKeyAt(x))) { return getKeyAt(x); }
		}
		throw new Exception("Output does not exist");
	}

	public ArrayList<String> getInputsList() throws Exception {
		ArrayList<String> inputs = new ArrayList<String>();
		for (int x = 0; x < this.getFeatureCount(); x ++) {
			if (this.outputs.contains(getKeyAt(x)) || this.unused.contains(getKeyAt(x))) { continue; }
			inputs.add(getKeyAt(x));
		}
		return inputs;
	}

	public void pruneUseless() throws Exception {
		ArrayList<Double> outputsList = new ArrayList<Double>();
		double[][] outputs = this.getOutputs();
		for (int i = 0; i < outputs.length; i ++) {
			outputsList.add(outputs[i][0]);
		}
		ArrayList<FeatureCorrelation> fc = new ArrayList<FeatureCorrelation>();
		for (int x = 0; x < this.getFeatureCount(); x ++) {
			if (this.outputs.contains(getKeyAt(x)) || this.unused.contains(getKeyAt(x))) { continue; }
			ArrayList<Double> feature = new ArrayList<Double>();
			double[] featureArray = getFeature(getKeyAt(x));
			double[][] finalFeatureArray = new double[featureArray.length][];
			for (int i = 0; i < featureArray.length; i ++) {
				finalFeatureArray[i] = new double[1];
				finalFeatureArray[i][0] = featureArray[i];
			}
			for (double d : featureArray) { feature.add(d); }
			fc.add(new FeatureCorrelation(getKeyAt(x), ErrorCalculation.computeCorrelation(outputs, finalFeatureArray)));
		}

		Collections.sort(fc, Collections.reverseOrder());

		while (fc.get(0).correlation.isNaN()) {
			this.markUnused(fc.get(0).featureName);
			System.out.println("Unused NaN: " + fc.remove(0).featureName);
		}

		for (int i = 0; i < PRUNE_AMOUNT; i ++) {
			System.out.println(fc.get(i).featureName + " " + fc.get(i).exactCorrelation);
		}

		for (int i = PRUNE_AMOUNT; i < fc.size(); i ++) {
			System.out.println("Unused: " + fc.get(i).featureName + " " + fc.get(i).exactCorrelation);
			this.markUnused(fc.get(i).featureName);
		}
	}

	public static double LOAD_SKIP_CONSTANT = 1;

	//Loads a DataSet in CSV format from a BufferedReader
	public static DataSet load(BufferedReader br) throws Exception {
		DataSet set = new DataSet();
		String[] headers = br.readLine().split(",");
		for (String feature : headers) {
			set.addFeature(feature);
		}
		String line;
		String[] split;
		while ((line = br.readLine()) != null) {
			FeatureVector fv = set.genBlankVector();
			if (Math.random() > LOAD_SKIP_CONSTANT) { continue; }
			split = line.split(",");
			for (int i = 0; i < split.length; i ++) {
				fv.setFeature(headers[i], Double.parseDouble(split[i]));
			}
			set.insertVector(fv);
		}
		Collections.shuffle(set.vectors);
		return set;
	}

	public void pruneDataset(int amount) {
		for (int i = 0; i < amount; i ++) {
			vectors.removeLast();
		}
	}

	//Inserts a FeatureVector into the DataSet
	public void insertVector(FeatureVector fv) throws Exception {
		String s;
		if (!fv.matches(features)) {
			throw new Exception("Feature Vector does not match predefined template for this DataSet");
		}
		if ((s = fv.incomplete()) != null) {
			throw new FeatureNotSetException(s);
		}
		vectors.add(fv);
	}

	//Generated a blank feature vector which can be filled
	public FeatureVector genBlankVector() {
		inserted = true;
		return new FeatureVector(this, features.size());
	}

	//Outputs this DataSet in csv format to a file
	public void outputToFile(File f) throws Exception {
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		output(bw);
		bw.close();
	}

	//Outputs this DataSet in csv format to a file along with scores
	public void outputToFile(File f, ArrayList<Double> scores) throws Exception {
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		output(bw, scores);
		bw.close();
	}

	//Outputs the FeatureVectors to a file and removes them from the DataSet
	public void flushToFile(File f) throws Exception {
		BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
		flush(bw);
		bw.close();
	}

	//Outputs the FeatureVectors to a BufferedWriter and removes them from the DataSet
	public void flush(BufferedWriter bw) throws Exception {
		for (FeatureVector fv : vectors) {
			fv.output(bw);
		}
		vectors.clear();
	}

	//Outputs the headers in csv format to a File
	public void outputHeadersToFile(File f) throws Exception {
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		outputHeaders(bw);
		bw.newLine();
		bw.close();
	}

	//Outputs the headers in csv format to a BufferedWriter
	public void outputHeaders(BufferedWriter bw) throws Exception {
		boolean first = true;
		for (Map.Entry<String, Integer> entry : features.entrySet()) {
			if (this.unused.contains(entry.getKey())) { continue; }
			if (!first) { bw.write(","); }
			bw.write(entry.getKey());
			first = false;
		}
	}

	//Outputs the headers in csv format to a BufferedWriter
	public void outputAllHeaders(BufferedWriter bw) throws Exception {
		boolean first = true;
		for (Map.Entry<String, Integer> entry : features.entrySet()) {
			if (!first) { bw.write(","); }
			bw.write(entry.getKey());
			first = false;
		}
	}

	//Outputs this DataSet in csv format to a BufferedWriter
	public void output(BufferedWriter bw) throws Exception {
		outputHeaders(bw);
		bw.newLine();
		flush(bw);
	}

	//Outputs this DataSet in csv format with scores to a BufferedWriter
	public void output(BufferedWriter bw, ArrayList<Double> scores) throws Exception {
		outputAllHeaders(bw);
		bw.write(",score");
		bw.newLine();
		flush(bw, scores);
	}

	//Outputs the FeatureVectors to a BufferedWriter with the scores
	public void flush(BufferedWriter bw, ArrayList<Double> scores) throws Exception {
		int i = 0;
		for (FeatureVector fv : vectors) {
			fv.output(bw, scores.get(i));
			i++;
		}
	}

	public double getPrecision(double[][] netOut, int k, int fold) throws Exception {
		double[][] realOut = getOutputs(k, fold, false);
		return getPrecision(netOut, realOut);
	}

	public double getRecall(double[][] netOut, int k, int fold) throws Exception {
		double[][] realOut = getOutputs(k, fold, false);
		return getRecall(netOut, realOut);
	}

	public static double getRecall(double[][] netOut, double[][] realOut) {
		double correct = 0;
		double total = 0;
		for (int i = 0; i < netOut.length; i ++) {
			for (int i2 = 0; i2 < netOut[i2].length; i2 ++) {
				if (netOut[i][i2] > 0 && realOut[i][i2] > 0) { correct++; }
				if (realOut[i][i2] > 0) { total ++; }
			}
		}
		return correct / total;
	}

	public static double getPrecision(double[][] netOut, double[][] realOut) {
		double correct = 0;
		double total = 0;
		for (int i = 0; i < netOut.length; i ++) {
			for (int i2 = 0; i2 < netOut[i2].length; i2 ++) {
				if (netOut[i][i2] > 0 && realOut[i][i2] > 0) { correct++; }
				if (netOut[i][i2] > 0) { total ++; }
			}
		}
		return correct / total;
	}

	public static double getF1Score(double[][] netOut, double[][] realOut) {
		return (2D / ((1D / getPrecision(netOut, realOut)) + (1D / getRecall(netOut, realOut))));
	}
	
	public double getF1Score(double[][] netOut, int k, int fold) throws Exception {
		return (2D / ((1D / getPrecision(netOut, k, fold)) + (1D / getRecall(netOut, k, fold))));
	}
}