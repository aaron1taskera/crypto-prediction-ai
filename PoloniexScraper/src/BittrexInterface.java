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

public class BittrexInterface implements ScrapeInterface {

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

				url = new URL("https://bittrex.com/api/v1.1/public/getmarkets");
				conn = url.openConnection();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
					data = reader.lines().collect(Collectors.joining("\n"));
					split = data.split("},");
					for (int i = 0; i < split.length; i ++) {
						ticker = split[i].split("\"MarketName\":\"")[1].split("\"")[0];
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
		tickers.add("USDT-BTC");
		return tickers;
	}

	public String getName() { return "bittrex"; }

	public void scrapeTicker(long timePeriod, String ticker) {
		
	}

	public void scrape() {
		
	}
}