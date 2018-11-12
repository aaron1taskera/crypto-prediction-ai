public class Scrape {
	public static final long cooldown = 5000;
	public static final long MONTH = 3600 * 24 * 28;

	public static void main(String[] args) {
		ScrapeInterface si;

		si = new PoloniexInterface();
		si.scrape();
	}
}