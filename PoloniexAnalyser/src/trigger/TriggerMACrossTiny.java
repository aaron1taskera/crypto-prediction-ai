package trigger;

import java.util.Arrays;

import csvutils.DataSet;
import csvutils.FeatureVector;
import stocks.Candlestick;
import stocks.Numerics;

public class TriggerMACrossTiny extends Trigger {

	public TriggerMACrossTiny() throws Exception { super("macross-tiny", Trigger.FIVE_MINUTES); }

	private static final int EMA_LENGTH = 30;
	private static final int SMA_LENGTH = 50;
	
	protected void addFeaturesToDataSet(DataSet set) throws Exception {
		addDefinedEndFeatures(set);
	}

	@Override
	public boolean isTriggered(Candlestick[] candles, Candlestick[] btcCandles, int index) {
		double lastEMA = Numerics.EMA(Candlestick.closePrices(Arrays.copyOfRange(candles, 0, index)), EMA_LENGTH);
		double curEMA = Numerics.EMA(Candlestick.closePrices(Arrays.copyOfRange(candles, 0, index + 1)), EMA_LENGTH);
		double lastSMA = Numerics.SMA(Candlestick.closePrices(Arrays.copyOfRange(candles, index - SMA_LENGTH, index)));
		double curSMA = Numerics.SMA(Candlestick.closePrices(Arrays.copyOfRange(candles, index + 1 - SMA_LENGTH, index + 1)));
		
		if (lastEMA < lastSMA && curEMA > curSMA) {
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
				double curEMA = Numerics.EMA(Candlestick.closePrices(Arrays.copyOfRange(candles, 0, sellsIndex + 1)), EMA_LENGTH);
				double curSMA = Numerics.SMA(Candlestick.closePrices(Arrays.copyOfRange(candles, sellsIndex + 1 - SMA_LENGTH, sellsIndex + 1)));
				if (curEMA < curSMA) {
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