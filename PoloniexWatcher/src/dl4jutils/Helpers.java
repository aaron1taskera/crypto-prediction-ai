package dl4jutils;

import java.util.Collections;
import java.util.List;

import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import csvutils.DataSet;
import csvutils.Normalisation;

public class Helpers {

	private static final int iterations = 1;
	private static final int train_iterations = 400;
	private static final int batchSize = 2048;

	public static INDArray toINDArray(double[][] input) {
		return Nd4j.create(input);
	}

	public static double[][] fromINDArray(INDArray input) {
		double[][] ret = new double[input.rows()][input.columns()];
		for (int i = 0; i < input.rows(); i ++) {
			for (int i2 = 0; i2 <  input.columns(); i2 ++) {
				ret[i][i2] = input.getDouble(i, i2);
			}
		}
		return ret;
	}

	public static DataSetIterator getTrainingIterator(INDArray inputs, INDArray outputs) {
		org.nd4j.linalg.dataset.DataSet allData = new org.nd4j.linalg.dataset.DataSet(inputs, outputs);
		List<org.nd4j.linalg.dataset.DataSet> list = allData.asList();
		Collections.shuffle(list);
		return new ListDataSetIterator(list, batchSize);
	}

	//Tests the predictions of a network
	public static void testNet(double[][] outputPredictions, double[][] output) {
		double purchases = 0;
		double percMade = 0;
		double total = 0;
		double correct = 0;
		for (int x = 0; x < output.length; x ++) {
			if (outputPredictions[x][0]-0.5 > 0) {
				purchases++;
				if (output[x][0] > 0) {
					correct ++;
				}
				//percMade += output[x][0];
			}
			total ++;
		}
		//System.out.println(total + " " + (percMade / total) + " " + (purchases * 100D / total) + " " + (correct * 100D / total));
	}

	//Tests a TrainedModel
	/*public static void testTrainedModel(DataSet ds, DataSet ds2, TrainedModel tm) throws Exception {
		int folds = tm.trained.length;

		final double maxScale = 1D;

		double purchases = 0;
		double percMade = 0;
		double total = 0;

		for (int i = 0; i < folds; i ++) {
			double[][] input = tm.inputNormalisation.normalise(ds.getInputs(folds, i, false));
			double[][] inputMax = tm.inputMaxNormalisation.normalise(ds2.getInputs(folds, i, false));
			double[] output = ds.getFeature("percentageatoneeighty", folds, i, false);
			double[] outputMax = ds2.getFeature("maxpercentage", folds, i, false);
			double[][] outputPredictions = tm.outputNormalisation.unnormalise(Helpers.fromINDArray(tm.trained[i].output(Helpers.toINDArray(input))));
			double[][] outputMaxPredictions = tm.outputMaxNormalisation.unnormalise(Helpers.fromINDArray(tm.trainedMax[i].output(Helpers.toINDArray(inputMax))));

			for (int x = 0; x < output.length; x ++) {
				if (outputPredictions[x][0] > 0) {
					purchases ++;
					if (outputMaxPredictions[x][0] * maxScale < outputMax[x]) {
						percMade += outputMaxPredictions[x][0] * maxScale;
					} else {
						percMade += output[x];
					}
				}
				total ++;
			}
			System.out.println(total + " " + (percMade / total) + " " + (purchases * 100D / total));
		}

		System.out.println(total + " " + (percMade / total) + " " + (purchases * 100D / total));
	}*/

	public static double[][] toClassificationOutput(double[][] input) {
		double[][] output = new double[input.length][2];
		for (int i = 0; i < output.length; i ++) {
			output[i][0] = input[i][0] == 1 ? 1 : 0;
			output[i][1] = input[i][0] == -1 ? 1 : 0;
		}
		return output;
	}

	public static double[][] fromClassificationOutput(double[][] input) {
		double[][] output = new double[input.length][1];
		for (int i = 0; i < output.length; i ++) {
			output[i][0] = input[i][0] > input[i][1] ? 1 : -1;
		}
		return output;
	}

