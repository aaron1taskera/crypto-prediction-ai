import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import csvutils.FeatureVector;
import models.LogisticRegressionModel;
import models.PredictionModel;
import models.TrainedModel;
import stocks.Candlestick;
import stocks.Stock;
import trigger.Trigger;
import trigger.TriggerAll;
import trigger.TriggerAllLong;
import trigger.TriggerAllMedium;
import trigger.TriggerMACDThirty;
import trigger.TriggerPriceSpike;
import trigger.TriggerVolumeSpike;

public class Monitor {
	public static ArrayList<Entry<Trigger, PredictionModel>> trigModelMappings;

	public static void fillModels() throws Exception {
		trigModelMappings = new ArrayList<Entry<Trigger, PredictionModel>>();

		Trigger trig = new TriggerAll();
		PredictionModel pm = new TrainedModel(trig.getName() + "-accumulation");
		Entry<Trigger, PredictionModel> entry = new AbstractMap.SimpleEntry<Trigger, PredictionModel>(trig, pm);
		trigModelMappings.add(entry);

		trig = new TriggerAll();
		pm = new TrainedModel(trig.getName() + "-promotion");
		entry = new AbstractMap.SimpleEntry<Trigger, PredictionModel>(trig, pm);
		trigModelMappings.add(entry);

		trig = new TriggerAll();
		pm = new TrainedModel(trig.getName() + "-max");
		entry = new AbstractMap.SimpleEntry<Trigger, PredictionModel>(trig, pm);
		trigModelMappings.add(entry);

		trig = new TriggerMACDThirty();
		pm = new TrainedModel(trig.getName());
		entry = new AbstractMap.SimpleEntry<Trigger, PredictionModel>(trig, pm);
		trigModelMappings.add(entry);

		trig = new TriggerMACDThirty();
		pm = new TrainedModel(trig.getName() + "-max");
		entry = new AbstractMap.SimpleEntry<Trigger, PredictionModel>(trig, pm);
		trigModelMappings.add(entry);

		trig = new TriggerAllMedium();
		pm = new TrainedModel(trig.getName() + "-max");
		entry = new AbstractMap.SimpleEntry<Trigger, PredictionModel>(trig, pm);
		trigModelMappings.add(entry);

		trig = new TriggerAllLong();
		pm = new TrainedModel(trig.getName() + "-max");
		entry = new AbstractMap.SimpleEntry<Trigger, PredictionModel>(trig, pm);
		trigModelMappings.add(entry);

		trig = new TriggerAll();
		pm = new TrainedModel(trig.getName() + "-end");
		entry = new AbstractMap.SimpleEntry<Trigger, PredictionModel>(trig, pm);
		trigModelMappings.add(entry);

		trig = new TriggerAllMedium();
		pm = new TrainedModel(trig.getName() + "-end");
		entry = new AbstractMap.SimpleEntry<Trigger, PredictionModel>(trig, pm);
		trigModelMappings.add(entry);

		trig = new TriggerAllLong();
		pm = new TrainedModel(trig.getName() + "-end");
		entry = new AbstractMap.SimpleEntry<Trigger, PredictionModel>(trig, pm);
		trigModelMappings.add(entry);

		trig = new TriggerPriceSpike();
		pm = new LogisticRegressionModel(trig.getName());
		entry = new AbstractMap.SimpleEntry<Trigger, PredictionModel>(trig, pm);
		trigModelMappings.add(entry);

		trig = new TriggerVolumeSpike();
		pm = new TrainedModel(trig.getName());
		entry = new AbstractMap.SimpleEntry<Trigger, PredictionModel>(trig, pm);
		trigModelMappings.add(entry);
	}

	public static int getMinTime() {
		int min = Integer.MAX_VALUE;
		for (Entry <Trigger, PredictionModel> entry : trigModelMappings) {
			if (entry.getKey().getTimePeriod() < min) {
				min = entry.getKey().getTimePeriod();
			}
		}
		return min;
	}

	public static long getCurTime() {
		return Instant.now().getEpochSecond();
	}

	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm");

	public static String dir = "C:/Users/aaron/Dropbox/crypto-ai/scores/";

