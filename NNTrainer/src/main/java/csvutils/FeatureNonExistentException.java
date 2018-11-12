package csvutils;

public class FeatureNonExistentException extends Exception {
	public FeatureNonExistentException() {
		super("Feature does not exist");
	}
}
