package stocks;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

import trigger.Trigger;

//Represents a pairing between two currencies
public class Stock {
	public String ticker;
	private String exchange;

	public static final String POLONIEX = "poloniex";
	public static final String KUCOIN = "kucoin";
	public static final String BITFINEX = "bitfinex";
	public static final long BITFINEX_COOLDOWN = 65000;

	private static final long cooldown = 5000;

	public Stock(String ticker, String exchange) {
		prevCandles = new HashMap<Integer, ArrayList<Candlestick>>();
		this.ticker = ticker;
		this.exchange = exchange;
	}

	public static String getBTCTicker(String exchange) {
		if (exchange.equals(POLONIEX)) {
			return "USDT_BTC";
		} else if (exchange.equals(BITFINEX)) {
			return "btcusd";
		} else if (exchange.equals(KUCOIN)) {
			return "BTC-USDT";
		} else {
			return null;
		}
	}

	public Stock(String ticker) {
		this(ticker, POLONIEX);
	}

	//Gets all the pairings which are paired against the bitcoin
	public static Stock[] getStocks(String exchange) {
		ArrayList<Stock> stocks = new ArrayList<Stock>();
		if (exchange.equals(POLONIEX)) {
			getPoloniexStocks(stocks);
		} else if (exchange.equals(KUCOIN)) {
			getKucoinStocks(stocks);
		} else if (exchange.equals(BITFINEX)) {
			getBitfinexStocks(stocks);
		}
		return stocks.toArray(new Stock[stocks.size()]);
	}

	public static void getKucoinStocks(ArrayList<Stock> stocks) {
		boolean complete = false;
		while (!complete) {
			try {
				URL url;
				URLConnection conn;
				String data;
				String[] split;
				String ticker;

				url = new URL("https://api.kucoin.com/v1/market/open/symbols");
				conn = url.openConnection();
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
				data = reader.lines().collect(Collectors.joining("\n"));
				split = data.split("\"symbol\":");
				for (int i = 0; i < split.length - 1; i ++) {
					ticker = split[i].split("\"")[1];
					if (ticker.endsWith("BTC")) {
						stocks.add(new Stock(ticker, KUCOIN));
					}
				}
				reader.close();
				complete = true;
			} catch (Exception e) {
				try { Thread.sleep(Stock.cooldown); } catch (Exception e2) {  }
			}
		}
	}

	private static void getBitfinexStocks(ArrayList<Stock> stocks) {
		boolean complete = false;
		while (!complete) {
			try {
				URL url;
				URLConnection conn;
				String data;
				String[] split;
				String ticker;

				url = new URL("https://api.bitfinex.com/v1/symbols");
				conn = url.openConnection();
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
				data = reader.lines().collect(Collectors.joining("\n"));
				split = data.split("\\[")[1].split("]")[0].split(",");
				for (int i = 0; i < split.length - 1; i ++) {
					ticker = split[i].split("\"")[1];
					if (ticker.endsWith("btc")) {
						stocks.add(new Stock(ticker, BITFINEX));
					}
				}
				reader.close();
				complete = true;
			} catch (Exception e) {
				e.printStackTrace();
				try { Thread.sleep(Stock.cooldown); } catch (Exception e2) {  }
			}
		}
	}

	private static void getPoloniexStocks(ArrayList<Stock> stocks) {
		boolean complete = false;
		while (!complete) {
			try {
				URL url;
				URLConnection conn;
				String data;
				String[] split;
				String ticker;

				url = new URL("https://poloniex.com/public?command=returnTicker");
				conn = url.openConnection();
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
				data = reader.lines().collect(Collectors.joining("\n"));
				split = data.split("\":\\{\"id");
				for (int i = 0; i < split.length - 1; i ++) {
					ticker = i == 0 ? split[i].split("\"")[1] : split[i].split("},\"")[1];
					if (ticker.startsWith("BTC")) {
						stocks.add(new Stock(ticker, POLONIEX));
					}
				}
				reader.close();
				complete = true;
			} catch (Exception e) {
				try { Thread.sleep(Stock.cooldown); } catch (Exception e2) {  }
			}
		}
	}

