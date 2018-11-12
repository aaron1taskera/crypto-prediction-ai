package trigger;

import java.util.Arrays;

import csvutils.DataSet;
import csvutils.FeatureVector;
import stocks.Candlestick;
import stocks.Numerics;
import stocks.Trade;

public class TriggerPriceSpike extends Trigger {

	public TriggerPriceSpike() throws Exception { super("price-spike", 900); }

	protected void addFeaturesToDataSet(DataSet set) throws Exception {
		set.addFeature("didpump");							//1 or 0 indicating whether or not this stock pumped
		set.addFeature("percentagebuyvolunproductive");     //the percentage of buy volume that is deemed 'unproductive' (the start of that order had the same rate as the previous order
		set.addFeature("percbuyvolunprodtotwodayvol");      //the percentage of buy volume that is deemed 'unproductive' (the start of that order had the same rate as the previous order
		set.addFeature("buytosellcountratio");              //the number of times buy is mentioned to sell
	}

	public boolean isTriggered(Candlestick[] candles, Candlestick[] btcCandles, int index) {
		for (int i = index - 1; i > index - ((Trigger.ONE_DAY * 1) / getTimePeriod()) - 1; i --) {
			if (Math.abs(candles[i].delta()) >= 2.5F) {
				return false;
			}
			if (candles[i].delta() >= candles[index].delta()) {
				return false;
			}
		}
		return candles[index].delta() > 0;
	}

	@Override
	protected void _calculateFeatures(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv,
			String ticker, boolean future) throws Exception {
		calculateDidPumpImmediate(candles, btcCandles, index, fv, ticker, future);

		double dailyVolume = Candlestick.getVolSum(Arrays.copyOfRange(candles, index - (Trigger.ONE_DAY * 2) / getTimePeriod(), index));

		fv.setFeature("percentagebuyvolunproductive", Numerics.perc(Trade.getUnproductiveBuyVolume(thirty, candles[index - 1].getClose()), candles[index].volume));
		fv.setFeature("percbuyvolunprodtotwodayvol", Numerics.perc(Trade.getUnproductiveBuyVolume(thirty, candles[index - 1].getClose()), dailyVolume));
		fv.setFeature("buytosellcountratio", Trade.buyToSellCount(thirty));
	}

	@Override
	protected void _load(DataSet ds) throws Exception {  }

	@Override
	protected void _loadMax(DataSet ds) throws Exception {  }

	public DataSet loadDidPump() throws Exception {
		DataSet ds = loadTemplate();

		ds.markOutput("didpump");
		return ds;
	}
}