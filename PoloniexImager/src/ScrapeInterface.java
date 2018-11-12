import java.util.ArrayList;

public interface ScrapeInterface {
	public abstract void scrape();
	abstract String getName();
	abstract void scrapeTicker(long timePeriod, String ticker);
	abstract ArrayList<String> getTickers();
}
