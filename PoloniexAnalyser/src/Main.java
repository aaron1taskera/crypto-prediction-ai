import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import stocks.Candlestick;
import trigger.Trigger;
import trigger.TriggerIchimoku;
import trigger.TriggerIchimokuShort;
import trigger.TriggerMACDShort;
import trigger.TriggerMACDThirty;

public class Main {
	public static void main(String[] args) {
		try {
			/*processForStock(new TriggerMACross());
			processForStock(new TriggerMACrossShort());
			processForStock(new TriggerMACrossTiny());*/
			//processForStock(new TriggerIchimoku());
			//processForStock(new TriggerIchimokuShort());
			processForStock(new TriggerMACDThirty());
			//processForStock(new TriggerMACDShort());
			//processForStock(new TriggerIchimokuTiny());
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	public static void processForStock(Trigger t) throws Exception {
		System.out.println("Calculating for " + t.getName());
		File inputDir = new File("polo/" + t.getTimePeriod());
		processStocksInDir(inputDir, t);
	}

	public static void processStocksInDir(File inputDir, Trigger trigger) throws Exception {
		File output = new File(trigger.getFileName());
		ArrayList<Candlestick> btcCandles = Candlestick.loadFromFile(new File(inputDir.getPath() + "/USDT_BTC.dat"));
		ArrayList<Candlestick> candles;
		String ticker;
		trigger.getDataSet().outputHeadersToFile(output);
		for (File input : inputDir.listFiles()) {
			if (input.getName().startsWith("BTC")) {
				candles = Candlestick.loadFromFile(input);
				ticker = input.getName().substring(0, input.getName().lastIndexOf("."));
				System.out.println("Processing for: " + ticker);
				processStocks(candles.toArray(new Candlestick[candles.size()]), btcCandles.toArray(new Candlestick[btcCandles.size()]), trigger, ticker);
				trigger.getDataSet().flushToFile(output);
			}
		}
	}

	public static void processStocks(Candlestick[] candles, Candlestick[] btcCandles, Trigger trigger, String ticker) {
		if (candles.length == 0) { return; }
		long start = btcCandles[0].getDate();

		int candleStart = 0;
		int btcStart = 0;

		for (int i = 0; i < candles.length; i ++) {
			if (candles[i].getDate() >= start) {
				candleStart = i;
				break;
			}
		}

		for (int i = 0; i < btcCandles.length; i ++) {
			if (candles[candleStart].getDate() == btcCandles[i].getDate()) {
				btcStart = i;
				break;
			}
		}
		candles = Arrays.copyOfRange(candles, candleStart, candles.length);
		btcCandles = Arrays.copyOfRange(btcCandles, btcStart, btcCandles.length);

		int periods = Trigger.ONE_MONTH / trigger.getTimePeriod() + 50;
		try {
			double perc = 0;
			double lastPerc = 0;
			for (int i = periods; i < candles.length; i ++) {
				trigger.addIfTriggered(candles, btcCandles, i, ticker);
				perc = ((double) (i + 1 - periods) / (double) (candles.length - periods)) * 100D;
				if (perc % 10 < lastPerc) {
					System.out.println((int)Math.floor(perc) + "% " + Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":" + Calendar.getInstance().get(Calendar.MINUTE));
				}
				lastPerc = perc % 10;
			}
		} catch (Exception e) { e.printStackTrace(); }
	}
}