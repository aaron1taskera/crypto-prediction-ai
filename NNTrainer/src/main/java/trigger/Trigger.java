package trigger;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javax.print.attribute.standard.RequestingUserName;

import csvutils.DataSet;
import csvutils.FeatureVector;
import csvutils.Stat;
import stocks.Candlestick;
import stocks.Numerics;
import stocks.Trade;

//A trigger being an event that happens with the stocks. An example of a trigger would be a pump
public abstract class Trigger {

	private DataSet set;
	public static final int FIVE_MINUTES = 300;
	public static final int QUARTER_HOUR = 900;
	public static final int HALF_HOUR = 1800;
	public static final int ONE_HOUR = 3600;
	public static final int TWO_HOURS = 7200;
	public static final int FOUR_HOURS = 14400;
	public static final int SIX_HOURS = ONE_HOUR * 6;
	public static final int TWELVE_HOURS = ONE_HOUR * 12;
	public static final int ONE_DAY = 86400;
	public static final int TWO_DAYS = ONE_DAY * 2;
	public static final int THREE_DAYS = ONE_DAY * 3;
	public static final int FIVE_DAYS = ONE_DAY * 5;
	public static final int ONE_WEEK = ONE_DAY * 7;
	public static final int FORTNIGHT = ONE_WEEK * 2;
	public static final int ONE_MONTH = ONE_DAY * 28;

	private int timePeriod;
	private String name;
	private boolean calcBasic;

	public Trigger(String name, int timePeriod) throws Exception {
		this(name, timePeriod, true);
	}

	public Trigger(String name, int timePeriod, boolean calcBasic) throws Exception {
		this.name = name;
		this.timePeriod = timePeriod;
		this.calcBasic = calcBasic;
		set = genDataSet();
	}

	private DataSet genDataSet() throws Exception{
		DataSet set = new DataSet();
		addFeaturesToDataSet(set);
		if (calcBasic) {
			addBasicFeatures(set);
		}
		return set;
	}

	public String getName() { return name; }

	public String getFileName() { return "output-" + name + ".csv"; }

	public int getTimePeriod() { return timePeriod; }

	public DataSet getDataSet() { return set; }

	//adds a list of the features to be calculated to the DataSet
	protected abstract void addFeaturesToDataSet(DataSet set) throws Exception;

