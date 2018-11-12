import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class Candlestick {
	long date;
	double high;
	double low;
	double open;
	double close;
	double volume;
	double quoteVolume;
	double weightedAverage;

	final static int chartWidth = 96; 	//How many periods is the chart for
	final static int chartHeight = 64;	//How many times is the chart being split vertically. Units of quantisation... Maybe?

	public void serialize(DataOutputStream dos) throws Exception {
		dos.writeLong(date);
		dos.writeDouble(high);
		dos.writeDouble(low);
		dos.writeDouble(open);
		dos.writeDouble(close);
		dos.writeDouble(volume);
		dos.writeDouble(quoteVolume);
		dos.writeDouble(weightedAverage);
	}

	public static void serialize(ArrayList<Candlestick> candles, DataOutputStream dos) throws Exception {
		for (int i = 0; i <= candles.size() - chartWidth - 3; i ++) {
			serializeChunk(arrayOfRange(candles, i, i + chartWidth + 3), dos);
		}
	}

	private static Candlestick[] arrayOfRange(ArrayList<Candlestick> candles, int start, int finish) {
		Candlestick[] array = new Candlestick[finish - start];
		for (int i = start; i < finish; i ++) {
			array[i - start] = candles.get(i);
		}
		return array;
	}

	private static void serializeChunk(Candlestick[] candles, DataOutputStream dos) throws Exception {
		Candlestick last = candles[candles.length - 4];
		double one = pc(last.close, candles[candles.length - 3].close);
		double two = pc(last.close, candles[candles.length - 2].close);
		double three = pc(last.close, candles[candles.length - 1].close);
		candles = Arrays.copyOfRange(candles, 0, candles.length - 3);
		double high = high(candles);
		double low = low(candles);
		double range = high - low;
		double chartHigh = high + range * 0.1D;
		double chartLow = low - range * 0.1D;
		double[][] chart = plotCandles(candles, chartHigh, chartLow);
		dos.writeLong(last.date);
		dos.writeDouble(one);
		dos.writeDouble(two);
		dos.writeDouble(three);
		serializeChart(chart, dos);
	}

	private static void serializeChart(double[][] chart, DataOutputStream dos) throws Exception {
		for (int i = 0; i < chart.length; i ++) {
			for (int i2 = 0; i2 < chart[i].length; i2 ++) {
				dos.writeDouble(chart[i][i2]);
			}
		}
	}

	private static void outputChart(double[][] chart) {
		for (int i2 = 0; i2 < chart[0].length; i2 ++) {
			for (int i = 0; i < chart.length; i ++) {
				System.out.print((int)Math.abs(chart[i][i2]) + " ");
			}
			System.out.println();
		}
		System.out.println();
	}

	private static double[][] plotCandles(Candlestick[] candles, double chartHigh, double chartLow) {
		double[][] chart = new double[chartWidth][chartHeight];
		boolean decreasing;
		double step = (chartHigh - chartLow) / chartHeight;
		for (int i = 0; i < chartWidth; i ++) {
			decreasing = candles[i].close < candles[i].open;
			double top = Math.max(candles[i].close, candles[i].open);
			double bottom = Math.min(candles[i].close, candles[i].open);
			for (int i2 = 0; i2 < chartHeight; i2 ++) {
				if (inBetween(bottom, chartLow + i2 * step, chartLow + (i2-1) * step, chartLow + (i2+1) * step, top)) {
					chart[i][i2] = decreasing ? -1 : 1;
				}
			}
			boolean allZero = true;
			for (int i2 = 0; i2 < chartHeight; i2 ++) {
				if (chart[i][i2] != 0) { allZero = false; }
			}
			if (allZero) {
				System.out.println(step + " " + chartHigh + " " + chartLow + " " + candles[i].close + " " + candles[i].open);
			}
		}
		return chart;
	}

	private static boolean inBetween(double lower, double mid, double last, double next, double upper) {
		if (mid > upper) {
			if(Math.abs(upper - last) > Math.abs(mid - upper)) {
				upper = mid;
				if (upper < lower) {
					lower = upper;
				}
			}
		}
		if (mid < lower) {
			if(Math.abs(mid - next) > Math.abs(mid - lower)) {
				lower = mid;
				if (lower > upper) {
					upper = lower;
				}
			}
		}
		return lower <= mid && mid <= upper;
	}

	private static double low(Candlestick[] candles) {
		double low = Double.MAX_VALUE;
		for (Candlestick candle : candles) {
			low = candle.low < low ? candle.low : low;
		}
		return low;
	}

	private static double high(Candlestick[] candles) {
		double high = -Double.MAX_VALUE;
		for (Candlestick candle : candles) {
			high = candle.high > high ? candle.high : high;
		}
		return high;
	}

	//percentage change
	private static double pc(double first, double last) {
		if (first == 0) { return 0D; }
		return ((last - first) / first) * 100D;
	}
}