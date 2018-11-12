public class Scrape {
	public static long timePeriod;

	public static final long cooldown = 5000;

	public static void main(String[] args) {
		timePeriod = Long.parseLong(args[0]);
		ScrapeInterface si;
		
		si = new PoloniexInterface();
		si.scrape();
	}
}