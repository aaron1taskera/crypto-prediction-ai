package stocks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Trade {
	double amount;
	double rate;
	boolean buy;
	long date;

	//Returns the total amount of this trade in whatever it is traded against
	public double getTotal() { return amount * rate; }

	
	public static Trade[] lastTrades;
	public static long lastFirst = 0L;
	public static long lastLast = 0L;
	//loads the trades that have occurred between two unix time stamps
	public static Trade[] loadTrades(String ticker, long first, long last) throws Exception {
		if (first == lastFirst && last == lastLast) { return lastTrades; }
		ArrayList<Trade> trades = new ArrayList<Trade>();
		long end = last + 2;
		String urlAddr = "https://poloniex.com/public?command=returnTradeHistory&currencyPair=" + ticker + "&start=" + first + "&end=" + end;
		int okay = 0;
		while (okay == 0) {
			try {
				URL url;
				URLConnection conn;
				String data;
				String[] split;

				url = new URL(urlAddr);
				conn = url.openConnection();
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
				data = reader.lines().collect(Collectors.joining("\n"));
				if (data.equals("[]")) { break; }
				split = data.split("},");
				for (int x = 0; x < split.length; x ++) {
					Trade t = new Trade();
					t.date = getUnixTime(split[x].split("\"date\":")[1].split(",")[0].split("\"")[1].split("\"")[0]);
					if (t.date > last) { continue; }
					t.amount = Double.parseDouble(split[x].split("\"amount\":")[1].split(",")[0].split("\"")[1].split("\"")[0]);
					t.rate = Double.parseDouble(split[x].split("\"rate\":")[1].split(",")[0].split("\"")[1].split("\"")[0]);
					t.buy = split[x].split("\"type\":")[1].split(",")[0].split("\"")[1].split("\"")[0].equals("buy");
					trades.add(t);
				}
				reader.close();
				okay = 1;
			} catch (Exception e) { Thread.sleep(5000); }
		}
		lastTrades = trades.toArray(new Trade[trades.size()]);
		lastFirst = first;
		lastLast = last;
		return lastTrades;
	}

	//Returns the percent that the modal trade volume is of the total volume for buy trades
	public static double percentageBuySamePrice(Trade[] trades) {
		return percentageSamePrice(trades, true);
	}
	
	public static double getUnproductiveBuyVolume(Trade[] trades, double lastClose) { return getUnproductiveVolume(trades, true, lastClose); }
	
	public static double getUnproductiveSellVolume(Trade[] trades, double lastClose) { return getUnproductiveVolume(trades, false, lastClose); }
	
	public static double getUnproductiveVolume(Trade[] trades, boolean buy, double lastClose) {
		double unproductive = 0;
		if (trades.length == 0) {
			return 0;
		}
		if (trades[0].rate == lastClose && trades[0].buy == buy) {
			unproductive += trades[0].getTotal();
		}
		for (int i = 1; i < trades.length; i ++) {
			if (trades[i].rate == trades[i-1].rate && trades[i].buy == buy) {
				unproductive += trades[i].getTotal();
			}
		}
		return unproductive;
	}
	
 	//Returns the number of times buy is mentioned to sell
	public static double buyToSellCount(Trade[] trades) {
		double buy = 0;
		double sell = 0;
		for (Trade t : trades) {
			if (t.buy) { buy ++; }
			else { sell ++; }
		}
		if (buy == 0D && sell == 0D) {
			return 1D;
		} else if (buy == 0D) {
			return Math.max(0.025D, 1D / sell);
		} else if (sell == 0D) {
			return Math.min(40D, buy);
		} else {
			return Numerics.lockRange(0.025, buy / sell, 40);
		}
	}

	//Returns the percent that the modal trade volume is of the total volume for sell trades
	public static double percentageSellSamePrice(Trade[] trades) {
		return percentageSamePrice(trades, false);
	}

	//Returns the percent that the modal trade volume is of the total volume (in a given direction, buy or sell)
	private static double percentageSamePrice(Trade[] trades, boolean buy) {
		if (trades.length == 0) { return 0D; }
		HashMap<Double, Double> amounts = new HashMap<Double, Double>();
		double total = 0D;
		for (Trade t : trades) {
			if (t.buy == buy) {
				total += t.amount * t.rate;
				if (amounts.containsKey(t.rate)) {
					amounts.put(t.rate, t.amount + amounts.get(t.rate));
				} else {
					amounts.put(t.rate, t.amount);
				}
			}
		}

		double rate = 0D;
		double amount = 0D;
		for (Map.Entry<Double, Double> entry : amounts.entrySet()) {
			if (rate == 0) {
				rate = entry.getKey();
				amount = entry.getValue();
			} else if (entry.getValue() > amount) {
				rate = entry.getKey();
				amount = entry.getValue();
			}
		}

		return total == 0 ? 0D : ((amount*rate) / total) * 100D;
	}

	//Returns the modal trade volume for buy trades
	public static double modalBuyVol(Trade[] trades) {
		return modalVol(trades, true);
	}

	//Returns the modal trade volume for sell trades
	public static double modalSellVol(Trade[] trades) {
		return modalVol(trades, false);
	}

	//Returns the modal trade volume (in a given direction, buy or sell)
	private static double modalVol(Trade[] trades, boolean buy) {
		HashMap<Double, Double> amounts = new HashMap<Double, Double>();
		for (Trade t : trades) {
			if (t.buy == buy) {
				if (amounts.containsKey(t.rate)) {
					amounts.put(t.rate, t.amount + amounts.get(t.rate));
				} else {
					amounts.put(t.rate, t.amount);
				}
			}
		}

		double rate = 0D;
		double amount = 0D;
		for (Map.Entry<Double, Double> entry : amounts.entrySet()) {
			if (rate == 0) {
				rate = entry.getKey();
				amount = entry.getValue();
			} else if (entry.getValue() > amount) {
				rate = entry.getKey();
				amount = entry.getValue();
			}
		}

		return amount * rate;
	}

	//Returns the ratio of volume purchased to volume sold
	public static double ratioBuySell(Trade[] trades) {
		double purchased = 0D;
		double sold = 0D;
		double amount;

		double buy = 0D;
		double sell = 0D;

		for (Trade t : trades) {
			amount = t.amount * t.rate;
			if (t.buy) {
				purchased += amount;
				buy ++;
			} else {
				sold += amount;
				sell ++;
			}
		}

		if (purchased == 0D && sold == 0D) {
			return 1D;
		} else if (purchased == 0D) {
			return Math.max(0.05D, 1D / sell);
		} else if (sold == 0D) {
			return Math.min(20D, buy);
		} else {
			return purchased/sold;
		}
	}

	//transforms a date given from poloniex to seconds from epoch time
	private static long getUnixTime(String date) throws Exception {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return Math.round(dateFormat.parse(date).getTime() / 1000D);
	}
}