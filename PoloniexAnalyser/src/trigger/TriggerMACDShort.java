package trigger;
import java.util.Arrays;

import csvutils.DataSet;
import csvutils.FeatureVector;
import stocks.Candlestick;
import stocks.Numerics;

public class TriggerMACDShort extends Trigger {

	public TriggerMACDShort() throws Exception { super("macd-short", Trigger.QUARTER_HOUR); }

	protected void addFeaturesToDataSet(DataSet set) throws Exception {
		addDefinedEndFeatures(set);

		set.addFeature("numsellpeak");                      //the number of sell indicators in the last peak
		set.addFeature("numbuystrough");                    //the number of buy indicators so far in this trough
		set.addFeature("pdlastsell");                       //the close difference from the buy to the last sell indicator
		set.addFeature("pdlastbuy");                        //the close difference from the buy to the last buy indicator (outside localised trough)
		set.addFeature("stanmacdpastbuy");                  //the standardised macd on the last peak buy indicator
		set.addFeature("stanmacdsell");                     //the standardised macd on the last peak sell indicator
		set.addFeature("stanmacd");                         //the standardised macd on the peak buy indicator
		set.addFeature("stanmacddiffpastbuy");              //the standardised macd difference between the peak and post buy indicator
		set.addFeature("pdpricemacdpastbuy");               //the percentage difference in the price between the peak and post buy prices
		set.addFeature("stanmacddiffsell");                 //the standardised macd difference between the peak and post buy indicator
		set.addFeature("pdpricemacdsell");                  //the percentage difference in the price between the peak and post buy prices
		set.addFeature("stanmacddiff");                     //the standardised macd difference between the peak and post buy indicator
	}

	public boolean isTriggered(Candlestick[] candles, Candlestick[] btcCandles, int index) {
		double curHist = Candlestick.calculateMACDHist(Arrays.copyOfRange(candles, 0, index + 1));
		if (curHist > 0) { return false; }
		double prevHist = Candlestick.calculateMACDHist(Arrays.copyOfRange(candles, 0, index));
		if (prevHist > 0 || prevHist > curHist) { return false; }
		double lastHist = Candlestick.calculateMACDHist(Arrays.copyOfRange(candles, 0, index - 1));
		if (curHist > prevHist && prevHist < lastHist &&
				curHist < 0 && prevHist < 0 && lastHist < 0) {
			try { //exception thrown if next histogram peak not inside array and as features cannot be calculated, false is returned
				return true;
			} catch (Exception e) { return false; }
		} else {
			return false;
		}
	}

	protected void _calculateFeatures(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv, String ticker, boolean future) throws Exception {
		int sellsIndex = !future ? 0 : Candlestick.calculateMACDHistNextSellsIndex(candles, index);
		calculateDefinedEndFeatures(candles, btcCandles, index, fv, ticker, future, sellsIndex);

		fv.setFeature("numsellpeak",                    Candlestick.calculateMACDHistSells(Arrays.copyOfRange(candles, 0, index)));
		fv.setFeature("numbuystrough",                  Candlestick.calculateMACDHistBuys(Arrays.copyOfRange(candles, 0, index)));

		int lastSellIndex = index - Candlestick.calculateMACDHistSellsOffset(Arrays.copyOfRange(candles, 0, index + 1));
		fv.setFeature("pdlastsell",                     Numerics.pc(candles[lastSellIndex + 1].getOpen(), candles[index].getClose()));
		fv.setFeature("stanmacdsell",                   Candlestick.calculateStanMACDHist(Arrays.copyOfRange(candles, 0, lastSellIndex + 1)));
		fv.setFeature("stanmacddiffsell",               fv.getFeature("stanmacdsell") - Candlestick.calculateStanMACDHist(Arrays.copyOfRange(candles, 0, lastSellIndex)));
		fv.setFeature("pdpricemacdsell",                Numerics.pc(candles[lastSellIndex - 1].getClose(), candles[lastSellIndex].getClose()));
		int lastBuyIndex = lastSellIndex - Candlestick.calculateMACDHistBuysOffset(Arrays.copyOfRange(candles, 0, lastSellIndex + 1));
		fv.setFeature("pdlastbuy",                      Numerics.pc(candles[lastBuyIndex + 1].getOpen(), candles[index].getClose()));
		fv.setFeature("stanmacdpastbuy",                Candlestick.calculateStanMACDHist(Arrays.copyOfRange(candles, 0, lastBuyIndex + 1)));
		fv.setFeature("stanmacd",                       Candlestick.calculateStanMACDHist(Arrays.copyOfRange(candles, 0, index + 1)));
		fv.setFeature("stanmacddiffpastbuy",            Candlestick.calculateStanMACDHist(Arrays.copyOfRange(candles, 0, lastBuyIndex)) - fv.getFeature("stanmacdpastbuy"));
		fv.setFeature("pdpricemacdpastbuy",             Numerics.pc(candles[lastBuyIndex - 1].getClose(), candles[lastBuyIndex].getClose()));
		fv.setFeature("stanmacddiff",                   fv.getFeature("stanmacd") - Candlestick.calculateStanMACDHist(Arrays.copyOfRange(candles, 0, index)));
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