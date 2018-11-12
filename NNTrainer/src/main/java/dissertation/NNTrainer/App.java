package dissertation.NNTrainer;

import java.io.File;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import csvutils.DataSet;
import dl4jutils.Helpers;
import models.ConfusionMatrix;
import models.LogisticRegressionModel;
import models.PredictionModel;
import models.TrainedModel;
import trigger.TriggerAll;
import trigger.TriggerMACDThirty;
import trigger.TriggerPriceSpike;
import trigger.TriggerSideways;
import trigger.TriggerVolumeSpike;

public class App {
	public static void main (String[] args) {
		try {
			TriggerMACDThirty trig = new TriggerMACDThirty();
			DataSet ds = trig.loadMax();
			ds.pruneUseless();
			MultiLayerNetwork[] trained = Helpers.getTrainedFolds(ds, 5, true);
			TrainedModel tm = new TrainedModel(trig.getName() + "-max", true, trig.getTimePeriod(), ds, trained);
			tm.outputScoredFolds(ds, trig.getName() + "-max");

			/*TriggerPriceSpike trig = new TriggerPriceSpike();
			DataSet ds = trig.loadDidPump();
			ds.pruneUseless();
			TrainedModel pm = new TrainedModel("price-spike");
			double[] outputs = pm.getTrainedPrediction(ds, ds.getInputs());
			double[][] actualOutputs = ds.getOutputs();
			for (int i = 0; i < ds.getVectorCount(); i ++) {
				System.out.println(outputs[i] + " " + actualOutputs[i][0]);
			}*/
			
			//DataSet.LOAD_SKIP_CONSTANT = 0.85D;
			/*TriggerAll trig = new TriggerAll();
			DataSet ds = trig.loadDidPump();
			//ds.pruneUseless();
			MultiLayerNetwork[] trained = Helpers.getTrainedFolds(ds, 5, false);
			TrainedModel tm = new TrainedModel(trig.getName() + "-accumulation", false, trig.getTimePeriod(), ds, trained);
			tm.outputScoredFolds(ds, trig.getName());*/

			//DataSet.LOAD_SKIP_CONSTANT = 0.85D;
			/*TriggerAll trig = new TriggerAll();
			DataSet ds = trig.loadMax();
			ds.pruneUseless();
			MultiLayerNetwork[] trained = Helpers.getTrainedFolds(ds, 5, true);
			TrainedModel tm = new TrainedModel(trig.getName() + "-max", true, trig.getTimePeriod(), ds, trained);
			tm.outputScoredFolds(ds, trig.getName() + "-max");

			/*TriggerSideways trig = new TriggerSideways();
			DataSet ds = trig.loadDidPump();
			ds.pruneUseless();
			MultiLayerNetwork[] trained = Helpers.getTrainedFolds(ds, 5);
			TrainedModel tm = new TrainedModel(trig.getName(), trig.getTimePeriod(), ds, trained);
			tm.outputScoredFolds(ds, trig.getName());

			/*TriggerAll trigAll = new TriggerAll();
			ds = trigAll.load();
			ds.pruneUseless();
			trained = Helpers.getTrainedFolds(ds, 5);
			tm = TrainedModel.saveModels(trig.getName(), ds, trained);

			Helpers.outputScoredFolds(ds, tm, trig);

			/*trig = new TriggerMACross();
			ds = trig.load();
			ds.pruneUseless();
			trained = Helpers.getTrainedFolds(ds, 5);
			tm = TrainedModel.saveModels(trig.getName(), ds, trained);

			trig = new TriggerMACross();
			ds = trig.loadMax();
			ds.pruneUseless();
			trained = Helpers.getTrainedFolds(ds, 5);
			tm = TrainedModel.saveModels(trig.getName() + "-max", ds, trained);*/
		} catch (Exception e) { e.printStackTrace(); }
	}

	int numInputs = 176;

	public static void test() {
		try {
			RecordReader rr = new CSVRecordReader();
			rr.initialize(new FileSplit(new File("output-rsi.csv")));
			DataSetIterator trainIter = new RecordReaderDataSetIterator(rr, 1024, 0, 2);

			RecordReader rrTest = new CSVRecordReader();
			rrTest.initialize(new FileSplit(new File("output-rsi-test.csv")));
			DataSetIterator testIter = new RecordReaderDataSetIterator(rrTest, 1024, 0, 2);

			MultiLayerConfiguration topology = new NeuralNetConfiguration.Builder()
					.iterations(1)
					.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
					.learningRate(0.01)
					.updater(new Nesterovs(0.9))
					.list()
					.layer(0, new DenseLayer.Builder().nIn(2).nOut(5)
							.activation(Activation.RELU)
							.weightInit(WeightInit.XAVIER).build())
					.layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
							.nIn(5)
							.nOut(2)
							.activation(Activation.SOFTMAX)
							.weightInit(WeightInit.XAVIER)
							.build())
					.pretrain(false).backprop(true).build();

			MultiLayerNetwork model = new MultiLayerNetwork(topology);
			model.init();
			//model.setListeners(new ScoreIterationListener(1));
			for (int n = 0; n < 10; n ++) {
				model.fit(trainIter);
			}

			Evaluation eval = new Evaluation(2);
			while (testIter.hasNext()) {
				org.nd4j.linalg.dataset.DataSet t = testIter.next();
				INDArray features = t.getFeatureMatrix();
				INDArray labels = t.getLabels();
				INDArray predicted = model.output(features, false);
				eval.eval(labels, predicted);
			}
			System.out.println(eval);
		} catch (Exception e) { e.printStackTrace(); }
	}
}