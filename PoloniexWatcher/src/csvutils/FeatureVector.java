package csvutils;
import java.io.BufferedWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class FeatureVector {
	
	private DataSet ds;
	double[] features;

	public FeatureVector(DataSet ds, int size) {
		this.ds = ds;
		features = new double[size];
		for (int i = 0; i < size; i ++) {
			features[i] = Double.NaN;
		}
	}

	//Checks to see if this FeatureVector matches a template
	public boolean matches(LinkedHashMap<String, Integer> check) {
		for (Map.Entry<String, Integer> entry : check.entrySet()) {
			if (!ds.features.containsKey(entry.getKey())) {
				return false;
			}
		}
		if (check.size() != ds.features.size()) { return false; }
		return true;
	}
	
	public double[] getFeatures() { return features; }

	//Sets a feature to value
	public void setFeature(String name, double value) throws Exception {
		if (ds.features.containsKey(name)) {
			features[ds.features.get(name)] = value;
		} else {
			throw new FeatureNonExistentException();
		}
	}

	//Gets the value of a feature
	public double getFeature(String key) throws Exception {
		if (ds.features.containsKey(key)) {
			if (!Double.isNaN(features[ds.features.get(key)])) {
				return features[ds.features.get(key)];
			} else {
				throw new FeatureNotSetException(key);
			}
		} else {
			throw new FeatureNonExistentException();
		}
	}

	//Returns the name of the first feature that has not been set or null if they have all been
	public String incomplete() {
		for (int i = 0; i < features.length; i ++) {
			if (Double.isNaN(features[i])) {
				for (Map.Entry<String, Integer> entry : ds.features.entrySet()) {
					if (i == entry.getValue()) { return entry.getKey(); }
				}
			}
		}
		return null;
	}

	
	public void output(BufferedWriter bw) throws Exception {
		outputVector(bw);
		bw.newLine();
	}
	
	public void output(BufferedWriter bw, double score) throws Exception {
		outputVector(bw);
		bw.write("," + score);
		bw.newLine();
	}
	
	//Outputs to a buffered writer in csv format
	private void outputVector(BufferedWriter bw) throws Exception {
		boolean first = true;
		for (int i = 0; i < features.length; i ++) {
			if (!first) { bw.write(","); }
			if (!Double.isNaN(features[i])) {
				bw.write(features[i] + "");
			} else {
				String key = "";
				int x = 0;
				for (Map.Entry<String, Integer> entry : ds.features.entrySet()) {
					if (i == x) { key = entry.getKey(); }
					x ++;
				}
				throw new FeatureNotSetException(key);
			}
			first = false;
		}
	}
}