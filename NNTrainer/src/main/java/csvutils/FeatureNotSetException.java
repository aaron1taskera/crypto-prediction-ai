package csvutils;

public class FeatureNotSetException extends Exception {
	public FeatureNotSetException(String feature) {
		super("Feature name '" + feature + "' not set");
	}
}