	public HashMap<Integer, ArrayList<Candlestick>> prevCandles;

	public Candlestick[] getCandles(long start, long endTime, int timePeriod, int aggregate) {
		ArrayList<Candlestick> toAggregate = prevCandles.get(aggregate);
		ArrayList<Candlestick> aggregated = new ArrayList<Candlestick>();
		Candlestick c;
		for (int i = toAggregate.size() - 1; i >= (timePeriod / aggregate) - 1; i -= (timePeriod / aggregate)) {
			c = new Candlestick();
			c.date = 0;
			c.open = 0;
			c.close = toAggregate.get(i).close;
			c.high = 0;
			c.low = Double.MAX_VALUE;
			c.volume = 0;
			c.quoteVolume = 0;
			c.weightedAverage = 0;
			for (int x = 0; x < timePeriod / aggregate; x ++) {
				c.date = toAggregate.get(i - x).date;
				c.open = toAggregate.get(i - x).open;
				c.high = Math.max(c.high, toAggregate.get(i - x).high);
				c.low = Math.min(c.low, toAggregate.get(i - x).low);
				c.volume += toAggregate.get(i - x).volume;
				c.quoteVolume += toAggregate.get(i - x).quoteVolume;
				c.weightedAverage += toAggregate.get(i - x).weightedAverage * toAggregate.get(i - x).volume;
			}
			if (c.volume == 0) {
				c.weightedAverage = (c.open + c.close) / 2D;
			} else {
				c.weightedAverage /= c.volume;
			}
			aggregated.add(c);
		}
		Collections.reverse(aggregated);
		/*System.out.println("toagg");

		for (Candlestick can : toAggregate) {
			System.out.println(can.date + "\t" + can.open + "\t" + can.close + "\t" + can.high + "\t" + can.low + "\t" + can.volume + "\t" + can.quoteVolume + "\t" + can.weightedAverage);
		}
		System.out.println("sep");
		for (Candlestick can : aggregated) {
			System.out.println(can.date + "\t" + can.open + "\t" + can.close + "\t" + can.high + "\t" + can.low + "\t" + can.volume + "\t" + can.quoteVolume + "\t" + can.weightedAverage);
		}
		System.out.println();*/
		return aggregated.toArray(new Candlestick[aggregated.size()]);
	}

	public Candlestick[] getCandles(long start, long endTime, int timePeriod) throws Exception {
		ArrayList<Candlestick> lastCandles;
		long lastFirst;
		long lastLast;
		if (prevCandles.containsKey(timePeriod)) {
			lastCandles = prevCandles.get(timePeriod);
			lastFirst = lastCandles.get(0).getDate();
			lastLast = lastCandles.get(lastCandles.size() - 1).getDate();
			if (start == lastFirst && endTime == lastLast) {
				return lastCandles.toArray(new Candlestick[lastCandles.size()]);
			}
			if (start > lastFirst && start < lastLast) {
				while (lastCandles.get(0).getDate() < start) {
					lastCandles.remove(0);
				}
				start = lastCandles.get(lastCandles.size() - 1).getDate() + timePeriod;
			} else {
				lastCandles = new ArrayList<Candlestick>();
				prevCandles.put(timePeriod, lastCandles);
			}
		} else {
			lastCandles = new ArrayList<Candlestick>();
			prevCandles.put(timePeriod, lastCandles);
		}
		if (exchange.equals(POLONIEX)) {
			getCandlesPoloniex(lastCandles, start, endTime, timePeriod);
		} else if (exchange.equals(BITFINEX)) {
			getCandlesBitfinex(lastCandles, start, endTime, timePeriod);
		} else if (exchange.equals(KUCOIN)) {
			getCandlesKucoin(lastCandles, start, endTime, timePeriod);
		}
		return lastCandles.toArray(new Candlestick[lastCandles.size()]);
	}