	//Trains a set of folds from a neural network
	public static MultiLayerNetwork[] getTrainedFolds(DataSet ds, int folds, boolean regression) throws Exception {
		MultiLayerNetwork[] nets = new MultiLayerNetwork[folds];
		Normalisation inputNormalisation = new Normalisation(ds.getInputs());
		Normalisation outputNormalisation = new Normalisation(ds.getOutputs());
		INDArray inputs;
		INDArray outputs;
		INDArray validationInputs;
		INDArray validationOutputs;
		double[][] normalisedInputTrain;
		double[][] outputTrain;
		double[][] normalisedInputValidator;
		double[][] outputValidator;
		for (int i = 0; i < folds; i ++) {
			normalisedInputTrain = inputNormalisation.normalise(ds.getInputs(folds, i, true, true));
			outputTrain = !regression ? toClassificationOutput(outputNormalisation.squash(ds.getOutputs(folds, i, true, true))) : outputNormalisation.normalise(ds.getOutputs(folds, i, true, true));
			inputs = Helpers.toINDArray(normalisedInputTrain);
			outputs = Helpers.toINDArray(outputTrain);
			normalisedInputTrain = null;
			outputTrain = null;

			normalisedInputValidator = inputNormalisation.normalise(ds.getInputs(folds, i, false));
			outputValidator = !regression ? ds.getOutputs(folds, i, false) : outputNormalisation.normalise(ds.getOutputs(folds, i, false));
			validationInputs = Helpers.toINDArray(normalisedInputValidator);
			validationOutputs = Helpers.toINDArray(outputValidator);
			normalisedInputValidator = null;
			outputValidator = null;

			nets[i] = getTrainedNet(outputNormalisation, inputs, outputs, validationInputs, validationOutputs, regression);
		}
		return nets;
	}

	//Gets a trained Neural network
	public static MultiLayerNetwork getTrainedNet(Normalisation outputNormalisation, INDArray inputs, INDArray outputs, INDArray validationInputs, INDArray validationOutputs, boolean regression) {
		MultiLayerConfiguration nn = regression ? getDeepDenseLayerNetworkConfiguration(inputs, outputs) : getDeepDenseLayerNetworkConfigurationClass(inputs, outputs);
		MultiLayerNetwork net = new MultiLayerNetwork(nn);
		MultiLayerNetwork minNet = null;
		double minErr = regression ? Double.MAX_VALUE : 0; //used to be double max value
		double curMin;
		net.init();
		net.setListeners(new ScoreIterationListener(1));
		org.nd4j.linalg.dataset.DataSet scoreSet = new org.nd4j.linalg.dataset.DataSet(validationInputs, validationOutputs);
		DataSetIterator iterator;
		for(int i = 0; i < train_iterations; i++) {
			iterator = Helpers.getTrainingIterator(inputs, outputs);
			net.fit(iterator);
			curMin = regression ? net.score(scoreSet, false) : DataSet.getF1Score(outputNormalisation.unsquash(Helpers.fromClassificationOutput(Helpers.fromINDArray(net.output(validationInputs)))), Helpers.fromINDArray(validationOutputs));
			if (!regression && i < 50) { minErr = 0; }
			if ((regression && curMin < minErr) || (!regression && curMin > minErr)) {
				minNet = net.clone();
				minErr = curMin;
			}
			System.out.println(i + "\t" + curMin + "\t" + minErr);
		}
		scoreSet = new org.nd4j.linalg.dataset.DataSet(validationInputs, validationOutputs);
		return minNet;
	}

