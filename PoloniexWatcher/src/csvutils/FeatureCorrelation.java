package csvutils;

public class FeatureCorrelation implements Comparable<FeatureCorrelation> {
	public String featureName;
	public Double correlation;
	public Double exactCorrelation;

	public FeatureCorrelation(String featureName, Double correlation) {
		this.featureName = featureName;
		this.exactCorrelation = correlation;
		this.correlation = Math.abs(correlation);
	}
	
	public int compareTo(FeatureCorrelation o) {
		return correlation.compareTo(o.correlation);
	}
}