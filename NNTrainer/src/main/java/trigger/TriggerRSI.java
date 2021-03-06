package trigger;

import java.util.Arrays;

import csvutils.DataSet;
import csvutils.FeatureVector;
import stocks.Candlestick;
import stocks.Numerics;

public class TriggerRSI extends Trigger {

	public static final int RSI_PURCHASE = 30;
	public static final int RSI_SELL = 70;

	public TriggerRSI() throws Exception { super("rsi", Trigger.HALF_HOUR); }

	protected void addFeaturesToDataSet(DataSet set) throws Exception {
		addDefinedEndFeatures(set);
	}

	public boolean isTriggered(Candlestick[] candles, Candlestick[] btcCandles, int index) {
		double rsi = Candlestick.calculateRSI(Arrays.copyOfRange(candles, 0, index + 1));
		double lastRsi = Candlestick.calculateRSI(Arrays.copyOfRange(candles, 0, index));
		if (rsi > RSI_PURCHASE || lastRsi < RSI_PURCHASE) {
			return false;
		} else {
			return true;
		}
	}

	protected void _calculateFeatures(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv, String ticker, boolean future) throws Exception {
		if (future) {
			int sellsIndex = Candlestick.calculateRSINextSellsIndex(candles, index, RSI_SELL);

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