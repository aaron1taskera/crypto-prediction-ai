package trigger;

import csvutils.DataSet;
import csvutils.FeatureVector;
import stocks.Candlestick;

public class TriggerAll extends Trigger {
	
	private int iteration = 0;
	private static final int SKIP_DURATION = Trigger.FOUR_HOURS;

	public TriggerAll() throws Exception { super("all", Trigger.HALF_HOUR); }

	@Override
	protected void addFeaturesToDataSet(DataSet set) throws Exception {
		set.addFeature("didpump");                 	        //1 or 0 indicating whether this stock pumped within the period
		set.addFeature("didpumpimmediate");                 //1 or 0 indicating whether this stock pumped within the short period
		addDefinedEndFeatures(set);
	}

	@Override
	public boolean isTriggered(Candlestick[] candles, Candlestick[] btcCandles, int index) {
		/*if ((candles[index].getDate() % SKIP_DURATION / getTimePeriod())== (iteration % (SKIP_DURATION / getTimePeriod()))) {
			iteration += ((SKIP_DURATION / getTimePeriod()) - 1);
			if (Math.random() > 0.98) { iteration--; }
			return true;
		} else {
			return false;
		}*/
		return true;
	}

	protected void _calculateFeatures(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv,
			String ticker, boolean future) throws Exception {
		calculateDidPump(candles, btcCandles, index, fv, ticker, future, "didpump");
		calculateDidPumpImmediate(candles, btcCandles, index, fv, ticker, future, "didpumpimmediate");
		calculateDefinedEndFeatures(candles, btcCandles, index, fv, ticker, future, index + 6);
	}

	@Override
	protected void _load(DataSet ds) throws Exception {
		ds.markUnused("maxpercentage");
		ds.markOutput("percentageatsell");
		
		ds.markUnused("didpump");
		ds.markUnused("didpumpimmediate");

		ds.markUnused("minpercentage");
		ds.markUnused("minperiod");
		ds.markUnused("maxperiod");
		ds.markUnused("sellperiod");
		ds.markUnused("minperiodopen");
		ds.markUnused("maxperiodopen");

		markTradeFeaturesUnused(ds);
	}

	@Override
	protected void _loadMax(DataSet ds) throws Exception {
		ds.markOutput("maxpercentage");
		ds.markUnused("percentageatsell");

		ds.markUnused("didpump");
		ds.markUnused("didpumpimmediate");

		ds.markUnused("minpercentage");
		ds.markUnused("minperiod");
		ds.markUnused("maxperiod");
		ds.markUnused("sellperiod");
		ds.markUnused("minperiodopen");
		ds.markUnused("maxperiodopen");

		markTradeFeaturesUnused(ds);
	}
	
	public DataSet loadDidPump() throws Exception {
		DataSet ds = loadTemplate();

		ds.markOutput("didpump");
		ds.markUnused("didpumpimmediate");

		ds.markUnused("percentageatsell");
		ds.markUnused("maxpercentage");
		
		ds.markUnused("minpercentage");
		ds.markUnused("minperiod");
		ds.markUnused("maxperiod");
		ds.markUnused("sellperiod");
		ds.markUnused("minperiodopen");
		ds.markUnused("maxperiodopen");

		markTradeFeaturesUnused(ds);

		return ds;
	}
	
	public DataSet loadDidPumpImmediate() throws Exception {
		DataSet ds = loadTemplate();

		ds.markOutput("didpumpimmediate");
		ds.markUnused("didpump");

		ds.markUnused("percentageatsell");
		ds.markUnused("maxpercentage");
		
		ds.markUnused("minpercentage");
		ds.markUnused("minperiod");
		ds.markUnused("maxperiod");
		ds.markUnused("sellperiod");
		ds.markUnused("minperiodopen");
		ds.markUnused("maxperiodopen");

		markTradeFeaturesUnused(ds);

		return ds;
	}
}