	public static void main(String[] args) {
		try {
			fillModels();
			(new Thread(new Runnable() {
				public void run() {
					try { outputPredictions(Stock.POLONIEX); } catch (Exception e) { e.printStackTrace(); }
				}
			})).start();
			(new Thread(new Runnable() {
				public void run() {
					try { outputPredictions(Stock.KUCOIN); } catch (Exception e) { e.printStackTrace(); }
				}
			})).start();
			(new Thread(new Runnable() {
				public void run() {
					try { outputPredictions(Stock.BITFINEX); } catch (Exception e) { e.printStackTrace(); }
				}
			})).start();
		} catch (Exception e) { e.printStackTrace(); }
	}

	public static void outputPredictions(String exchange) throws Exception {
		int min = getMinTime();
		Stock[] tickers = Stock.getStocks(exchange);
		Stock btc = new Stock(Stock.getBTCTicker(exchange), exchange);
		System.out.println("Now monitoring stocks on " + exchange);
		long nextNextTime = 0L;
		while (true) {
			long nextTime = Math.round(Math.ceil((double)getCurTime()/min)) * (long)min;
			if (nextTime > nextNextTime && nextNextTime != 0) {
				nextTime = nextNextTime;
				System.out.println("Backtracing on " + exchange);
			}
			System.out.println(getCurTime() + " " + nextTime + " " + min + " on " + exchange);
			while (getCurTime() < nextTime) {
				try { Thread.sleep(2000); } catch (Exception e) {  }
			}
			nextNextTime = nextTime + min;
			Candlestick[] stockCandles;
			FeatureVector fv;
			System.out.println("Starting checking " + getCurTime() + " on " + exchange);
			Candlestick[] btcCandles;
			Set<Integer> timePeriods = new HashSet<Integer>();
			Set<String> skip = new HashSet<String>();
			ArrayList<Stock> skipStocks = new ArrayList<Stock>();
			for (Entry <Trigger, PredictionModel> entry : trigModelMappings) {
				if (timePeriods.contains(entry.getKey().getTimePeriod())) { continue; }
				else { timePeriods.add(entry.getKey().getTimePeriod()); }
				if (nextTime % entry.getKey().getTimePeriod() == 0 && Stock.isPeriodAvailable(exchange, entry.getKey().getTimePeriod())) {
					System.out.println("Getting for time period " + entry.getKey().getTimePeriod() + " on " + exchange);
					long endTime = nextTime - (long)entry.getKey().getTimePeriod();
					long firstTime = nextTime - entry.getKey().getTimePeriod()*48*28*Trigger.getRatio(Trigger.HALF_HOUR, entry.getKey().getTimePeriod()) - entry.getKey().getTimePeriod() * (42 + 640 + 6);
					btc.getCandles(firstTime, endTime, entry.getKey().getTimePeriod());
					HashMap<Stock, Integer> stockAges = new HashMap<Stock, Integer>();
					HashMap<Integer, Integer> votes = new HashMap<Integer, Integer>();
					for (Stock s : tickers) {
						try {
							stockCandles = s.getCandles(firstTime, endTime, entry.getKey().getTimePeriod());
							stockAges.put(s, stockCandles.length);
							if (votes.containsKey(stockCandles.length)) {
								votes.put(stockCandles.length, votes.get(stockCandles.length) + 1);
							} else {
								votes.put(stockCandles.length, 1);
							}
						} catch (Exception e) { skip.add(s.ticker); }
					}
					int max = 0;
					int maxVote = 0;
					for (Entry<Integer, Integer> vote : votes.entrySet()) {
						if (vote.getValue() > maxVote) {
							max = vote.getKey();
							maxVote = vote.getValue();
						}
					}
					for (Entry<Stock, Integer> stockAge : stockAges.entrySet()) {
						if (stockAge.getValue() != max) {
							skipStocks.add(stockAge.getKey());
						}
					}
 				} else {
					System.out.println("Aggregating for time period " + entry.getKey().getTimePeriod() + " on " + exchange);
				}
			}
			for (Entry <Trigger, PredictionModel> entry : trigModelMappings) {
				System.out.println("Checking for model " + entry.getValue().getName() + " on " + exchange);
				boolean aggregate = false;
				if (nextTime % entry.getKey().getTimePeriod() != 0 || !Stock.isPeriodAvailable(exchange, entry.getKey().getTimePeriod())) { aggregate = true; }
				long endTime = nextTime - (long)entry.getKey().getTimePeriod();
				long firstTime = nextTime - entry.getKey().getTimePeriod()*48*28*Trigger.getRatio(Trigger.HALF_HOUR, entry.getKey().getTimePeriod()) - entry.getKey().getTimePeriod() * (42 + 640 + 6);
				if (aggregate) {
					btcCandles = btc.getCandles(firstTime, endTime, entry.getKey().getTimePeriod(), min);
				} else {
					btcCandles = btc.getCandles(firstTime, endTime, entry.getKey().getTimePeriod());
				}
				ArrayList<Entry<String, Double[]>> predictions = new ArrayList<Entry<String, Double[]>>();
				for (Stock s : tickers) {
					if (skipStocks.contains(s)) { continue; }
					if (skip.contains(s.ticker)) { continue; }
					if (aggregate) {
						stockCandles = s.getCandles(firstTime, endTime, entry.getKey().getTimePeriod(), min);
					} else {
						stockCandles = s.getCandles(firstTime, endTime, entry.getKey().getTimePeriod());
					}
					if (entry.getKey().isTriggered(stockCandles, btcCandles, stockCandles.length - 1)) {
						fv = entry.getKey().getDataSet().genBlankVector();
						entry.getKey().calculatePastFeatures(stockCandles, btcCandles, stockCandles.length - 1, fv, s.ticker);
						double[] input = entry.getKey().getInput(fv, entry.getValue().getOutputFeature(), entry.getValue().getInputFeatures());
						double prediction = entry.getValue().getTrainedPrediction(input);
						predictions.add(new AbstractMap.SimpleEntry<String, Double[]>(s.ticker, new Double[] { prediction, stockCandles[stockCandles.length - 1].close }));
						//System.out.println(entry.getKey().getName() + " trigger on " + s.ticker + " verdict " + prediction);
					}
				}
				outputScores(dir + exchange + "/" + entry.getValue().getName(), sdf.format(nextTime * 1000), predictions);
				pruneScores(dir + exchange + "/" + entry.getValue().getName());
			}
			System.out.println("Ending checking on " + exchange);
		}
	}

