package trigger;

import java.util.Arrays;

import csvutils.DataSet;
import csvutils.FeatureVector;
import stocks.Candlestick;

public class TriggerIchimokuTiny extends Trigger {

	public TriggerIchimokuTiny() throws Exception { super("ichimoku-tiny", Trigger.FIVE_MINUTES); }
	
	protected void addFeaturesToDataSet(DataSet set) throws Exception {
		addDefinedEndFeatures(set);
	}

	@Override
	public boolean isTriggered(Candlestick[] candles, Candlestick[] btcCandles, int index) {
		double lastSignal = Candlestick.ichimokuSignal(Arrays.copyOfRange(candles, 0, index)/*, 20, 60*/);
		double curSignal = Candlestick.ichimokuSignal(Arrays.copyOfRange(candles, 0, index + 1)/*, 20, 60*/);
		
		if (lastSignal < 0 && curSignal > 0) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void _calculateFeatures(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv,
			String ticker, boolean future) throws Exception {
		if (future) {
			int sellsIndex = index;
			while (true) {
				sellsIndex++;
				double curSignal = Candlestick.ichimokuSignal(Arrays.copyOfRange(candles, 0, sellsIndex + 1)/*, 20, 60*/);
				if (curSignal < 0) {
					break;
				}
			}
			calculateFutureDefinedEndFeatures(candles, btcCandles, fv, index, sellsIndex);
		} else {
			fillDefinedEndFeatures(fv);
		}
	}

	protected void _load(DataSet ds) throws Exception {
		ds.markUnused("maxpercentage");
		ds.markOutput("percentageatsell");

		ds.markUnused("minpercentage");
		ds.markUnused("minperiod");
		ds.markUnused("maxperiod");
		ds.markUnused("sellperiod");
		ds.markUnused("minperiodopen");
		ds.markUnused("maxperiodopen");

		markTradeFeaturesUnused(ds);
	}

	protected void _loadMax(DataSet ds) throws Exception {
		ds.markOutput("maxpercentage");
		ds.markUnused("percentageatsell");

		ds.markUnused("minpercentage");
		ds.markUnused("minperiod");
		ds.markUnused("maxperiod");
		ds.markUnused("sellperiod");
		ds.markUnused("minperiodopen");
		ds.markUnused("maxperiodopen");

		markTradeFeaturesUnused(ds);
	}
}