	//If triggered, adds to DataSet
	public boolean addIfTriggered(Candlestick[] candles, Candlestick[] btcCandles, int index, String ticker) throws Exception {
		boolean triggered = isTriggered(candles, btcCandles, index);
		FeatureVector fv;
		if (triggered) {
			fv = getDataSet().genBlankVector();
			try {
				calculateAllFeatures(candles, btcCandles, index, ticker, fv);
				getDataSet().insertVector(fv);
			} catch (ArrayIndexOutOfBoundsException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
		return triggered;
	}

	public double[] getInput(FeatureVector fv, String output, ArrayList<String> inputFeatures) throws Exception {
		DataSet set = genDataSet();
		set.insertVector(fv);
		set.markOutput(output);
		set.markUnusedUnless(output, inputFeatures);
		double[][] inputs = set.getInputs();
		double[] input = new double[inputs[0].length];
		for (int i = 0; i < input.length; i ++) {
			input[i] = inputs[0][i];
		}
		return input;
	}

	protected void calculateAllFeatures(Candlestick[] candles, Candlestick[] btcCandles, int index, String ticker, FeatureVector fv) throws Exception {
		if (calcBasic) {
			calculateBasicFeatures(candles, btcCandles, index, fv, ticker);
		}
		_calculateFeatures(candles, btcCandles, index, fv, ticker, true);
	}

	//Checks to see if this event has been triggered i.e if this is PumpInterface then it will return true if a pump has occurred.
	public abstract boolean isTriggered(Candlestick[] candles, Candlestick[] btcCandles, int index);

	public void calculatePastFeatures(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv, String ticker) throws Exception {
		calculateBasicFeatures(candles, btcCandles, index, fv, ticker);
		_calculateFeatures(candles, btcCandles, index, fv, ticker, false);
	}

	//Calculates the features
	protected abstract void _calculateFeatures(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv, String ticker, boolean future) throws Exception;

	public DataSet load() throws Exception { return load(getFileName()); }

	public DataSet loadTemplate() throws Exception { return loadTemplate(getFileName()); }

	public DataSet loadTemplate(String name) throws Exception {
		DataSet ds = DataSet.loadFromFile(new File(name));
		ds.markUnused("ticker");
		//ds.markUnused("timestamp");
		markOldTradeFeaturesUnused(ds);
		return ds;
	}

	public void markTradeFeaturesUnused(DataSet ds) throws Exception {
		ds.markUnused("ratiobsthirty");
		ds.markUnused("pbuyvolsamethirty");
		ds.markUnused("pmodalbuyvoltodayvol");
		ds.markUnused("psellvolsamethirty");
		ds.markUnused("pmodalsellvoltodayvol");
	}

	public void markOldTradeFeaturesUnused(DataSet ds) throws Exception {
		ds.markUnused("pbuyvolsamethirtymone");
		ds.markUnused("pmodalbuyvoltodayvolmone");
		ds.markUnused("psellvolsamethirtymone");
		ds.markUnused("pmodalsellvoltodayvolmone");
		ds.markUnused("ratiobsthirtymone");
		ds.markUnused("ratiobsthirtymtwo");
		ds.markUnused("ratiobsthirtymthree");
	}

	//Loads this trigger as a DataSet. The output is the percentage which the stock is sold for when the event is over	
	public DataSet load(String name) throws Exception {
		DataSet ds = loadTemplate(name);
		_load(ds);
		return ds;
	}

	public DataSet loadMax() throws Exception { return loadMax(getFileName()); }

	//Loads this trigger as a DataSet. The output is the max percentage the stock increases by before the event is over
	public DataSet loadMax(String name) throws Exception {
		DataSet ds = loadTemplate(name);
		_loadMax(ds);
		return ds;
	}

	//Marks the % at sell feature as output and others as unused
	protected abstract void _load(DataSet ds) throws Exception;

	//Marks the max feature as output and others as unused
	protected abstract void _loadMax(DataSet ds) throws Exception;

	protected void addDefinedEndFeatures(DataSet set) throws Exception {
		set.addFeature("percentageatsell");                 //the percentage on sell signal
		set.addFeature("maxpercentage");                    //the maximum percentage this stock reaches before the sell signal
		set.addFeature("minpercentage");					//the maximum percentage this stock drops before the sell signal
		set.addFeature("minperiod");						//number of periods after buy signal that the stock opens with lowest
		set.addFeature("maxperiod");						//number of periods after buy signal that the stock opens with the highest
		set.addFeature("sellperiod");						//number of periods after buy signal that sell signal occurs
		set.addFeature("minperiodopen");					//the percentage change from the buy signal on the lowest opening before sell
		set.addFeature("maxperiodopen");					//the percentage change from the buy signal on the highest opening before sell
	}

	protected void calculateDidPumpImmediate(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv, String ticker, boolean future) throws Exception {
		calculateDidPumpImmediate(candles, btcCandles, index, fv, ticker, future, "didpump");
	}

	protected void calculateDidPumpImmediate(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv, String ticker, boolean future, String feature) throws Exception {
		if (future) {
			boolean first = Numerics.pc(candles[index].open, Candlestick.high(Arrays.copyOfRange(candles, index + 1, index + 1 + getRatio(Trigger.TWO_HOURS, getTimePeriod())))) > 5;
			boolean second = Numerics.pc(candles[index].open, Candlestick.high(Arrays.copyOfRange(candles, index + 1, index + 1 + getRatio(Trigger.SIX_HOURS, getTimePeriod())))) > 15;
			double didPump = first && second ? 1D : -1D;
			fv.setFeature(feature, didPump);
		} else {
			fv.setFeature(feature, -1D);
		}
	}
	
	public static int getRatio(double trigger, double timePeriod) {
		return (int)Math.ceil(trigger / timePeriod);
	}

	protected void calculateDidPump(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv, String ticker, boolean future) throws Exception {
		calculateDidPump(candles, btcCandles, index, fv, ticker, future, "didpump");
	}

	protected void calculateDidPump(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv, String ticker, boolean future, String feature) throws Exception {
		if (future) {
			double didPump = Numerics.pc(candles[index].getOpen(), Candlestick.high(Arrays.copyOfRange(candles, index + 1, index + 1 + getRatio(Trigger.ONE_DAY * 3D, getTimePeriod())))) > 15 ? 1D : -1D;
			fv.setFeature(feature, didPump);
		} else {
			fv.setFeature(feature, -1D);
		}
	}

	protected void fillDefinedEndFeatures(FeatureVector fv) throws Exception {
		calculateDefinedEndFeatures(null, null, 0, null, null, false, 0);
	}
	
	protected void calculateFutureDefinedEndFeatures(Candlestick[] candles, Candlestick[] btcCandles, FeatureVector fv, int index, int sellsIndex) throws Exception {
		calculateDefinedEndFeatures(candles, btcCandles, index, fv, null, true, sellsIndex);
	}
	
	protected void calculateDefinedEndFeatures(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv, String ticker, boolean future, int sellsIndex) throws Exception {
		if (future) {
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

	//Adds a set of basic features used by all triggers
	private void addBasicFeatures(DataSet set) throws Exception {
		set.addFeature("timestamp");						//the timestamp of a stock
		set.addFeature("ticker");							//the stock ticker

		set.addFeature("pddailyavg");                       //percentage difference from the daily avg
		set.addFeature("pdthreedayavg");                    //percentage difference from the 3 day avg
		set.addFeature("pdweeklyavg");                      //percentage difference from the weekly avg
		set.addFeature("pdmonthlyavg");                     //percentage difference from the monthly avg

		set.addFeature("sdvolthirty");                      //number of standard deviations away from volume mean for past 3 days in the past 30 minutes
		set.addFeature("pvolonethirty");                    //percentage of daily volume in past 30 minutes
		set.addFeature("relativeinteresttwelve");			//the twelve hour volume divided by the previous twelve hour volume
		set.addFeature("relativeinterestday");				//the day volume divided by the previous day volume
		set.addFeature("relativeinterestthreeday");			//the three day volume divided by the previous three day volume
		set.addFeature("relativeinterestweek");				//the week volume divided by the previous week volume
		set.addFeature("relativeinterestfort");				//the fortnight volume divided by the previous fortnight volume

		set.addFeature("ratiopricedeltatovol");             //the ratio of the price delta to volume
		set.addFeature("ratiopricedeltatovolmone");         //the ratio of the price delta to volume
		set.addFeature("ratiopricedeltatovolmtwo");         //the ratio of the price delta to volume
		set.addFeature("ratiopricedeltatovolmthree");       //the ratio of the price delta to volume

		set.addFeature("ratiobsthirty");                    //the ratio of buy trades to sell trades in the past 30 minutes
		set.addFeature("pbuyvolsamethirty");                //modal percentage of buy volume for the same price in the past 30 minutes
		set.addFeature("pmodalbuyvoltodayvol");             //percentage of the modal buy volume to the volume past 24 hours in the past 30 minutes
		set.addFeature("psellvolsamethirty");               //modal percentage of sell volume for the same price in the past 30 minutes
		set.addFeature("pmodalsellvoltodayvol");            //percentage of the modal sell volume to the volume past 24 horus in the past 30 minutes

		set.addFeature("pcbtcthirty");                      //percentage change of bitcoin in the past 30 minutes
		set.addFeature("pcbtcthirtymone");                  //percentage change of bitcoin in the previous 30 minutes
		set.addFeature("pcbtcone");                         //percentage change of bitcoin in the previous hour
		set.addFeature("pcbtctwo");                         //percentage change of bitcoin in the past 2 hours
		set.addFeature("pcbtcsix");                         //percentage change of bitcoin in the past 6 hours
		set.addFeature("pcbtctwelve");                      //percentage change of bitcoin in the past 12 hours
		set.addFeature("pcbtctwofour");                     //percentage change of bitcoin in the past 24 hours
		set.addFeature("pcbtcthree");                       //percentage change of bitcoin in the past 3 days
		set.addFeature("pcbtcweek");                        //percentage change of bitcoin in the past week
		set.addFeature("pcbtcfort");                        //percentage change of bitcoin in the past fortnight
		set.addFeature("pcbtcmonth");                       //percentage change of bitcoin in the past month

		set.addFeature("pcthirty");                         //percentage increase in the past 30 minutes (diff open to close)
		set.addFeature("pcthirtymone");                     //percentage change of -1 30 minutes
		set.addFeature("pcthirtymtwo");                     //percentage change of -2 30 minutes
		set.addFeature("pcthirtymthree");                   //percentage change of -3 30 minutes

		set.addFeature("pbuyvolsamethirtymone");            //modal percentage of buy volume for the same price in -2 30 minutes
		set.addFeature("pmodalbuyvoltodayvolmone");         //percentage of the modal buy volume to the volume past 24 hours in -2 30 minutes
		set.addFeature("psellvolsamethirtymone");           //modal percentage of sell volume for the same price in -2 30 minutes
		set.addFeature("pmodalsellvoltodayvolmone");        //percentage of the modal sell volume to the volume past 24 hours in -2 30 minutes
		set.addFeature("ratiobsthirtymone");                //the ratio of buy trades to sell trades in past -2 30 minutes
		set.addFeature("ratiobsthirtymtwo");                //the ratio of buy trades to sell trades in past -3 30 minutes
		set.addFeature("ratiobsthirtymthree");              //the ratio of buy trades to sell trades in past -4 30 minutes

		set.addFeature("sdvolthirtymone");                  //number of standard deviations away from volume mean for past 3 days in -2 30 minutes
		set.addFeature("sdvolthirtymtwo");                  //number of standard deviations away from volume mean for past 3 days in -3 30 minutes
		set.addFeature("sdvolthirtymthree");                //number of standard deviations away from volume mean for past 3 days in -4 30 minutes
		set.addFeature("pvolonethirtymone");                //percentage of daily volume in past -2 30 minutes
		set.addFeature("pvolonethirtymtwo");                //percentage of daily volume in past -3 30 minutes
		set.addFeature("pvolonethirtymthree");              //percentage of daily volume in past -4 30 minutes
		set.addFeature("pvolonesixty");                     //percentage of the daily volume in the past 60 minutes
		set.addFeature("pvoloneonetwenty");                 //percentage of the daily volume in the past 2 hours
		set.addFeature("pvolonesix");                       //percentage of the daily volume in the past 6 hours
		set.addFeature("npumpfive");                        //number of pumps in the past 5 days
		set.addFeature("ndumpfive");                        //number of dumps in the past 5 days
		set.addFeature("pcone");                            //percentage change over hour
		set.addFeature("pctwo");                            //percentage change over two hours
		set.addFeature("pcsix");                            //percentage change over six hours
		set.addFeature("pctwelve");                         //percentage change over 12 hours
		set.addFeature("pctwofour");                        //percentage change over 1 day
		set.addFeature("pctwoday");                         //percentage change over 2 days
		set.addFeature("pcthreeday");                       //percentage change over 3 days
		set.addFeature("pcfiveday");                        //percentage change over 5 days
		set.addFeature("pcweek");                           //percentage change over 7 days
		set.addFeature("pcfort");                           //percentage change over 2 weeks
		set.addFeature("pcmonth");                          //percentage change over 1 month
		set.addFeature("pddh");                             //percentage difference of the close price of past 30 to the daily high
		set.addFeature("pddm");                             //percentage difference of the close price of past 30 to the daily mean
		set.addFeature("pddl");                             //percentage difference of the close price of past 30 to the daily low
		set.addFeature("pdthreedh");                        //percentage difference of the close price of past 30 to the 3 day high
		set.addFeature("pdthreedl");                        //percentage difference of the close price of past 30 to the 3 day low
		set.addFeature("pdwh");                             //percentage difference of the close price of past 30 to the weekly high
		set.addFeature("pdwl");                             //percentage difference of the close price of past 30 to the weekly low
		set.addFeature("pdfh");                             //percentage difference of the close price of past 30 to the fortnightly high
		set.addFeature("pdfl");                             //percentage difference of the close price of past 30 to the fortnightly low
		set.addFeature("pdmh");                             //percentage difference of the close price of past 30 to the monthly high
		set.addFeature("pdml");                             //percentage difference of the close price of past 30 to the monthly low
		set.addFeature("labtcpriceday");                    //ln of the average price against the bitcoin for the day
		set.addFeature("lvoltwofour");                      //ln of the volume for the day
		set.addFeature("labtcpriceonetofourdaymone");       //ln(average price against bitcoin day/average price against bitcoin 4 days (not including last))
		set.addFeature("shadowtobodythirty");               //(ratio of shadow to body) previous 30 minutes(CAN CAUSE DIVIDE BY ZERO SO MAX 5x)
		set.addFeature("shadowpositionthirty");             //position of the shadow +1 for all above, -1 for all below. Reverse if stock closed higher than opened (red bar) previous 30 minutes
		set.addFeature("shadowtobodythirtymone");           //(ratio of shadow to body) previous 30 minutes(CAN CAUSE DIVIDE BY ZERO SO MAX 5x)
		set.addFeature("shadowpositionthirtymone");         //position of the shadow +1 for all above, -1 for all below previous 30 minutes
		set.addFeature("shadowtobodythirtymtwo");           //(ratio of shadow to body) previous 30 minutes(CAN CAUSE DIVIDE BY ZERO SO MAX 5x)
		set.addFeature("shadowpositionthirtymtwo");         //position of the shadow +1 for all above, -1 for all below previous 30 minutes
		set.addFeature("shadowtobodythirtymthree");         //(ratio of shadow to body) previous 30 minutes(CAN CAUSE DIVIDE BY ZERO SO MAX 5x)
		set.addFeature("shadowpositionthirtymthree");       //position of the shadow +1 for all above, -1 for all below previous 30 minutes
		set.addFeature("stabsix");                          //stability (std deviation)/mean over past 6 hours
		set.addFeature("stabtwelve");                       //stability (std deviation)/mean over past 12 hours
		set.addFeature("stabone");                          //stability (std deviation)/mean over past day
		set.addFeature("stabthree");                        //stability (std deviation)/mean over past 3 days
		set.addFeature("stabweek");                         //stability (std deviation)/mean over past week
		set.addFeature("stabfort");                         //stability (std deviation)/mean over past 2 weeks
		set.addFeature("stabmonth");                        //stability (std deviation)/mean over past month
		set.addFeature("pdbitcointhirty");                  //percentage change difference between this stock and bitcoin previous 30 minutes
		set.addFeature("pdbitcoinsixty");                   //percentage change difference between this stock and bitcoin previous 60 minutes
		set.addFeature("pdbitcoinonetwenty");               //percentage change difference between this stock and bitcoin previous 120 minutes
		set.addFeature("pdbitcointwoforty");                //percentage change difference between this stock and bitcoin previous 240 minutes
		set.addFeature("pdbitcoinsix");                     //percentage change difference between this stock and bitcoin previous 6 hours
		set.addFeature("pdbitcointwelve");                  //percentage change difference between this stock and bitcoin previous 12 hours
		set.addFeature("pdbitcointwentyfour");              //percentage change difference between this stock and bitcoin previous day
		set.addFeature("pdbitcointhree");                   //percentage change difference between this stock and bitcoin previous 3 days
		set.addFeature("pdbitcoinweek");                    //percentage change difference between this stock and bitcoin previous week
		set.addFeature("pdbitcoinfort");                    //percentage change difference between this stock and bitcoin previous fortnight
		set.addFeature("pdbitcoinmonth");                   //percentage change difference between this stock and bitcoin previous month
		set.addFeature("waposthirty");                      //Position of the weighted average in the previous 30 mins
		set.addFeature("waposthirtymone");                  //Position of the weighted average in the -1 30 mins
		set.addFeature("waposthirtymtwo");                  //Position of the weighted average in the -2 30 mins
		set.addFeature("waposthirtymthree");                //Position of the weighted average in the -3 30 mins
		set.addFeature("aroonup");							//Aroon up in the previous 30 minutes
		set.addFeature("aroondown");						//Aroon down in the previous 30 mins
		set.addFeature("aroondelta");						//Difference between aroon up and aroon down
		set.addFeature("aroonupmone");						//Aroon up in the -1 30 mins
		set.addFeature("aroondownmone");					//Aroon down in the -1 30 mins
		set.addFeature("aroondeltamone");					//Difference between aroon up and aroon down -1 30 mins
		set.addFeature("aroonupdiffmone");					//Difference between aroon up previous 30 mins and -1 30 mins
		set.addFeature("aroondowndiffmone");				//Difference between aroon down previous 30 mins and -1 30 mins
		set.addFeature("aroondeltadiffmone");				//Difference between aroon delta previous 30 mins and -1 30 mins
		set.addFeature("aroonupmtwo");						//Aroon up in the -2 30 mins
		set.addFeature("aroondownmtwo");					//Aroon down in the -2 30 mins
		set.addFeature("aroondeltamtwo");					//Difference between aroon up and aroon down -2 30 mins
		set.addFeature("aroonupdiffmtwo");					//Difference between aroon up previous 30 mins and -2 30 mins
		set.addFeature("aroondowndiffmtwo");				//Difference between aroon down previous 30 mins and -2 30 mins
		set.addFeature("aroondeltadiffmtwo");				//Difference between aroon delta previous 30 mins and -2 30 mins
		set.addFeature("aroonupmthree");					//Aroon up in the -3 30 mins
		set.addFeature("aroondownmthree");					//Aroon down in the -3 30 mins
		set.addFeature("aroondeltamthree");					//Difference between aroon up and aroon down -3 30 mins
		set.addFeature("aroonupdiffmthree");				//Difference between aroon up previous 30 mins and -3 30 mins
		set.addFeature("aroondowndiffmthree");				//Difference between aroon down previous 30 mins and -3 30 mins
		set.addFeature("aroondeltadiffmthree");				//Difference between aroon delta previous 30 mins and -3 30 mins
		set.addFeature("rsi");								//RSI in previous 30 mins
		set.addFeature("stochrsi");							//stoch rsi in previous 30 mins
		set.addFeature("rsimone");							//rsi in -1 30 mins
		set.addFeature("stochrsimone");						//stoch rsi in -1 30 mins
		set.addFeature("rsidiffmone");						//diff rsi previous 30 mins and -1 30 mins
		set.addFeature("stochrsidiffmone");					//diff stoch rsi previous 30 mins and -1 30 mins
		set.addFeature("rsimtwo");							//rsi in -2 30 mins
		set.addFeature("stochrsimtwo");						//stoch rsi in -2 30 mins
		set.addFeature("rsidiffmtwo");						//diff rsi previous 30 mins and -2 30 mins
		set.addFeature("stochrsidiffmtwo");					//diff stoch rsi previous 30 mins and -2 30 mins
		set.addFeature("rsimthree");						//rsi in -3 30 mins
		set.addFeature("stochrsimthree");					//stoch rsi in -3 30 mins
		set.addFeature("rsidiffmthree");					//diff rsi previous 30 mins and -3 30 mins
		set.addFeature("stochrsidiffmthree");				//diff stoch rsi previous 30 mins and -3 30 mins
		set.addFeature("tmf");								//twiggs money flow in previous 30 mins
		set.addFeature("tmfmone");							//twiggs money flow in -1 30 mins
		set.addFeature("tmfdiffmone");						//diff tmf previous 30 mins and -1 30 mins
		set.addFeature("tmfmtwo");							//twiggs money flow in -2 30 mins
		set.addFeature("tmfdiffmtwo");						//diff tmf previous 30 mins and -2 30 mins
		set.addFeature("tmfmthree");						//twiggs money flow in -3 30 mins
		set.addFeature("tmfdiffmthree");					//diff tmf previous 30 mins and -3 30 mins

		set.addFeature("percoutsidebandtwo");				//percentage outside bollinger bands past 2 hours (percentage of total candlestick areas outside bands)
		set.addFeature("percoutsidebandsix");				//percentage outside bollinger bands past 6 hours
		set.addFeature("percoutsidebandtwelve");			//percentage outside bollinger bands past 12 hours
		set.addFeature("percoutsidebandday");				//percentage outside bollinger bands past 24 hours
		set.addFeature("percabovebandtwo");					//percentage above bollinger bands past 2 hours
		set.addFeature("percabovebandsix");					//percentage above bollinger bands past 6 hours
		set.addFeature("percabovebandtwelve");				//percentage above bollinger bands past 12 hours
		set.addFeature("percabovebandday");					//percentage above bollinger bands past 24 hours
		set.addFeature("percbelowbandtwo");					//percentage below bollinger bands past 2 hours
		set.addFeature("percbelowbandsix");					//percentage below bollinger bands past 6 hours
		set.addFeature("percbelowbandtwelve");				//percentage below bollinger bands past 12 hours
		set.addFeature("percbelowbandday");					//percentage below bollinger bands past 24 hours

		set.addFeature("standardiseddpo");					//the standardised dpo
		set.addFeature("standardiseddpomone");				//the standardised dpo 1 period before
		set.addFeature("standardiseddpomtwo");				//the standardised dpo 2 periods before
		set.addFeature("standardiseddpomthree");			//the standardised dpo 3 periods before
		set.addFeature("standardiseddpodiffmone");			//the diff between the standardised dpo mone and current
		set.addFeature("standardiseddpodiffmtwo");			//the diff between the standardised dpo mtwo and current
		set.addFeature("standardiseddpodiffmthree");		//the diff between the standardised dpo mtwo and current

		set.addFeature("rvi");								//relative vigor index
		set.addFeature("rvimone");							//relative vigor index 1 period before
		set.addFeature("rvimtwo");							//relative vigor index 2 periods before
		set.addFeature("rvimthree");						//relative vigor index 3 periods before
		set.addFeature("rvidiffmone");						//the diff between the rvi mone and current
		set.addFeature("rvidiffmtwo");						//the diff between the rvi mtwo and current
		set.addFeature("rvidiffmthree");					//the diff between the rvi mthree and current
		set.addFeature("rvihist");							//relative vigor index histogram
		set.addFeature("rvihistmone");						//relative vigor index histogram 1 period before
		set.addFeature("rvihistmtwo");						//relative vigor index histogram 2 periods before
		set.addFeature("rvihistmthree");					//relative vigor index histogram 3 periods before
		set.addFeature("rvihistdiffmone");					//the diff between the rvi hist mone and current
		set.addFeature("rvihistdiffmtwo");					//the diff between the rvi hist mone and current
		set.addFeature("rvihistdiffmthree");				//the diff between the rvi hist mone and current

		set.addFeature("stochastic");						//the stochastic oscillator for the current period
		set.addFeature("stochasticmone");					//the stochastic oscillator 1 period before
		set.addFeature("stochasticmtwo");					//the stochastic oscillator 2 periods before
		set.addFeature("stochasticmthree");					//the stochastic oscillator 3 periods before
		set.addFeature("stochasticdiffmone");				//the diff between stochastic oscillator mone and current
		set.addFeature("stochasticdiffmtwo");				//the diff between stochastic oscillator mtwo and current
		set.addFeature("stochasticdiffmthree");				//the diff between stochastic oscillator mthree and current
		set.addFeature("stochastichist");					//the stochastic oscillator histogram
		set.addFeature("stochastichistmone");				//the stochastic oscillator 1 period before
		set.addFeature("stochastichistmtwo");				//the stochastic oscillator 2 periods before
		set.addFeature("stochastichistmthree");				//the stochastic oscillator 3 periods before
		set.addFeature("stochastichistdiffmone");			//the diff between stochastic oscillator histogram mone and current
		set.addFeature("stochastichistdiffmtwo");			//the diff between stochastic oscillator histogram mtwo and current
		set.addFeature("stochastichistdiffmthree");			//the diff between stochastic oscillator histogram mthree and current

		set.addFeature("pmmcvolsix");						//The PMMC of the volume in the past six hours
		set.addFeature("pmmcvoltwelve");					//The PMMC of the volume in the past twelve hours
		set.addFeature("pmmcvolday");						//The PMMC of the volume in the past day
		set.addFeature("pmmcvolthreeday");					//The PMMC of the volume in the past three days
		set.addFeature("pmmcvolweek");						//The PMMC of the volume in the past week
		set.addFeature("pmmcvolfort");						//The PMMC of the volume in the past fortnight
		set.addFeature("pmmcvolmonth");						//The PMMC of the volume in the past month
		set.addFeature("pmmcpricesix");						//The PMMC of the price in the past six hours
		set.addFeature("pmmcpricetwelve");					//The PMMC of the price in the last twelve hours
		set.addFeature("pmmcpriceday");						//The PMMC of the price in the last day
		set.addFeature("pmmcpricethreeday");				//The PMMC of the price in the last three days
		set.addFeature("pmmcpriceweek");					//The PMMC of the price in the last week
		set.addFeature("pmmcpricefort");					//The PMMC of the price in the last fortnight
		set.addFeature("pmmcpricemonth");					//The PMMC of the price in the last month
		set.addFeature("pmmcdiffvolpricesix");				//The PMMC of the volume against the price in the past six hours
		set.addFeature("pmmcdiffvolpricetwelve");			//The PMMC of the volume against the price in the past twelve hours
		set.addFeature("pmmcdiffvolpriceday");				//The PMMC of the volume against the price in the past day
		set.addFeature("pmmcdiffvolpricethreeday");			//The PMMC of the volume against the price in the past three days
		set.addFeature("pmmcdiffvolpriceweek");				//The PMMC of the volume against the price in the past week
		set.addFeature("pmmcdiffvolpricefort");				//The PMMC of the volume against the price in the past fortnight
		set.addFeature("pmmcdiffvolpricemonth");			//The PMMC of the volume against the price in the past month
		set.addFeature("pmmctmfsix");						//The PMMC of the tmf in the past six hours
		set.addFeature("pmmctmftwelve");					//The PMMC of the tmf in the past twelve hours
		set.addFeature("pmmctmfday");						//The PMMC of the tmf in the past day
		set.addFeature("pmmctmfthreeday");					//The PMMC of the tmf in the past three days
		set.addFeature("pmmctmfweek");						//The PMMC of the tmf in the past week
		set.addFeature("pmmctmffort");						//The PMMC of the tmf in the past fortnight
		set.addFeature("pmmctmfmonth");						//The PMMC of the tmf in the past month
		set.addFeature("pmmcdifftmfpricesix");				//The PMMC of the tmf against the price in the past six hours
		set.addFeature("pmmcdifftmfpricetwelve");			//The PMMC of the tmf against the price in the past twelve hours
		set.addFeature("pmmcdifftmfpriceday");				//The PMMC of the tmf against the price in the past day
		set.addFeature("pmmcdifftmfpricethreeday");			//The PMMC of the tmf against the price in the past three days
		set.addFeature("pmmcdifftmfpriceweek");				//The PMMC of the tmf against the price in the past week
		set.addFeature("pmmcdifftmfpricefort");				//The PMMC of the tmf against the price in the past fortnight
		set.addFeature("pmmcdifftmfpricemonth");			//The PMMC of the tmf against the price in the past month
		
		set.addFeature("ichimoku");
		set.addFeature("ichimokumone");
		set.addFeature("ichimokumtwo");
		set.addFeature("pmmcichimokusix");
		set.addFeature("pmmcichimokutwelve");
		set.addFeature("pmmcichimokuday");
		set.addFeature("pmmcichimokuthreeday");
		set.addFeature("pmmcichimokuweek");
		set.addFeature("pmmcichimokufort");
		set.addFeature("pmmcichimokumonth");
	}

	public Trade[] thirty;
	public Trade[] thirtymone;
	public Trade[] thirtymtwo;
	public Trade[] thirtymthree;

	//Calculates a set of basic features used by all triggers
	private void calculateBasicFeatures(Candlestick[] candles, Candlestick[] btcCandles, int index, FeatureVector fv, String ticker) throws Exception {
		Candlestick[] pastCandles = Arrays.copyOfRange(candles, 0, index + 1);

		fv.setFeature("timestamp", candles[index].getDate());
		String tickerChar = "";
		for (int i = 0; i < ticker.length(); i ++) {
			tickerChar += (int)ticker.charAt(i) + "";
		}
		fv.setFeature("ticker", Double.parseDouble(tickerChar));
		
		fv.setFeature("pddailyavg",                     Numerics.pc(Candlestick.weightedAverage(Arrays.copyOfRange(candles, index - getRatio(Trigger.ONE_DAY, getTimePeriod()), index)), candles[index].getClose()));
		fv.setFeature("pdthreedayavg",                  Numerics.pc(Candlestick.weightedAverage(Arrays.copyOfRange(candles, index - getRatio(Trigger.THREE_DAYS, getTimePeriod()), index)), candles[index].getClose()));
		fv.setFeature("pdweeklyavg",                    Numerics.pc(Candlestick.weightedAverage(Arrays.copyOfRange(candles, index - getRatio(Trigger.ONE_WEEK, getTimePeriod()), index)), candles[index].getClose()));
		fv.setFeature("pdmonthlyavg",                   Numerics.pc(Candlestick.weightedAverage(Arrays.copyOfRange(candles, index - getRatio(Trigger.ONE_MONTH, getTimePeriod()), index)), candles[index].getClose()));

		Stat s = Candlestick.getVolStat(Arrays.copyOfRange(candles, index - getRatio(Trigger.THREE_DAYS, getTimePeriod()) / getTimePeriod(), index));
		if (s.std != 0) {
			fv.setFeature("sdvolthirty",                    (candles[index].getVolume() - s.mean) / s.std);
		} else {
			fv.setFeature("sdvolthirty",                    0);
		}
		double dailyVolume = Candlestick.getVolSum(Arrays.copyOfRange(candles, index - getRatio(Trigger.ONE_DAY, getTimePeriod()), index));
		fv.setFeature("pvolonethirty",                  Numerics.perc(candles[index].getVolume(), dailyVolume));
		fv.setFeature("relativeinteresttwelve", getRelativeVolume(candles, index, Trigger.TWELVE_HOURS));
		fv.setFeature("relativeinterestday", getRelativeVolume(candles, index, Trigger.ONE_DAY));
		fv.setFeature("relativeinterestthreeday", getRelativeVolume(candles, index, Trigger.THREE_DAYS));
		fv.setFeature("relativeinterestweek", getRelativeVolume(candles, index, Trigger.ONE_WEEK));
		fv.setFeature("relativeinterestfort", getRelativeVolume(candles, index, Trigger.FORTNIGHT));

		fv.setFeature("ratiopricedeltatovol", candles[index].volume == 0 ? 0 : (candles[index].getClose() - candles[index].getOpen()) / candles[index].volume);
		fv.setFeature("ratiopricedeltatovolmone", candles[index - 1].volume == 0 ? 0 :(candles[index - 1].getClose() - candles[index - 1].getOpen()) / candles[index - 1].volume);
		fv.setFeature("ratiopricedeltatovolmtwo", candles[index - 2].volume == 0 ? 0 :(candles[index - 2].getClose() - candles[index - 2].getOpen()) / candles[index - 2].volume);
		fv.setFeature("ratiopricedeltatovolmthree", candles[index - 3].volume == 0 ? 0 : (candles[index - 3].getClose() - candles[index - 3].getOpen()) / candles[index - 3].volume);

		if (candles[index].volume == 0) { fv.setFeature("ratiopricedeltatovol", 0); }
		if (candles[index - 1].volume == 0) { fv.setFeature("ratiopricedeltatovolmone", 0); }
		if (candles[index - 2].volume == 0) { fv.setFeature("ratiopricedeltatovolmtwo", 0); }
		if (candles[index - 3].volume == 0) { fv.setFeature("ratiopricedeltatovolmthree", 0); }


		//thirty = Trade.loadTrades(ticker, candles[index].getDate(), candles[index].getDate() + timePeriod);
		//thirtymone = Trade.loadTrades(ticker, candles[index - 1].getDate(), candles[index - 1].getDate() + timePeriod);
		/*thirtymtwo = Trade.loadTrades(ticker, candles[index - 2].getDate(), candles[index - 2].getDate() + timePeriod);
		thirtymthree = Trade.loadTrades(ticker, candles[index - 3].getDate(), candles[index - 3].getDate() + timePeriod);*/
		thirty = new Trade[0];
		thirtymone = new Trade[0];
		thirtymtwo = new Trade[0];
		thirtymthree = new Trade[0];

		fv.setFeature("ratiobsthirty",                  Numerics.lockRange(0, Trade.ratioBuySell(thirty), 100));
		fv.setFeature("pbuyvolsamethirty",              Trade.percentageBuySamePrice(thirty));
		fv.setFeature("pmodalbuyvoltodayvol",           Numerics.perc(Trade.modalBuyVol(thirty), dailyVolume));
		fv.setFeature("psellvolsamethirty",             Trade.percentageSellSamePrice(thirty));
		fv.setFeature("pmodalsellvoltodayvol",          Numerics.perc(Trade.modalSellVol(thirty), dailyVolume));
		fv.setFeature("pbuyvolsamethirtymone",          Trade.percentageBuySamePrice(thirtymone));
		fv.setFeature("pmodalbuyvoltodayvolmone",       Numerics.perc(Trade.modalBuyVol(thirtymone), dailyVolume));
		fv.setFeature("psellvolsamethirtymone",         Trade.percentageBuySamePrice(thirtymone));
		fv.setFeature("pmodalsellvoltodayvolmone",      Numerics.perc(Trade.modalSellVol(thirtymone), dailyVolume));
		fv.setFeature("ratiobsthirtymone",              Trade.ratioBuySell(thirtymone));
		fv.setFeature("ratiobsthirtymtwo",              Trade.ratioBuySell(thirtymtwo));
		fv.setFeature("ratiobsthirtymthree",            Trade.ratioBuySell(thirtymthree));

		fv.setFeature("pcbtcthirty",                    btcCandles[index].delta());
		fv.setFeature("pcbtcthirtymone",                btcCandles[index - 1].delta());
		fv.setFeature("pcbtcone",                       Numerics.pc(btcCandles[index + 1 - getRatio(Trigger.ONE_HOUR, getTimePeriod())].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pcbtctwo",                       Numerics.pc(btcCandles[index + 1 - getRatio(Trigger.TWO_HOURS, getTimePeriod())].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pcbtcsix",                       Numerics.pc(btcCandles[index + 1 - getRatio(Trigger.SIX_HOURS, getTimePeriod())].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pcbtctwelve",                    Numerics.pc(btcCandles[index + 1 - getRatio(Trigger.TWELVE_HOURS, getTimePeriod())].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pcbtctwofour",                   Numerics.pc(btcCandles[index + 1 - Trigger.ONE_DAY / getTimePeriod()].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pcbtcthree",                     Numerics.pc(btcCandles[index + 1 - Trigger.THREE_DAYS / getTimePeriod()].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pcbtcweek",                      Numerics.pc(btcCandles[index + 1 - Trigger.ONE_WEEK / getTimePeriod()].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pcbtcfort",                      Numerics.pc(btcCandles[index + 1 - Trigger.FORTNIGHT / getTimePeriod()].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pcbtcmonth",                     Numerics.pc(btcCandles[index + 1 - Trigger.ONE_MONTH / getTimePeriod()].getOpen(), btcCandles[index].getClose()));

		fv.setFeature("pcthirty",                       candles[index].delta());
		fv.setFeature("pcthirtymone",                   candles[index - 1].delta());
		fv.setFeature("pcthirtymtwo",                   candles[index - 2].delta());
		fv.setFeature("pcthirtymthree",                 candles[index - 3].delta());

		if (s.std != 0) {
			fv.setFeature("sdvolthirtymone",                (candles[index - 1].getVolume() - s.mean) / s.std);
			fv.setFeature("sdvolthirtymtwo",                (candles[index - 2].getVolume() - s.mean) / s.std);
			fv.setFeature("sdvolthirtymthree",              (candles[index - 3].getVolume() - s.mean) / s.std);
		} else {
			fv.setFeature("sdvolthirtymone",                0);
			fv.setFeature("sdvolthirtymtwo",                0);
			fv.setFeature("sdvolthirtymthree",              0);
		}
		fv.setFeature("pvolonethirtymone",              Numerics.perc(candles[index - 1].getVolume(), dailyVolume));
		fv.setFeature("pvolonethirtymtwo",              Numerics.perc(candles[index - 2].getVolume(), dailyVolume));
		fv.setFeature("pvolonethirtymthree",            Numerics.perc(candles[index - 3].getVolume(), dailyVolume));
		fv.setFeature("pvolonesixty",                   Numerics.perc(Candlestick.getVolSum(Arrays.copyOfRange(candles, index + 1 - getRatio(Trigger.ONE_HOUR, getTimePeriod()), index + 1)), dailyVolume));
		fv.setFeature("pvoloneonetwenty",               Numerics.perc(Candlestick.getVolSum(Arrays.copyOfRange(candles, index + 1 - getRatio(Trigger.TWO_HOURS, getTimePeriod()), index + 1)), dailyVolume));
		fv.setFeature("pvolonesix",                     Numerics.perc(Candlestick.getVolSum(Arrays.copyOfRange(candles, index + 1 - getRatio(Trigger.SIX_HOURS, getTimePeriod()), index + 1)), dailyVolume));

		fv.setFeature("npumpfive",                      Candlestick.totalPumps(Arrays.copyOfRange(candles, index - Trigger.FIVE_DAYS / getTimePeriod(), index)));
		fv.setFeature("ndumpfive",                      Candlestick.totalDumps(Arrays.copyOfRange(candles, index - Trigger.FIVE_DAYS / getTimePeriod(), index)));
		fv.setFeature("pcone",                          Numerics.pc(candles[index + 1 - getRatio(Trigger.ONE_HOUR, getTimePeriod())].getOpen(), candles[index].getClose()));
		fv.setFeature("pctwo",                          Numerics.pc(candles[index + 1 - getRatio(Trigger.TWO_HOURS, getTimePeriod())].getOpen(), candles[index].getClose()));
		fv.setFeature("pcsix",                          Numerics.pc(candles[index + 1 - getRatio(Trigger.SIX_HOURS, getTimePeriod())].getOpen(), candles[index].getClose()));
		fv.setFeature("pctwelve",                       Numerics.pc(candles[index + 1 - getRatio(Trigger.TWELVE_HOURS, getTimePeriod())].getOpen(), candles[index].getClose()));
		fv.setFeature("pctwofour",                      Numerics.pc(candles[index + 1 - Trigger.ONE_DAY / getTimePeriod()].getOpen(), candles[index].getClose()));
		fv.setFeature("pctwoday",                       Numerics.pc(candles[index + 1 - Trigger.TWO_DAYS / getTimePeriod()].getOpen(), candles[index].getClose()));
		fv.setFeature("pcthreeday",                     Numerics.pc(candles[index + 1 - Trigger.THREE_DAYS / getTimePeriod()].getOpen(), candles[index].getClose()));
		fv.setFeature("pcfiveday",                      Numerics.pc(candles[index + 1 - Trigger.FIVE_DAYS / getTimePeriod()].getOpen(), candles[index].getClose()));
		fv.setFeature("pcweek",                         Numerics.pc(candles[index + 1 - Trigger.ONE_WEEK / getTimePeriod()].getOpen(), candles[index].getClose()));
		fv.setFeature("pcfort",                         Numerics.pc(candles[index + 1 - Trigger.FORTNIGHT / getTimePeriod()].getOpen(), candles[index].getClose()));
		fv.setFeature("pcmonth",                        Numerics.pc(candles[index + 1 - Trigger.ONE_MONTH / getTimePeriod()].getOpen(), candles[index].getClose()));

		double dailyHigh = Candlestick.high(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_DAY / getTimePeriod(), index + 1));
		fv.setFeature("pddh",                           Numerics.pc(dailyHigh, candles[index].getClose()));
		double dailyMean = Candlestick.weightedAverage(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_DAY / getTimePeriod(), index + 1));
		fv.setFeature("pddm",                           Numerics.pc(dailyMean, candles[index].getClose()));
		double dailyLow = Candlestick.low(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_DAY / getTimePeriod(), index + 1));
		fv.setFeature("pddl",                           Numerics.pc(dailyLow, candles[index].getClose()));
		double threeDayHigh = Candlestick.high(Arrays.copyOfRange(candles, index + 1 - Trigger.THREE_DAYS / getTimePeriod(), index + 1));
		fv.setFeature("pdthreedh",                      Numerics.pc(threeDayHigh, candles[index].getClose()));
		double threeDayLow = Candlestick.low(Arrays.copyOfRange(candles, index + 1 - Trigger.THREE_DAYS / getTimePeriod(), index + 1));
		fv.setFeature("pdthreedl",                      Numerics.pc(threeDayLow, candles[index].getClose()));
		double weeklyHigh = Candlestick.high(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_WEEK / getTimePeriod(), index + 1));
		fv.setFeature("pdwh",                           Numerics.pc(weeklyHigh, candles[index].getClose()));
		double weeklyLow = Candlestick.low(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_WEEK / getTimePeriod(), index + 1));
		fv.setFeature("pdwl",                           Numerics.pc(weeklyLow, candles[index].getClose()));
		double fortnightHigh = Candlestick.high(Arrays.copyOfRange(candles, index + 1 - Trigger.FORTNIGHT / getTimePeriod(), index + 1));
		fv.setFeature("pdfh",                           Numerics.pc(fortnightHigh, candles[index].getClose()));
		double fortnightLow = Candlestick.low(Arrays.copyOfRange(candles, index + 1 - Trigger.FORTNIGHT / getTimePeriod(), index + 1));
		fv.setFeature("pdfl",                           Numerics.pc(fortnightLow, candles[index].getClose()));
		double monthlyHigh = Candlestick.high(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_MONTH / getTimePeriod(), index + 1));
		fv.setFeature("pdmh",                           Numerics.pc(monthlyHigh, candles[index].getClose()));
		double monthlyLow = Candlestick.low(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_MONTH / getTimePeriod(), index + 1));
		fv.setFeature("pdml",                           Numerics.pc(monthlyLow, candles[index].getClose()));

		fv.setFeature("labtcpriceday",                  Math.log(Candlestick.weightedAverage(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_DAY / getTimePeriod(), index + 1))));
		fv.setFeature("lvoltwofour",                    Math.max(0, Math.log(dailyVolume)));
		fv.setFeature("labtcpriceonetofourdaymone",     Math.min(3, Math.log(Candlestick.weightedAverage(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_DAY / getTimePeriod(), index + 1)) / Candlestick.weightedAverage(Arrays.copyOfRange(candles, index - (Trigger.ONE_DAY / getTimePeriod()) * 5 + 1, index - (Trigger.ONE_DAY / getTimePeriod()) + 1)))));
		fv.setFeature("shadowtobodythirty",             candles[index].shadowToBody());
		fv.setFeature("shadowpositionthirty",           candles[index].shadowPosition());
		fv.setFeature("shadowtobodythirtymone",         candles[index-1].shadowToBody());
		fv.setFeature("shadowpositionthirtymone",       candles[index-1].shadowPosition());
		fv.setFeature("shadowtobodythirtymtwo",         candles[index-2].shadowToBody());
		fv.setFeature("shadowpositionthirtymtwo",       candles[index-2].shadowPosition());
		fv.setFeature("shadowtobodythirtymthree",       candles[index-3].shadowToBody());
		fv.setFeature("shadowpositionthirtymthree",     candles[index-3].shadowPosition());

