package trigger;

import java.util.Arrays;

import csvutils.DataSet;
import csvutils.FeatureVector;
import stocks.Candlestick;

public class TriggerNegativeTMF extends Trigger {
	public TriggerNegativeTMF() throws Exception { super("negative-tmf", Trigger.HALF_HOUR); }

	protected void addFeaturesToDataSet(DataSet set) throws Exception {
		set.addFeature("didpump");							//1 or 0 indicating whether or not this stock pumped
	}

	public boolean isTriggered(Candlestick[] candles, Candlestick[] btcCandles, int index) {
		Candlestick[] pastCandles = Arrays.copyOfRange(candles, 0, index + 1);
		double[] tmfs = Candlestick.calculateTMFGraph(pastCandles, Trigger.THREE_DAYS / getTimePeriod());
		for (int i = 0; i < tmfs.length - 1; i ++) {
			if (tmfs[i] >= 0) { return false; }
		}
		return tmfs[tmfs.length - 1] > 0;
	}

	@Override
	protected void _calculateFeatures(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv,
			String ticker, boolean future) throws Exception {
		calculateDidPump(candles, btcCandles, index, fv, ticker, future);
	}

	@Override
	protected void _load(DataSet ds) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _loadMax(DataSet ds) throws Exception {
		// TODO Auto-generated method stub

	}

	public DataSet loadDidPump() throws Exception {
		DataSet ds = loadTemplate();

		ds.markOutput("didpump");
		markTradeFeaturesUnused(ds);
		return ds;
	}
}