	public static void pruneScores(String dirName) throws Exception {
		int max = 288;
		String[] children = new File(dirName).list();
		while (children.length > max) {
			Date minDate = new Date();
			String minDateFile = "";
			for (String s : children) {
				if (sdf.parse(s.split(".csv")[0]).before(minDate)) {
					minDateFile = s;
					minDate = sdf.parse(s.split(".csv")[0]);
				}
			}
			new File(dirName + "/" + minDateFile).delete();
			children = new File(dirName).list();
		}
	}

	public static void copyScores(String dirName, String oldFileName, String fileName) throws Exception {
		File f = new File(dirName + "/" + oldFileName + ".csv");
		if (!new File(dirName).exists()) {
			new File(dirName).mkdir();
		}
		if (f.exists() && 1 == 2) {
			File f2 = new File(dirName + "/" + fileName + ".csv");
			Files.copy(f.toPath(), f2.toPath());
		} else {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(dirName + "/" + fileName + ".csv")));
			bw.write("ticker,score,current");
			bw.newLine();
			bw.close();
		}
	}

	public static void outputScores(String dirName, String fileName, ArrayList<Entry<String, Double[]>> predictions) throws Exception {
		if (!new File(dirName).exists()) {
			new File(dirName).mkdir();
		}
		DecimalFormat df = new DecimalFormat("#.####");
		df.setRoundingMode(RoundingMode.HALF_UP);

		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(dirName + "/" + fileName + ".csv")));
		bw.write("ticker,score,current");
		bw.newLine();
		for (Entry<String, Double[]> prediction : predictions) {
			bw.write(prediction.getKey() + "," + df.format(prediction.getValue()[0]) + "," + prediction.getValue()[1]);
			bw.newLine();
		}
		bw.close();
	}
}