	public static boolean isPeriodAvailable(String exchange, int timePeriod) {
		if (exchange.equals(POLONIEX)) {
			return true;
		} else if (exchange.equals(BITFINEX)) {
			if (timePeriod == Trigger.FIVE_MINUTES || timePeriod == Trigger.QUARTER_HOUR) {
				return true;
			}
		} else if (exchange.equals(KUCOIN)) {
			if (timePeriod == Trigger.FIVE_MINUTES || timePeriod == Trigger.QUARTER_HOUR || timePeriod == Trigger.HALF_HOUR) {
				return true;
			}
		}
		return false;
	}

	private void getCandlesBitfinex(ArrayList<Candlestick> lastCandles, long startTime, long endTime, long timePeriod) throws Exception {
		long tempStart = startTime;
		tempStart -= timePeriod;
		long tempEnd;
		while (tempStart <= endTime) {
			tempEnd = tempStart + 999 * timePeriod;
			getSubCandlesBitfinex(lastCandles, tempStart + timePeriod, Math.min(tempEnd, endTime), timePeriod);
			tempStart = tempEnd;
		}
	}

	private void getSubCandlesBitfinex(ArrayList<Candlestick> lastCandles, long startTime, long endTime, long timePeriod) throws Exception {
		startTime *= 1000;
		endTime *= 1000;
		Candlestick c;
		Candlestick fillC;
		int okay = 0;
		URL url;
		URLConnection conn;
		String data;
		String[] split;
		String[] subSplit;
		ArrayList<Candlestick> candles = new ArrayList<Candlestick>();
		long start = startTime;
		if (startTime == endTime) { start -= 1; }
		long end = endTime;
		String periodString = "";
		if (timePeriod == Trigger.FIVE_MINUTES) {
			periodString = "5m";
		} else if (timePeriod == Trigger.QUARTER_HOUR) {
			periodString = "15m";
		} else if (timePeriod == Trigger.HALF_HOUR) {
			periodString = "30m";
		}
		long limit = 1 + ((endTime - startTime) / (timePeriod * 1000));
		while (okay == 0) {
			try {
				url = new URL("https://api.bitfinex.com/v2/candles/trade:" + periodString + ":" + getBitfinexTicker() + "/hist?sort=1&start=" + start + "&end=" + end + "&limit=" + limit);
				conn = url.openConnection();
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
				data = reader.lines().collect(Collectors.joining("\n"));
				split = data.replace("[[", "").replace("]]", "").split("\\],\\[");
				for (int x = 0; x < split.length; x ++) {
					subSplit = split[x].split(",");
					c = new Candlestick();
					if (subSplit[0].equals("[]")) { continue; }
					c.date = Long.parseLong(subSplit[0]) / 1000;
					if (c.date > endTime / 1000) {
						break;
					}
					if (c.date < start / 1000) { continue; }
					c.open = Double.parseDouble(subSplit[1]);
					c.close = Double.parseDouble(subSplit[2]);
					c.high = Double.parseDouble(subSplit[3]);
					c.low = Double.parseDouble(subSplit[4]);
					c.quoteVolume = Double.parseDouble(subSplit[5]);
					c.weightedAverage = (c.open + c.close + c.high + c.low) / 4D;
					c.volume = c.quoteVolume * c.weightedAverage;
					candles.add(c);
				}
				reader.close();
				okay++;
			} catch (Exception e) { try { Thread.sleep(Stock.BITFINEX_COOLDOWN); } catch (Exception e2) {  } }
		}
		long expectedDate = startTime / 1000;
		if (String.valueOf(expectedDate).endsWith("1")) { expectedDate -= 1; }
		int index = 0;
		while (expectedDate <= endTime / 1000) {
			while ((index >= candles.size() || candles.get(index).date != expectedDate) && expectedDate <= endTime / 1000) {
				fillC = new Candlestick();
				fillC.date = expectedDate;
				fillC.open = getLastReplacement(index, Candlestick.openPrices(candles.toArray(new Candlestick[candles.size()])), Candlestick.openPrices(lastCandles.toArray(new Candlestick[lastCandles.size()])));
				fillC.close = fillC.open;
				fillC.high = fillC.open;
				fillC.close = fillC.open;
				fillC.low = fillC.open;
				fillC.volume = 0;
				fillC.quoteVolume = 0;
				fillC.weightedAverage = fillC.open;
				candles.add(index, fillC);
				expectedDate += timePeriod;
				index ++;
			}
			expectedDate += timePeriod;
			index ++;
		}
		lastCandles.addAll(candles);
		Thread.sleep(3000);
	}

