package trigger;

import java.util.Arrays;

import csvutils.DataSet;
import csvutils.FeatureVector;
import stocks.Candlestick;
import stocks.Numerics;

public class TriggerSideways extends Trigger {

	public TriggerSideways() throws Exception { super("sideways", Trigger.HALF_HOUR); }

	@Override
	protected void addFeaturesToDataSet(DataSet set) throws Exception {
		set.addFeature("didpump");                 	        //1 or 0 indicating whether this stock pumped within the period
		set.addFeature("didpumpimmediate");                 //1 or 0 indicating whether this stock pumped within the short period
	}

	@Override
	public boolean isTriggered(Candlestick[] candles, Candlestick[] btcCandles, int index) {
		double weightedAverage = Candlestick.weightedAverage(Arrays.copyOfRange(candles, index - (Trigger.ONE_DAY * 3) / getTimePeriod(), index + 1));
		for (int i = index - 1; i > index - ((Trigger.ONE_DAY * 3) / getTimePeriod()) - 1; i --) {
			if (Math.abs(Numerics.pc(weightedAverage, candles[i].weightedAverage)) >= 3.5F) {
				return false;
			}
		}
		return true;
	}

	protected void _calculateFeatures(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv,
			String ticker, boolean future) throws Exception {
		calculateDidPump(candles, btcCandles, index, fv, ticker, future, "didpump");
		calculateDidPumpImmediate(candles, btcCandles, index, fv, ticker, future, "didpumpimmediate");
	}

	@Override
	protected void _load(DataSet ds) throws Exception {  }

	@Override
	protected void _loadMax(DataSet ds) throws Exception {  }
	
	public DataSet loadDidPump() throws Exception {
		DataSet ds = loadTemplate();

		ds.markOutput("didpump");
		ds.markUnused("didpumpimmediate");
		markTradeFeaturesUnused(ds);
		return ds;
	}
	
	public DataSet loadDidPumpImmediate() throws Exception {
		DataSet ds = loadTemplate();

		ds.markOutput("didpumpimmediate");
		ds.markUnused("didpump");
		markTradeFeaturesUnused(ds);
		return ds;
	}
}