	//Creates a new topology for a regression neural network
	public static MultiLayerConfiguration getDeepDenseLayerNetworkConfigurationClass(INDArray inputs, INDArray outputs) {
		ListBuilder list = new NeuralNetConfiguration.Builder()
				.iterations(iterations)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				//.regularization(true)
				//.dropOut(0.4)
				//.l1(0.001)
				//.l2(0.001)
				.weightInit(WeightInit.XAVIER)
				.updater(new Adam()) //new Adam() //0.25 smoller datasets
				//.updater(new Adam(0.001, 0.9, 0.999, 1e-08)).learningRate(0.2D) //new Adam() //0.25 smoller datasets
				.list();
		/*int dec = -80;
		int decInc = 60;
		int i = 0;
		int lastNodes = inputs.columns();
		int nodes;
		while ((nodes = (inputs.columns() - dec * i)) > outputs.columns()) {
			System.out.println(lastNodes);
			list = list.layer(i, new DenseLayer.Builder().nIn(lastNodes).nOut(nodes)
					.activation(Activation.TANH).build());
			lastNodes = nodes;
			dec += decInc;
			i ++;
		}*/

		list = list.layer(0, new DenseLayer.Builder().nIn(inputs.columns()).nOut(65)
				.activation(Activation.RELU).build());
		list = list.layer(1, new DenseLayer.Builder().nIn(65).nOut(45)
				.activation(Activation.RELU).build());
		list = list.layer(2, new DenseLayer.Builder().nIn(45).nOut(30)
				.activation(Activation.RELU).build());
		list = list.layer(3, new DenseLayer.Builder().nIn(30).nOut(25)
				.activation(Activation.RELU).build());
		list = list.layer(4, new DenseLayer.Builder().nIn(25).nOut(20)
				.activation(Activation.RELU).build());
		list = list.layer(5, new DenseLayer.Builder().nIn(20).nOut(15)
				.activation(Activation.RELU).build());
		list = list.layer(6, new DenseLayer.Builder().nIn(15).nOut(10)
				.activation(Activation.RELU).build());
		list = list.layer(7, new DenseLayer.Builder().nIn(10).nOut(5)
				.activation(Activation.RELU).build());
		return list.layer(8, new OutputLayer.Builder(LossFunctions.LossFunction.XENT)
				.activation(Activation.SOFTMAX)
				.nIn(5).nOut(outputs.columns()).build())
				.pretrain(false).backprop(true).build();
	}

	//Creates a new topology for a regression neural network
	public static MultiLayerConfiguration getDeepDenseLayerNetworkConfiguration(INDArray inputs, INDArray outputs) {
		ListBuilder list = new NeuralNetConfiguration.Builder()
				.iterations(iterations)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.regularization(true)
				//.dropOut(0.9)
				//.l1(0.001)
				.l2(1e-5)
				.weightInit(WeightInit.XAVIER)
				.updater(Updater.ADAM).learningRate(0.2D) //new Adam() //0.25 smoller datasets
				//.updater(new Adam(0.001, 0.9, 0.999, 1e-08)).learningRate(0.2D) //new Adam() //0.25 smoller datasets
				.list();
		/*int dec = -80;
		int decInc = 60;
		int i = 0;
		int lastNodes = inputs.columns();
		int nodes;
		while ((nodes = (inputs.columns() - dec * i)) > outputs.columns()) {
			System.out.println(lastNodes);
			list = list.layer(i, new DenseLayer.Builder().nIn(lastNodes).nOut(nodes)
					.activation(Activation.TANH).build());
			lastNodes = nodes;
			dec += decInc;
			i ++;
		}*/

		list = list.layer(0, new DenseLayer.Builder().nIn(inputs.columns()).nOut(65)
				.activation(Activation.RELU).build());
		list = list.layer(1, new DenseLayer.Builder().nIn(65).nOut(45)
				.activation(Activation.RELU).build());
		list = list.layer(2, new DenseLayer.Builder().nIn(45).nOut(30)
				.activation(Activation.RELU).build());
		list = list.layer(3, new DenseLayer.Builder().nIn(30).nOut(25)
				.activation(Activation.RELU).build());
		list = list.layer(4, new DenseLayer.Builder().nIn(25).nOut(20)
				.activation(Activation.RELU).build());
		list = list.layer(5, new DenseLayer.Builder().nIn(20).nOut(15)
				.activation(Activation.RELU).build());
		list = list.layer(6, new DenseLayer.Builder().nIn(15).nOut(10)
				.activation(Activation.RELU).build());
		list = list.layer(7, new DenseLayer.Builder().nIn(10).nOut(5)
				.activation(Activation.RELU).build());
		return list.layer(8, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
				.activation(Activation.IDENTITY)
				.nIn(5).nOut(outputs.columns()).build())
				.pretrain(false).backprop(true).build();
	}
}