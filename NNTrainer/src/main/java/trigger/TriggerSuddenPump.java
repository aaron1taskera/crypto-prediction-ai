package trigger;
import java.util.Arrays;

import csvutils.DataSet;
import csvutils.FeatureVector;
import stocks.Candlestick;
import stocks.Numerics;

public class TriggerSuddenPump extends Trigger {

	public TriggerSuddenPump() throws Exception { super("suddenpump", Trigger.HALF_HOUR); }

	public TriggerSuddenPump(String name) throws Exception { super(name, Trigger.HALF_HOUR); }

	protected void addFeaturesToDataSet(DataSet set) throws Exception {
		set.addFeature("percentageatthirty");               //the percentage after plus thirty minutes
		set.addFeature("percentageatsixty");               	//the percentage after plus sixty minutes
		set.addFeature("percentageatninety");               //the percentage after plus ninety minutes
		set.addFeature("percentageatonetwenty");            //the percentage after plus one twenty minutes
		set.addFeature("percentageatonefifty");             //the percentage after plus one fifty minutes
		set.addFeature("percentageatoneeighty");			//the percentage after plus one eighty minutes
		set.addFeature("minperiod");						//number of periods after buy signal that the stock opens with lowest
		set.addFeature("maxperiod");						//number of periods after buy signal that the stock opens with the highest
		set.addFeature("minperiodopen");					//the percentage change from the buy signal on the lowest opening before sell
		set.addFeature("maxperiodopen");					//the percentage change from the buy signal on the highest opening before sell
	}

	public boolean isTriggered(Candlestick[] candles, Candlestick[] btcCandles, int index) {
		if (Numerics.pc(candles[index].getOpen(), candles[index].getClose()) > Candlestick.PUMP_DELTA) {
			try {
				candles[index + 6].getOpen();
				return true;
			} catch (Exception e) { return false; }
		} else {
			return false;
		}
	}

	protected void _calculateFeatures(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv, String ticker, boolean future) throws Exception {
		if (future) {
			int sellsIndex = index + 6;
			double highBeforeSell = Candlestick.high(Arrays.copyOfRange(candles, index + 1, sellsIndex + 1));
			double lowBeforeSell = Candlestick.low(Arrays.copyOfRange(candles, index + 1, sellsIndex + 1));

			double highOpenIndex = Candlestick.highOpenIndex(Arrays.copyOfRange(candles, index + 1, sellsIndex + 1));
			double lowOpenIndex = Candlestick.lowOpenIndex(Arrays.copyOfRange(candles, index + 1, sellsIndex + 1));
			double highOpen = Candlestick.highOpen(Arrays.copyOfRange(candles, index + 1, sellsIndex + 1));
			double lowOpen = Candlestick.lowOpen(Arrays.copyOfRange(candles, index + 1, sellsIndex + 1));

			double purchaseApprox = candles[index + 1].getOpen();
			fv.setFeature("maxpercentage",                  Numerics.pc(purchaseApprox, highBeforeSell));
			fv.setFeature("percentageatthirty",				Numerics.pc(purchaseApprox, candles[index + 1].getClose()));
			fv.setFeature("percentageatsixty",				Numerics.pc(purchaseApprox, candles[index + 2].getClose()));
			fv.setFeature("percentageatninety",				Numerics.pc(purchaseApprox, candles[index + 3].getClose()));
			fv.setFeature("percentageatonetwenty",			Numerics.pc(purchaseApprox, candles[index + 4].getClose()));
			fv.setFeature("percentageatonefifty",			Numerics.pc(purchaseApprox, candles[index + 5].getClose()));
			fv.setFeature("percentageatoneeighty",			Numerics.pc(purchaseApprox, candles[index + 6].getClose()));
			fv.setFeature("minpercentage",    				Numerics.pc(purchaseApprox, lowBeforeSell));
			fv.setFeature("minperiod",						lowOpenIndex + 1);
			fv.setFeature("maxperiod", 						highOpenIndex + 1);
			fv.setFeature("minperiodopen",					Numerics.pc(purchaseApprox, lowOpen));
			fv.setFeature("maxperiodopen",					Numerics.pc(purchaseApprox, highOpen));
		} else {
			fv.setFeature("maxpercentage",                  0);
			fv.setFeature("percentageatthirty",             0);
			fv.setFeature("percentageatsixty",              0);
			fv.setFeature("percentageatninety",             0);
			fv.setFeature("percentageatonetwenty",          0);
			fv.setFeature("percentageatonefifty",           0);
			fv.setFeature("percentageatoneeighty",          0);
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
		ds.markOutput("percentageatoneeighty");

		ds.markUnused("percentageatthirty");
		ds.markUnused("percentageatsixty");
		ds.markUnused("percentageatninety");
		ds.markUnused("percentageatonetwenty");
		ds.markUnused("percentageatonefifty");

		ds.markUnused("minpercentage");
		ds.markUnused("minperiod");
		ds.markUnused("maxperiod");
		ds.markUnused("minperiodopen");
		ds.markUnused("maxperiodopen");
	}

	protected void _loadMax(DataSet ds) throws Exception {
		ds.markOutput("maxpercentage");
		ds.markUnused("percentageatoneeighty");

		ds.markUnused("percentageatthirty");
		ds.markUnused("percentageatsixty");
		ds.markUnused("percentageatninety");
		ds.markUnused("percentageatonetwenty");
		ds.markUnused("percentageatonefifty");

		ds.markUnused("minpercentage");
		ds.markUnused("minperiod");
		ds.markUnused("maxperiod");
		ds.markUnused("minperiodopen");
		ds.markUnused("maxperiodopen");
	}
}