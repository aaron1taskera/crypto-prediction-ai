package trigger;

import java.util.Arrays;

import csvutils.DataSet;
import csvutils.FeatureVector;
import stocks.Candlestick;
import stocks.Numerics;

public class TriggerAccumulation extends Trigger {

	public TriggerAccumulation() throws Exception { super("accumulation", Trigger.HALF_HOUR); }

	protected void addFeaturesToDataSet(DataSet set) throws Exception {
		set.addFeature("didpump");							//1 or 0 indicating whether or not this stock pumped
	}

	public boolean isTriggered(Candlestick[] candles, Candlestick[] btcCandles, int index) {
		Candlestick[] pastCandles = Arrays.copyOfRange(candles, 0, index + 1);
		double[] tmfs = Candlestick.calculateTMFGraph(pastCandles, Trigger.FORTNIGHT / getTimePeriod());
		for (int i = 0; i < tmfs.length; i ++) {
			if (tmfs[i] >= 0) { return false; }
		}
		return true;
	}

	private double calculateDifference(Candlestick[] candles, Candlestick[] btcCandles, int index) {
		Candlestick[] pastCandles = Arrays.copyOfRange(candles, 0, index + 1);
		double[] tempCandles = Candlestick.weightedAverages(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_MONTH / getTimePeriod(), index + 1));
		double pearsonsPrice = Numerics.pearsons(Numerics.generateTime(tempCandles), tempCandles);
		tempCandles = Candlestick.calculateTMFGraph(pastCandles, Trigger.ONE_MONTH / getTimePeriod());
		double pearsonsTMF = Numerics.pearsons(Numerics.generateTime(tempCandles), tempCandles);
		return pearsonsTMF - pearsonsPrice;
	}

	@Override
	protected void _calculateFeatures(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv,
			String ticker, boolean future) throws Exception {
		calculateDidPump(candles, btcCandles, index, fv, ticker, future);
	}

	@Override
	protected void _load(DataSet ds) throws Exception {  }

	@Override
	protected void _loadMax(DataSet ds) throws Exception {  }

	public DataSet loadDidPump() throws Exception {
		DataSet ds = loadTemplate();

		ds.markOutput("didpump");
		markTradeFeaturesUnused(ds);
		return ds;
	}
}