	private String getBitfinexTicker() {
		return "t" + ticker.toUpperCase();
	}

	private void getCandlesKucoin(ArrayList<Candlestick> lastCandles, long start, long endTime, int timePeriod) throws Exception {
		Candlestick c;
		int okay = 0;
		URL url;
		URLConnection conn;
		String data;
		String[] opens;
		String[] closes;
		String[] timestamps;
		String[] highs;
		String[] lows;
		String[] volumes;
		ArrayList<Candlestick> candles = new ArrayList<Candlestick>();
		boolean wasTooNull = false;
		long end = endTime;
		String periodString = "";
		if (timePeriod == Trigger.FIVE_MINUTES) {
			periodString = "5";
		} else if (timePeriod == Trigger.QUARTER_HOUR) {
			periodString = "15";
		} else if (timePeriod == Trigger.HALF_HOUR) {
			periodString = "30";
		}
		while (okay == 0) {
			try {
				wasTooNull = false;
				url = new URL("https://api.kucoin.com/v1/open/chart/history?symbol=" + ticker + "&resolution" + periodString + "&from=" + start + "&to=" + end);
				conn = url.openConnection();
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
				data = reader.lines().collect(Collectors.joining("\n"));
				opens = data.split("\"o\":\\[")[1].split("\\]")[0].split(",");
				closes = data.split("\"c\":\\[")[1].split("\\]")[0].split(",");
				timestamps = data.split("\"t\":\\[")[1].split("\\]")[0].split(",");
				highs = data.split("\"h\":\\[")[1].split("\\]")[0].split(",");
				lows = data.split("\"l\":\\[")[1].split("\\]")[0].split(",");
				volumes = data.split("\"v\":\\[")[1].split("\\]")[0].split(",");
				for (int x = 0; x < timestamps.length; x ++) {
					c = new Candlestick();
					if (timestamps[x].equals("")) {
						System.out.println("https://api.kucoin.com/v1/open/chart/history?symbol=" + ticker + "&resolution" + periodString + "&from=" + start + "&to=" + end);
					}
					c.date = Long.parseLong(timestamps[x]);
					if (c.date > endTime) {
						break;
					}
					if (c.date < start) { continue; }
					wasTooNull = true;
					c.open = getNullReplacement(x, opens, Candlestick.openPrices(lastCandles.toArray(new Candlestick[lastCandles.size()])));
					c.close = closes[x].equals("null") ? c.open : Double.parseDouble(closes[x]);
					c.high = closes[x].equals("null") ? c.open : Double.parseDouble(highs[x]);
					c.low = closes[x].equals("null") ? c.open : Double.parseDouble(lows[x]);
					c.quoteVolume = Double.parseDouble(volumes[x]);
					c.weightedAverage = (c.open + c.close + c.high + c.low) / 4D;
					c.volume = c.weightedAverage * c.quoteVolume;
					candles.add(c);
				}
				reader.close();
				okay++;
			} catch (Exception e) {
				if (wasTooNull) {
					throw e;
				}
				try { e.printStackTrace(); Thread.sleep(Stock.cooldown); } catch (Exception e2) {  }
			}
		}
		lastCandles.addAll(candles);
	}

	private double getNullReplacement(int index, String[] lasts, double[] prior) throws Exception {
		if (lasts[index].equals("null")) {
			for (int i = index - 1; i >= 0; i --) {
				if (!lasts[i].equals("null")) {
					return Double.parseDouble(lasts[i]);
				}
			}

			if (prior.length > 0) {
				return prior[prior.length - 1];
			}

			for (int i = index + 1; i < lasts.length; i ++) {
				if (!lasts[i].equals("null")) {
					return Double.parseDouble(lasts[i]);
				}
			}

			throw new Exception("Could not replace null value from stock data");
		} else {
			return Double.parseDouble(lasts[index]);
		}
	}

