package models;

import csvutils.DataSet;

public class ConfusionMatrix {
	private double predictedNoActualNo;
	private double predictedNoActualYes;
	private double predictedYesActualYes;
	private double predictedYesActualNo;
	
	private DataSet ds;
	private int k;
	
	public ConfusionMatrix(DataSet ds, int k) {
		this.ds = ds;
		this.k = k;
		this.predictedNoActualNo = 0;
		this.predictedNoActualYes = 0;
		this.predictedYesActualYes = 0;
		this.predictedYesActualNo = 0;
	}
	
	public void outputMatrix() {
		double total = predictedNoActualNo + predictedNoActualYes + predictedYesActualYes + predictedYesActualNo;
		System.out.println("n=" + total + "\tPredicted No\tPredicted Yes");
		System.out.println("Actual No\t" + predictedNoActualNo + "\t" + predictedYesActualNo);
		System.out.println("Actual Yes\t" + predictedNoActualYes + "\t" + predictedYesActualYes);
	}
	
	public void outputF1Score() {
		double precision = predictedYesActualYes / (predictedYesActualYes + predictedYesActualNo);
		double recall = predictedYesActualYes / (predictedYesActualYes + predictedNoActualYes);
		System.out.println("Precision:\t" + precision);
		System.out.println("Recall:\t" + recall);
		System.out.println("F1:\t" + (2 * precision * recall) / (precision + recall));
	}
	
	public void calculateAdditional(double[][] netOut, int fold) throws Exception {
		double[] formattedOut = new double[netOut.length];
		for (int i = 0; i < netOut.length; i ++) { formattedOut[i] = netOut[i][0]; }
		calculateAdditional(formattedOut, fold);
	}
	
	public void calculateAdditional(double[] netOut) throws Exception {
		calculateFromScores(netOut, ds.getOutputs());
	}
	
	public void calculateAdditional(double[] netOut, int fold) throws Exception {
		double[][] outputs = ds.getOutputs(k, fold, false);
		calculateFromScores(netOut, outputs);
	}
	
	private void calculateFromScores(double[] netOut, double[][] outputs) {
		for (int i = 0; i < netOut.length; i ++) {
			if (netOut[i] > 0) {
				if (outputs[i][0] > 0) {
					predictedYesActualYes ++;
				} else {
					predictedYesActualNo ++;
				}
			}
			else {
				if (outputs[i][0] > 0) {
					predictedNoActualYes ++;
				} else {
					predictedNoActualNo ++;
				}
			}
		}
	}
}