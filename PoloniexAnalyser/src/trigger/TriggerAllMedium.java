package trigger;

import csvutils.DataSet;
import csvutils.FeatureVector;
import stocks.Candlestick;

public class TriggerAllMedium extends Trigger {

	public TriggerAllMedium() throws Exception { super("all-medium", Trigger.TWO_HOURS); }

	@Override
	protected void addFeaturesToDataSet(DataSet set) throws Exception {
		set.addFeature("didpump");                 	        //1 or 0 indicating whether this stock pumped within the period
		set.addFeature("didpumpimmediate");                 //1 or 0 indicating whether this stock pumped within the short period
		addDefinedEndFeatures(set);
	}

	@Override
	public boolean isTriggered(Candlestick[] candles, Candlestick[] btcCandles, int index) {
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
