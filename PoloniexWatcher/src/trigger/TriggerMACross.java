package trigger;

import java.util.Arrays;

import csvutils.DataSet;
import csvutils.FeatureVector;
import stocks.Candlestick;
import stocks.Numerics;

public class TriggerMACross extends Trigger {

	public TriggerMACross() throws Exception { super("macross", Trigger.HALF_HOUR); }

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
				double lastEMA = Numerics.EMA(Candlestick.closePrices(Arrays.copyOfRange(candles, 0, sellsIndex)), EMA_LENGTH);
				double curEMA = Numerics.EMA(Candlestick.closePrices(Arrays.copyOfRange(candles, 0, sellsIndex + 1)), EMA_LENGTH);
				double lastSMA = Numerics.SMA(Candlestick.closePrices(Arrays.copyOfRange(candles, sellsIndex - SMA_LENGTH, sellsIndex)));
				double curSMA = Numerics.SMA(Candlestick.closePrices(Arrays.copyOfRange(candles, sellsIndex + 1 - SMA_LENGTH, sellsIndex + 1)));
				if (lastEMA > lastSMA && curEMA < curSMA) {
					break;
				}
			}

			double highBeforeSell = Candlestick.high(Arrays.copyOfRange(candles, index + 1, sellsIndex + 1));
			double lowBeforeSell = Candlestick.low(Arrays.copyOfRange(candles, index + 1, sellsIndex + 1));

			double highOpenIndex = Candlestick.highOpenIndex(Arrays.copyOfRange(candles, index + 1, sellsIndex + 1));
			double lowOpenIndex = Candlestick.lowOpenIndex(Arrays.copyOfRange(candles, index + 1, sellsIndex + 1));
			double highOpen = Candlestick.highOpen(Arrays.copyOfRange(candles, index + 1, sellsIndex + 1));
			double lowOpen = Candlestick.lowOpen(Arrays.copyOfRange(candles, index + 1, sellsIndex + 1));

			double sellApprox = candles[sellsIndex + 1].getOpen();
			double purchaseApprox = candles[index + 1].getOpen();

			fv.setFeature("maxpercentage",                  Numerics.pc(purchaseApprox, highBeforeSell));
			fv.setFeature("percentageatsell",               Numerics.pc(purchaseApprox, sellApprox));
			fv.setFeature("minpercentage",    				Numerics.pc(purchaseApprox, lowBeforeSell));
			fv.setFeature("minperiod",						lowOpenIndex + 1);
			fv.setFeature("maxperiod", 						highOpenIndex + 1);
			fv.setFeature("sellperiod",						sellsIndex - index + 1);
			fv.setFeature("minperiodopen",					Numerics.pc(purchaseApprox, lowOpen));
			fv.setFeature("maxperiodopen",					Numerics.pc(purchaseApprox, highOpen));
		} else {
			fv.setFeature("maxpercentage",                  0);
			fv.setFeature("percentageatsell",               0);
			fv.setFeature("minpercentage",    				0);
			fv.setFeature("minperiod",						0);
			fv.setFeature("maxperiod", 						0);
			fv.setFeature("sellperiod",						0);
			fv.setFeature("minperiodopen",					0);
			fv.setFeature("maxperiodopen",					0);
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