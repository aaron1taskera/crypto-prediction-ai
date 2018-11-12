import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class PoloniexInterface implements ScrapeInterface {

	public ArrayList<String> getTickers() {
		ArrayList<String> tickers = new ArrayList<String>();
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
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
					data = reader.lines().collect(Collectors.joining("\n"));
					split = data.split("\":\\{\"id");
					for (int i = 0; i < split.length - 1; i ++) {
						ticker = i == 0 ? split[i].split("\"")[1] : split[i].split("},\"")[1];
						if (ticker.startsWith("BTC")) {
							tickers.add(ticker);
						}
					}
				}
				complete = true;
			} catch (Exception e) {
				try { Thread.sleep(Scrape.cooldown); } catch (Exception e2) {  }
			}
		}
		tickers.add("USDT_BTC");
		return tickers;
	}

	public String getName() { return "polo"; }

	public void scrapeTicker(long timePeriod, String ticker) {
		long firstTime = 0;
		long lastTime = 0;

		final String filePath = getName() + "/" + timePeriod + "/" + ticker + ".dat";

		URL url;
		URLConnection conn;
		String data = "";

		while (firstTime == 0 || lastTime == 0L) {
			try {
				url = new URL("https://poloniex.com/public?command=returnChartData&currencyPair=" + ticker + "&start=0&end=9999999999&period=86400");
				conn = url.openConnection();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
					data = reader.lines().collect(Collectors.joining("\n"));
					firstTime = Long.parseLong(data.split("\"date\":")[1].split(",")[0]);
					lastTime = Long.parseLong(data.split("\"date\":")[data.split("\"date\":").length - 2].split(",")[0]);
				} catch (Exception e2) { Thread.sleep(Scrape.cooldown); }
			} catch (Exception e) { try { Thread.sleep(Scrape.cooldown); } catch (Exception e2) {  } }
		}

		ArrayList<Candlestick> candles = new ArrayList<Candlestick>();

		int okay = 0;
		String[] split;
		Candlestick c;

		long interval = (timePeriod * 1344);

		long start = firstTime;
		long end = start + interval - timePeriod;
		System.out.println("Begin: " + ticker + " "+ start + " " + end + " " + lastTime);

		try {
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(filePath));
			do {
				boolean failed = false;
				while (okay == 0) {
					try {
						url = new URL("https://poloniex.com/public?command=returnChartData&currencyPair=" + ticker + "&start=" + start + "&end=" + end + "&period=" + timePeriod);
						conn = url.openConnection();
						try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
							data = reader.lines().collect(Collectors.joining("\n"));
							split = data.split("},");
							for (int x = 0; x < split.length; x ++) {
								c = new Candlestick();
								c.date = Long.parseLong(split[x].split("\"date\":")[1].split(",")[0]);
								if (c.date == 0) {
									failed = true;
									break;
								}
								if (c.date > lastTime) { break; }
								c.open = Double.parseDouble(split[x].split("\"open\":")[1].split(",")[0]);
								c.close = Double.parseDouble(split[x].split("\"close\":")[1].split(",")[0]);
								c.high = Double.parseDouble(split[x].split("\"high\":")[1].split(",")[0]);
								c.low = Double.parseDouble(split[x].split("\"low\":")[1].split(",")[0]);
								c.volume = Double.parseDouble(split[x].split("\"volume\":")[1].split(",")[0]);
								c.quoteVolume = Double.parseDouble(split[x].split("\"quoteVolume\":")[1].split(",")[0]);
								c.weightedAverage = Double.parseDouble(split[x].split("\"weightedAverage\":")[1].split("}")[0]);
								candles.add(c);
								if (x == split.length - 1) { end = c.date; }
							}
							okay ++;
						} catch (Exception e2) { e2.printStackTrace(); Thread.sleep(10000); }
					} catch (Exception e) { try { e.printStackTrace(); Thread.sleep(10000); } catch (Exception e2) {  } }
				}
				okay = 0;
				start = failed ? start : end + timePeriod;
				end = failed ? end + timePeriod : start + interval - timePeriod;
			} while (start < lastTime);

			for (Candlestick candle : candles) {
				candle.serialize(dos);
			}
			dos.close();
		} catch (Exception e) { e.printStackTrace(); }
	}

	public void scrape() {
		ArrayList<String> tickers = getTickers();
		new File(getName()).mkdir();
		long times[] = new long[] { 300, 900 };
		for (long time : times) {
			new File(getName() + "/" + time).mkdir();
			for (String ticker : tickers) {
				scrapeTicker(time, ticker);
			}
		}
	}
}