		fv.setFeature("stabsix",                        Candlestick.stability(Arrays.copyOfRange(candles, index - getRatio(Trigger.SIX_HOURS, getTimePeriod()), index)));
		fv.setFeature("stabtwelve",                     Candlestick.stability(Arrays.copyOfRange(candles, index - getRatio(Trigger.TWELVE_HOURS, getTimePeriod()), index)));
		fv.setFeature("stabone",                        Candlestick.stability(Arrays.copyOfRange(candles, index - Trigger.ONE_DAY / getTimePeriod(), index)));
		fv.setFeature("stabthree",                      Candlestick.stability(Arrays.copyOfRange(candles, index - Trigger.THREE_DAYS / getTimePeriod(), index)));
		fv.setFeature("stabweek",                       Candlestick.stability(Arrays.copyOfRange(candles, index - Trigger.ONE_WEEK / getTimePeriod(), index)));
		fv.setFeature("stabfort",                       Candlestick.stability(Arrays.copyOfRange(candles, index - Trigger.FORTNIGHT / getTimePeriod(), index)));
		fv.setFeature("stabmonth",                      Candlestick.stability(Arrays.copyOfRange(candles, index - Trigger.ONE_MONTH / getTimePeriod(), index)));

		fv.setFeature("pdbitcointhirty",                Numerics.pc(candles[index + 1 - getRatio(Trigger.HALF_HOUR, getTimePeriod())].getOpen(), candles[index].getClose()) - Numerics.pc(btcCandles[index + 1 - getRatio(Trigger.HALF_HOUR, getTimePeriod())].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pdbitcoinsixty",                 Numerics.pc(candles[index + 1 - getRatio(Trigger.ONE_HOUR, getTimePeriod())].getOpen(), candles[index].getClose()) - Numerics.pc(btcCandles[index + 1 - getRatio(Trigger.ONE_HOUR, getTimePeriod())].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pdbitcoinonetwenty",             Numerics.pc(candles[index + 1 - getRatio(Trigger.TWO_HOURS, getTimePeriod())].getOpen(), candles[index].getClose()) - Numerics.pc(btcCandles[index + 1 - getRatio(Trigger.TWO_HOURS, getTimePeriod())].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pdbitcointwoforty",              Numerics.pc(candles[index + 1 - getRatio(Trigger.FOUR_HOURS, getTimePeriod())].getOpen(), candles[index].getClose()) - Numerics.pc(btcCandles[index + 1 - getRatio(Trigger.FOUR_HOURS, getTimePeriod())].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pdbitcoinsix",                   Numerics.pc(candles[index + 1 - getRatio(Trigger.SIX_HOURS, getTimePeriod())].getOpen(), candles[index].getClose()) - Numerics.pc(btcCandles[index + 1 - getRatio(Trigger.SIX_HOURS, getTimePeriod())].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pdbitcointwelve",                Numerics.pc(candles[index + 1 - getRatio(Trigger.TWELVE_HOURS, getTimePeriod())].getOpen(), candles[index].getClose()) - Numerics.pc(btcCandles[index + 1 - getRatio(Trigger.TWELVE_HOURS, getTimePeriod())].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pdbitcointwentyfour",            Numerics.pc(candles[index + 1 - Trigger.ONE_DAY / getTimePeriod()].getOpen(), candles[index].getClose()) - Numerics.pc(btcCandles[index + 1 - Trigger.ONE_DAY / getTimePeriod()].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pdbitcointhree",                 Numerics.pc(candles[index + 1 - Trigger.THREE_DAYS / getTimePeriod()].getOpen(), candles[index].getClose()) - Numerics.pc(btcCandles[index + 1 - Trigger.THREE_DAYS / getTimePeriod()].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pdbitcoinweek",                  Numerics.pc(candles[index + 1 - Trigger.ONE_WEEK / getTimePeriod()].getOpen(), candles[index].getClose()) - Numerics.pc(btcCandles[index + 1 - Trigger.ONE_WEEK / getTimePeriod()].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pdbitcoinfort",                  Numerics.pc(candles[index + 1 - Trigger.FORTNIGHT / getTimePeriod()].getOpen(), candles[index].getClose()) - Numerics.pc(btcCandles[index + 1 - Trigger.FORTNIGHT / getTimePeriod()].getOpen(), btcCandles[index].getClose()));
		fv.setFeature("pdbitcoinmonth",                 Numerics.pc(candles[index + 1 - Trigger.ONE_MONTH / getTimePeriod()].getOpen(), candles[index].getClose()) - Numerics.pc(btcCandles[index + 1 - Trigger.ONE_MONTH / getTimePeriod()].getOpen(), btcCandles[index].getClose()));

		fv.setFeature("waposthirty",                    candles[index].weightAveragePos());
		fv.setFeature("waposthirtymone",                candles[index - 1].weightAveragePos());
		fv.setFeature("waposthirtymtwo",                candles[index - 2].weightAveragePos());
		fv.setFeature("waposthirtymthree",              candles[index - 3].weightAveragePos());

		fv.setFeature("aroonup",                        Candlestick.calculateAroonUp(Arrays.copyOfRange(candles, 0, index + 1)));
		fv.setFeature("aroondown",                      Candlestick.calculateAroonDown(Arrays.copyOfRange(candles, 0, index + 1)));
		fv.setFeature("aroondelta",                     fv.getFeature("aroonup") - fv.getFeature("aroondown"));
		fv.setFeature("aroonupmone",                    Candlestick.calculateAroonUp(Arrays.copyOfRange(candles, 0, index)));
		fv.setFeature("aroondownmone",                  Candlestick.calculateAroonDown(Arrays.copyOfRange(candles, 0, index)));
		fv.setFeature("aroondeltamone",                 fv.getFeature("aroonupmone") - fv.getFeature("aroondownmone"));
		fv.setFeature("aroonupdiffmone",                fv.getFeature("aroonup") - fv.getFeature("aroonupmone"));
		fv.setFeature("aroondowndiffmone",              fv.getFeature("aroondown") - fv.getFeature("aroondownmone"));
		fv.setFeature("aroondeltadiffmone",             fv.getFeature("aroondelta") - fv.getFeature("aroondeltamone"));
		fv.setFeature("aroonupmtwo",                    Candlestick.calculateAroonUp(Arrays.copyOfRange(candles, 0, index - 1)));
		fv.setFeature("aroondownmtwo",                  Candlestick.calculateAroonDown(Arrays.copyOfRange(candles, 0, index - 1)));
		fv.setFeature("aroondeltamtwo",                 fv.getFeature("aroonupmtwo") - fv.getFeature("aroondownmtwo"));
		fv.setFeature("aroonupdiffmtwo",                fv.getFeature("aroonup") - fv.getFeature("aroonupmtwo"));
		fv.setFeature("aroondowndiffmtwo",              fv.getFeature("aroondown") - fv.getFeature("aroondownmtwo"));
		fv.setFeature("aroondeltadiffmtwo",             fv.getFeature("aroondelta") - fv.getFeature("aroondeltamtwo"));
		fv.setFeature("aroonupmthree",                  Candlestick.calculateAroonUp(Arrays.copyOfRange(candles, 0, index - 2)));
		fv.setFeature("aroondownmthree",                Candlestick.calculateAroonDown(Arrays.copyOfRange(candles, 0, index - 2)));
		fv.setFeature("aroondeltamthree",               fv.getFeature("aroonupmthree") - fv.getFeature("aroondownmthree"));
		fv.setFeature("aroonupdiffmthree",              fv.getFeature("aroonup") - fv.getFeature("aroonupmthree"));
		fv.setFeature("aroondowndiffmthree",            fv.getFeature("aroondown") - fv.getFeature("aroondownmthree"));
		fv.setFeature("aroondeltadiffmthree",           fv.getFeature("aroondelta") - fv.getFeature("aroondeltamthree"));

		fv.setFeature("rsi",                            Candlestick.calculateRSI(Arrays.copyOfRange(candles, 0, index + 1)));
		fv.setFeature("stochrsi",                       Candlestick.calculateStochRSI(Arrays.copyOfRange(candles, 0, index + 1)));
		fv.setFeature("rsimone",                        Candlestick.calculateRSI(Arrays.copyOfRange(candles, 0, index)));
		fv.setFeature("stochrsimone",                   Candlestick.calculateStochRSI(Arrays.copyOfRange(candles, 0, index)));
		fv.setFeature("rsidiffmone",                    fv.getFeature("rsi") - fv.getFeature("rsimone"));
		fv.setFeature("stochrsidiffmone",               fv.getFeature("stochrsi") - fv.getFeature("stochrsimone"));
		fv.setFeature("rsimtwo",                        Candlestick.calculateRSI(Arrays.copyOfRange(candles, 0, index - 1)));
		fv.setFeature("stochrsimtwo",                   Candlestick.calculateStochRSI(Arrays.copyOfRange(candles, 0, index - 1)));
		fv.setFeature("rsidiffmtwo",                    fv.getFeature("rsi") - fv.getFeature("rsimtwo"));
		fv.setFeature("stochrsidiffmtwo",               fv.getFeature("stochrsi") - fv.getFeature("stochrsimtwo"));
		fv.setFeature("rsimthree",                      Candlestick.calculateRSI(Arrays.copyOfRange(candles, 0, index - 2)));
		fv.setFeature("stochrsimthree",                 Candlestick.calculateStochRSI(Arrays.copyOfRange(candles, 0, index - 2)));
		fv.setFeature("rsidiffmthree",                  fv.getFeature("rsi") - fv.getFeature("rsimthree"));
		fv.setFeature("stochrsidiffmthree",             fv.getFeature("stochrsi") - fv.getFeature("stochrsimthree"));

		fv.setFeature("tmf",                            Candlestick.calculateTwiggsMoneyFlow(Arrays.copyOfRange(candles, 0, index + 1)));
		fv.setFeature("tmfmone",                        Candlestick.calculateTwiggsMoneyFlow(Arrays.copyOfRange(candles, 0, index)));
		fv.setFeature("tmfdiffmone",                    fv.getFeature("tmf") - fv.getFeature("tmfmone"));
		fv.setFeature("tmfmtwo",                        Candlestick.calculateTwiggsMoneyFlow(Arrays.copyOfRange(candles, 0, index - 1)));
		fv.setFeature("tmfdiffmtwo",                    fv.getFeature("tmf") - fv.getFeature("tmfmtwo"));
		fv.setFeature("tmfmthree",                      Candlestick.calculateTwiggsMoneyFlow(Arrays.copyOfRange(candles, 0, index - 2)));
		fv.setFeature("tmfdiffmthree",                  fv.getFeature("tmf") - fv.getFeature("tmfmthree"));

		fv.setFeature("percoutsidebandtwo",             Candlestick.percOutsideBollingerBand(pastCandles, getRatio(Trigger.TWO_HOURS, getTimePeriod())));
		fv.setFeature("percoutsidebandsix",             Candlestick.percOutsideBollingerBand(pastCandles, getRatio(Trigger.SIX_HOURS, getTimePeriod())));
		fv.setFeature("percoutsidebandtwelve",          Candlestick.percOutsideBollingerBand(pastCandles, getRatio(Trigger.TWELVE_HOURS, getTimePeriod())));
		fv.setFeature("percoutsidebandday",             Candlestick.percOutsideBollingerBand(pastCandles, Trigger.ONE_DAY / getTimePeriod()));
		fv.setFeature("percabovebandtwo",               Candlestick.percAboveBollingerBand(pastCandles, Trigger.TWO_HOURS / getTimePeriod()));
		fv.setFeature("percabovebandsix",               Candlestick.percAboveBollingerBand(pastCandles, Trigger.SIX_HOURS / getTimePeriod()));
		fv.setFeature("percabovebandtwelve",            Candlestick.percAboveBollingerBand(pastCandles, getRatio(Trigger.TWELVE_HOURS, getTimePeriod())));
		fv.setFeature("percabovebandday",               Candlestick.percAboveBollingerBand(pastCandles, Trigger.ONE_DAY / getTimePeriod()));
		fv.setFeature("percbelowbandtwo",               Candlestick.percBelowBollingerBand(pastCandles, Trigger.TWO_HOURS / getTimePeriod()));
		fv.setFeature("percbelowbandsix",               Candlestick.percBelowBollingerBand(pastCandles, Trigger.SIX_HOURS / getTimePeriod()));
		fv.setFeature("percbelowbandtwelve",            Candlestick.percBelowBollingerBand(pastCandles, getRatio(Trigger.TWELVE_HOURS, getTimePeriod())));
		fv.setFeature("percbelowbandday",               Candlestick.percBelowBollingerBand(pastCandles, Trigger.ONE_DAY / getTimePeriod()));

		fv.setFeature("standardiseddpo",				Candlestick.calculateStandardisedDPO(Arrays.copyOfRange(candles, 0, index + 1)));
		fv.setFeature("standardiseddpomone",			Candlestick.calculateStandardisedDPO(Arrays.copyOfRange(candles, 0, index)));
		fv.setFeature("standardiseddpomtwo",			Candlestick.calculateStandardisedDPO(Arrays.copyOfRange(candles, 0, index - 1)));
		fv.setFeature("standardiseddpomthree",			Candlestick.calculateStandardisedDPO(Arrays.copyOfRange(candles, 0, index - 2)));
		fv.setFeature("standardiseddpodiffmone",		fv.getFeature("standardiseddpo") - fv.getFeature("standardiseddpomone"));
		fv.setFeature("standardiseddpodiffmtwo",		fv.getFeature("standardiseddpo") - fv.getFeature("standardiseddpomtwo"));
		fv.setFeature("standardiseddpodiffmthree",		fv.getFeature("standardiseddpo") - fv.getFeature("standardiseddpomthree"));
		fv.setFeature("rvi",							Candlestick.calculateRVI(Arrays.copyOfRange(candles, 0, index + 1)));
		fv.setFeature("rvimone",						Candlestick.calculateRVI(Arrays.copyOfRange(candles, 0, index)));
		fv.setFeature("rvimtwo",						Candlestick.calculateRVI(Arrays.copyOfRange(candles, 0, index - 1)));
		fv.setFeature("rvimthree",						Candlestick.calculateRVI(Arrays.copyOfRange(candles, 0, index - 2)));
		fv.setFeature("rvidiffmone",					fv.getFeature("rvi") - fv.getFeature("rvimone"));
		fv.setFeature("rvidiffmtwo",					fv.getFeature("rvi") - fv.getFeature("rvimtwo"));
		fv.setFeature("rvidiffmthree",					fv.getFeature("rvi") - fv.getFeature("rvimthree"));
		fv.setFeature("rvihist",						Candlestick.calculateRVIHist(Arrays.copyOfRange(candles, 0, index + 1)));
		fv.setFeature("rvihistmone",					Candlestick.calculateRVIHist(Arrays.copyOfRange(candles, 0, index)));
		fv.setFeature("rvihistmtwo",					Candlestick.calculateRVIHist(Arrays.copyOfRange(candles, 0, index - 1)));
		fv.setFeature("rvihistmthree",					Candlestick.calculateRVIHist(Arrays.copyOfRange(candles, 0, index - 2)));
		fv.setFeature("rvihistdiffmone",				fv.getFeature("rvihist") - fv.getFeature("rvihistmone"));
		fv.setFeature("rvihistdiffmtwo",				fv.getFeature("rvihist") - fv.getFeature("rvihistmtwo"));
		fv.setFeature("rvihistdiffmthree",				fv.getFeature("rvihist") - fv.getFeature("rvihistmthree"));
		fv.setFeature("stochastic",						Candlestick.calculateStochastic(Arrays.copyOfRange(candles, 0, index + 1)));
		fv.setFeature("stochasticmone",					Candlestick.calculateStochastic(Arrays.copyOfRange(candles, 0, index)));
		fv.setFeature("stochasticmtwo",					Candlestick.calculateStochastic(Arrays.copyOfRange(candles, 0, index - 1)));
		fv.setFeature("stochasticmthree",				Candlestick.calculateStochastic(Arrays.copyOfRange(candles, 0, index - 2)));
		fv.setFeature("stochasticdiffmone",				fv.getFeature("stochastic") - fv.getFeature("stochasticmone"));
		fv.setFeature("stochasticdiffmtwo",				fv.getFeature("stochastic") - fv.getFeature("stochasticmtwo"));
		fv.setFeature("stochasticdiffmthree",			fv.getFeature("stochastic") - fv.getFeature("stochasticmthree"));
		fv.setFeature("stochastichist",					Candlestick.calculateStochasticHist(Arrays.copyOfRange(candles, 0, index + 1)));
		fv.setFeature("stochastichistmone",				Candlestick.calculateStochasticHist(Arrays.copyOfRange(candles, 0, index)));
		fv.setFeature("stochastichistmtwo",				Candlestick.calculateStochasticHist(Arrays.copyOfRange(candles, 0, index - 1)));
		fv.setFeature("stochastichistmthree",			Candlestick.calculateStochasticHist(Arrays.copyOfRange(candles, 0, index - 2)));
		fv.setFeature("stochastichistdiffmone",			fv.getFeature("stochastichist") - fv.getFeature("stochastichistmone"));
		fv.setFeature("stochastichistdiffmtwo",			fv.getFeature("stochastichist") - fv.getFeature("stochastichistmtwo"));
		fv.setFeature("stochastichistdiffmthree",		fv.getFeature("stochastichist") - fv.getFeature("stochastichistmthree"));

		double[] tempCandles;
		double[] tempCandles2;

		tempCandles = Candlestick.weightedAverages(Arrays.copyOfRange(candles, index - getRatio(Trigger.SIX_HOURS, getTimePeriod()), index));
		tempCandles2 = Candlestick.volumes(Arrays.copyOfRange(candles, index - getRatio(Trigger.SIX_HOURS, getTimePeriod()), index));
		fv.setFeature("pmmcpricesix", Numerics.pearsons(Numerics.generateTime(tempCandles), tempCandles));
		fv.setFeature("pmmcvolsix", Numerics.pearsons(Numerics.generateTime(tempCandles2), tempCandles2));
		fv.setFeature("pmmcdiffvolpricesix", fv.getFeature("pmmcvolsix") - fv.getFeature("pmmcpricesix"));
		tempCandles2 = Candlestick.calculateTMFGraph(pastCandles, getRatio(Trigger.SIX_HOURS, getTimePeriod()));
		fv.setFeature("pmmctmfsix", Numerics.pearsons(Numerics.generateTime(tempCandles2), tempCandles2));
		fv.setFeature("pmmcdifftmfpricesix", fv.getFeature("pmmctmfsix") - fv.getFeature("pmmcpricesix"));

		tempCandles = Candlestick.weightedAverages(Arrays.copyOfRange(candles, index - getRatio(Trigger.TWELVE_HOURS, getTimePeriod()), index));
		tempCandles2 = Candlestick.volumes(Arrays.copyOfRange(candles, index - getRatio(Trigger.TWELVE_HOURS, getTimePeriod()), index));
		fv.setFeature("pmmcpricetwelve", Numerics.pearsons(Numerics.generateTime(tempCandles), tempCandles));
		fv.setFeature("pmmcvoltwelve", Numerics.pearsons(Numerics.generateTime(tempCandles2), tempCandles2));
		fv.setFeature("pmmcdiffvolpricetwelve", fv.getFeature("pmmcvoltwelve") - fv.getFeature("pmmcpricetwelve"));
		tempCandles2 = Candlestick.calculateTMFGraph(pastCandles, getRatio(Trigger.TWELVE_HOURS, getTimePeriod()));
		fv.setFeature("pmmctmftwelve", Numerics.pearsons(Numerics.generateTime(tempCandles2), tempCandles2));
		fv.setFeature("pmmcdifftmfpricetwelve", fv.getFeature("pmmctmftwelve") - fv.getFeature("pmmcpricetwelve"));

		tempCandles = Candlestick.weightedAverages(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_DAY / getTimePeriod(), index + 1));
		tempCandles2 = Candlestick.volumes(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_DAY / getTimePeriod(), index + 1));
		fv.setFeature("pmmcpriceday", Numerics.pearsons(Numerics.generateTime(tempCandles), tempCandles));
		fv.setFeature("pmmcvolday", Numerics.pearsons(Numerics.generateTime(tempCandles2), tempCandles2));
		fv.setFeature("pmmcdiffvolpriceday", fv.getFeature("pmmcvolday") - fv.getFeature("pmmcpriceday"));
		tempCandles2 = Candlestick.calculateTMFGraph(pastCandles, Trigger.ONE_DAY / getTimePeriod());
		fv.setFeature("pmmctmfday", Numerics.pearsons(Numerics.generateTime(tempCandles2), tempCandles2));
		fv.setFeature("pmmcdifftmfpriceday", fv.getFeature("pmmctmfday") - fv.getFeature("pmmcpriceday"));

		tempCandles = Candlestick.weightedAverages(Arrays.copyOfRange(candles, index + 1 - Trigger.THREE_DAYS / getTimePeriod(), index + 1));
		tempCandles2 = Candlestick.volumes(Arrays.copyOfRange(candles, index + 1 - Trigger.THREE_DAYS / getTimePeriod(), index + 1));
		fv.setFeature("pmmcpricethreeday", Numerics.pearsons(Numerics.generateTime(tempCandles), tempCandles));
		fv.setFeature("pmmcvolthreeday", Numerics.pearsons(Numerics.generateTime(tempCandles2), tempCandles2));
		fv.setFeature("pmmcdiffvolpricethreeday", fv.getFeature("pmmcvolthreeday") - fv.getFeature("pmmcpricethreeday"));
		tempCandles2 = Candlestick.calculateTMFGraph(pastCandles, Trigger.THREE_DAYS / getTimePeriod());
		fv.setFeature("pmmctmfthreeday", Numerics.pearsons(Numerics.generateTime(tempCandles2), tempCandles2));
		fv.setFeature("pmmcdifftmfpricethreeday", fv.getFeature("pmmctmfthreeday") - fv.getFeature("pmmcpricethreeday"));

		tempCandles = Candlestick.weightedAverages(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_WEEK / getTimePeriod(), index + 1));
		tempCandles2 = Candlestick.volumes(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_WEEK / getTimePeriod(), index + 1));
		fv.setFeature("pmmcpriceweek", Numerics.pearsons(Numerics.generateTime(tempCandles), tempCandles));
		fv.setFeature("pmmcvolweek", Numerics.pearsons(Numerics.generateTime(tempCandles2), tempCandles2));
		fv.setFeature("pmmcdiffvolpriceweek", fv.getFeature("pmmcvolweek") - fv.getFeature("pmmcpriceweek"));
		tempCandles2 = Candlestick.calculateTMFGraph(pastCandles, Trigger.ONE_WEEK / getTimePeriod());
		fv.setFeature("pmmctmfweek", Numerics.pearsons(Numerics.generateTime(tempCandles2), tempCandles2));
		fv.setFeature("pmmcdifftmfpriceweek", fv.getFeature("pmmctmfweek") - fv.getFeature("pmmcpriceweek"));

		tempCandles = Candlestick.weightedAverages(Arrays.copyOfRange(candles, index + 1 - Trigger.FORTNIGHT / getTimePeriod(), index + 1));
		tempCandles2 = Candlestick.volumes(Arrays.copyOfRange(candles, index + 1 - Trigger.FORTNIGHT / getTimePeriod(), index + 1));
		fv.setFeature("pmmcpricefort", Numerics.pearsons(Numerics.generateTime(tempCandles), tempCandles));
		fv.setFeature("pmmcvolfort", Numerics.pearsons(Numerics.generateTime(tempCandles2), tempCandles2));
		fv.setFeature("pmmcdiffvolpricefort", fv.getFeature("pmmcvolfort") - fv.getFeature("pmmcpricefort"));
		tempCandles2 = Candlestick.calculateTMFGraph(pastCandles, Trigger.FORTNIGHT / getTimePeriod());
		fv.setFeature("pmmctmffort", Numerics.pearsons(Numerics.generateTime(tempCandles2), tempCandles2));
		fv.setFeature("pmmcdifftmfpricefort", fv.getFeature("pmmctmffort") - fv.getFeature("pmmcpricefort"));

		tempCandles = Candlestick.weightedAverages(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_MONTH / getTimePeriod(), index + 1));
		tempCandles2 = Candlestick.volumes(Arrays.copyOfRange(candles, index + 1 - Trigger.ONE_MONTH / getTimePeriod(), index + 1));
		fv.setFeature("pmmcpricemonth", Numerics.pearsons(Numerics.generateTime(tempCandles), tempCandles));
		fv.setFeature("pmmcvolmonth", Numerics.pearsons(Numerics.generateTime(tempCandles2), tempCandles2));
		fv.setFeature("pmmcdiffvolpricemonth", fv.getFeature("pmmcvolmonth") - fv.getFeature("pmmcpricemonth"));
		tempCandles2 = Candlestick.calculateTMFGraph(pastCandles, Trigger.ONE_MONTH / getTimePeriod());
		fv.setFeature("pmmctmfmonth", Numerics.pearsons(Numerics.generateTime(tempCandles2), tempCandles2));
		fv.setFeature("pmmcdifftmfpricemonth", fv.getFeature("pmmctmfmonth") - fv.getFeature("pmmcpricemonth"));
		
		fv.setFeature("ichimoku", Candlestick.ichimokuSignal(pastCandles));
		fv.setFeature("ichimokumone", Candlestick.ichimokuSignal(Arrays.copyOfRange(pastCandles, 0, pastCandles.length - 1)));
		fv.setFeature("ichimokumtwo", Candlestick.ichimokuSignal(Arrays.copyOfRange(pastCandles, 0, pastCandles.length - 2)));

		tempCandles = Candlestick.calculateIchimokuSignalGraph(pastCandles, Trigger.SIX_HOURS / getTimePeriod());
		fv.setFeature("pmmcichimokusix", Numerics.pearsons(tempCandles, Numerics.generateTime(tempCandles)));
		tempCandles = Candlestick.calculateIchimokuSignalGraph(pastCandles, Trigger.TWELVE_HOURS / getTimePeriod());
		fv.setFeature("pmmcichimokutwelve", Numerics.pearsons(tempCandles, Numerics.generateTime(tempCandles)));
		tempCandles = Candlestick.calculateIchimokuSignalGraph(pastCandles, Trigger.ONE_DAY / getTimePeriod());
		fv.setFeature("pmmcichimokuday", Numerics.pearsons(tempCandles, Numerics.generateTime(tempCandles)));
		tempCandles = Candlestick.calculateIchimokuSignalGraph(pastCandles, Trigger.THREE_DAYS / getTimePeriod());
		fv.setFeature("pmmcichimokuthreeday", Numerics.pearsons(tempCandles, Numerics.generateTime(tempCandles)));
		tempCandles = Candlestick.calculateIchimokuSignalGraph(pastCandles, Trigger.ONE_WEEK / getTimePeriod());
		fv.setFeature("pmmcichimokuweek", Numerics.pearsons(tempCandles, Numerics.generateTime(tempCandles)));
		tempCandles = Candlestick.calculateIchimokuSignalGraph(pastCandles, Trigger.FORTNIGHT / getTimePeriod());
		fv.setFeature("pmmcichimokufort", Numerics.pearsons(tempCandles, Numerics.generateTime(tempCandles)));
		tempCandles = Candlestick.calculateIchimokuSignalGraph(pastCandles, Trigger.ONE_MONTH / getTimePeriod());
		fv.setFeature("pmmcichimokumonth", Numerics.pearsons(tempCandles, Numerics.generateTime(tempCandles)));
	}

	private double getRelativeVolume(Candlestick[] candles, int index, int trigger) {
		double volume = Candlestick.getVolSum(Arrays.copyOfRange(candles, index - getRatio(trigger, getTimePeriod()), index));
		double prevVolume = Candlestick.getVolSum(Arrays.copyOfRange(candles, index - getRatio((trigger * 2), getTimePeriod()), index - getRatio(trigger, getTimePeriod())));
		return prevVolume == 0 ? Math.min(100, volume) : volume / prevVolume;
	}
}