	private double getLastReplacement(int index, double[] lasts, double[] prior) throws Exception {
		if (index > 0) {
			return lasts[index - 1];
		}

		if (prior.length > 0) {
			return prior[prior.length - 1];
		}

		if (index + 1 < lasts.length) {
			return lasts[index + 1];
		}

		throw new Exception("Could not replace null value from stock data");
	}

	private void getCandlesPoloniex(ArrayList<Candlestick> lastCandles, long start, long endTime, int timePeriod) {
		Candlestick c;
		int okay = 0;
		URL url;
		URLConnection conn;
		String data;
		String[] split;
		ArrayList<Candlestick> candles = new ArrayList<Candlestick>();
		long end = endTime;
		while (okay == 0) {
			try {
				url = new URL("https://poloniex.com/public?command=returnChartData&currencyPair=" + ticker + "&start=" + start + "&end=" + end + "&period=" + timePeriod);
				conn = url.openConnection();
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
				data = reader.lines().collect(Collectors.joining("\n"));
				split = data.split("},");
				for (int x = 0; x < split.length; x ++) {
					c = new Candlestick();
					c.date = Long.parseLong(split[x].split("\"date\":")[1].split(",")[0]);
					if (c.date == 0) {
						c.date = lastCandles.get(lastCandles.size() - 1).date + timePeriod;
						c.open = lastCandles.get(lastCandles.size() - 1).close;
						c.close = lastCandles.get(lastCandles.size() - 1).close;
						c.high = lastCandles.get(lastCandles.size() - 1).close;
						c.low = lastCandles.get(lastCandles.size() - 1).close;
						c.volume = 0;
						c.quoteVolume = 0;
						c.weightedAverage = lastCandles.get(lastCandles.size() - 1).weightedAverage;
						candles.add(c);
						break;
					}
					if (c.date > endTime) {
						break;
					}
					c.open = Double.parseDouble(split[x].split("\"open\":")[1].split(",")[0]);
					c.close = Double.parseDouble(split[x].split("\"close\":")[1].split(",")[0]);
					c.high = Double.parseDouble(split[x].split("\"high\":")[1].split(",")[0]);
					c.low = Double.parseDouble(split[x].split("\"low\":")[1].split(",")[0]);
					c.volume = Double.parseDouble(split[x].split("\"volume\":")[1].split(",")[0]);
					c.quoteVolume = Double.parseDouble(split[x].split("\"quoteVolume\":")[1].split(",")[0]);
					c.weightedAverage = Double.parseDouble(split[x].split("\"weightedAverage\":")[1].split("}")[0]);
					candles.add(c);
				}
				reader.close();
				if (candles.get(candles.size() - 1).getDate() != endTime) {
					Candlestick copyCandle = candles.get(candles.size() - 1);
					while (candles.get(candles.size() - 1).getDate() > endTime) { candles.remove(candles.size() - 1); }
					while(candles.get(candles.size() - 1).getDate() != endTime) {
						c = new Candlestick();
						c.date = copyCandle.date + timePeriod;
						c.open = copyCandle.close;
						c.close = copyCandle.close;
						c.high = copyCandle.close;
						c.low = copyCandle.close;
						c.volume = 0;
						c.quoteVolume = 0;
						c.weightedAverage = copyCandle.weightedAverage;
						copyCandle = c;
						candles.add(c);
					}
				}
				okay++;
			} catch (Exception e) { try {
				System.out.println("https://poloniex.com/public?command=returnChartData&currencyPair=" + ticker + "&start=" + start + "&end=" + end + "&period=" + timePeriod);
				e.printStackTrace(); Thread.sleep(Stock.cooldown);
			} catch (Exception e2) {  } }
		}
		lastCandles.addAll(candles);
	}
}