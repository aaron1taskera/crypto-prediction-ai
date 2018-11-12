package stocks;
//Represents trades over a period of time i.e a candlestick
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;

import csvutils.Stat;

public class Candlestick {
	public long date;
	public double high;
	public double low;
	public double open;
	public double close;
	public double volume;
	public double quoteVolume;
	public double weightedAverage;

	public static final double PUMP_DELTA = 7D; //changing by this many percent in a period is considered a pump
	public static final double DUMP_DELTA = -7D; //changing by this many percent in a period is considered a dump

	public static final int AROON_DEFAULT_PERIODS = 25; //the default number of periods in which to calculate the Aroon indicator
	public static final int RSI_DEFAULT_PERIODS = 14; //the default number of periods in which to calculate the RSI
	public static final int TMF_DEFAULT_PERIODS = 21; //the default number of periods in which to calculate the TMF indicator
	public static final int BOLLINGER_DEFAULT_PERIODS = 20; //the default number of periods in which to calculate the bollinger bands
	public static final int RVI_GREEN_DEFAULT_LENGTH = 10; //the default SMA length which the RVI green line is calculated
	public static final int RVI_RED_DEFAULT_LENGTH = 4; //the default weighted SMA length which the RVI red line is calculated
	public static final int STOCHASTIC_DEFAULT_LENGTH = 14; //the default number of periods in which to calculate the stochastic oscillator
	public static final int STOCHASTIC_SIGNAL_DEFAULT_LENGTH = 3; //the default number of periods in which to calculate the MA of the stochastic oscillator
	
	public static final int ICHIMOKU_CONVERSION_LENGTH = 9;
	public static final int ICHIMOKU_BASE_LENGTH = 26;

	public long getDate() { return date; }
	public double getHigh() { return high; }
	public double getLow() { return low; }
	public double getOpen() { return open; }
	public double getClose() { return close; }
	public double getVolume() { return volume; }
	public double getQuoteVolume() { return quoteVolume; }
	public double getWeightedAverage() { return weightedAverage; }

	//Returns the percentage change between the opening and closing prices of this candlestick
	public double delta() { return Numerics.pc(open, close); }

	public void unserialize(DataInputStream dis) throws Exception {
		date = dis.readLong();
		high = dis.readDouble();
		low = dis.readDouble();
		open = dis.readDouble();
		close = dis.readDouble();
		volume = dis.readDouble();
		quoteVolume = dis.readDouble();
		weightedAverage = dis.readDouble();
	}

	//Unserializes a set of candlesticks from a file
	public static ArrayList<Candlestick> loadFromFile(File input) {
		ArrayList<Candlestick> candles = new ArrayList<Candlestick>();
		DataInputStream dis;
		try {
			dis = new DataInputStream(new FileInputStream(input));
			try {
				while (true) {
					Candlestick c = new Candlestick();
					c.unserialize(dis);
					candles.add(c);
				}
			} catch (Exception e) {  }
			dis.close();
		} catch (Exception e) {  }
		return candles;
	}

	public static double calculateStandardisedDPO(Candlestick[] candles) {
		double price = candles[candles.length - 1].close;
		double SMA = Numerics.SMA(closePrices(Arrays.copyOfRange(candles, candles.length - 11 - 21, candles.length - 11)));
		if (SMA == 0) { return 0D; }
		return (price - SMA) / SMA;
	}

	//Calculates the stochastic oscillator
	public static double calculateStochastic(Candlestick[] candles) {
		double lowest = Candlestick.low(Arrays.copyOfRange(candles, candles.length - STOCHASTIC_DEFAULT_LENGTH, candles.length));
		double highest = Candlestick.high(Arrays.copyOfRange(candles, candles.length - STOCHASTIC_DEFAULT_LENGTH, candles.length));
		return Numerics.lockRange(0, 100 * (candles[candles.length - 1].close - lowest) / (highest - lowest), 100);
	}

	//Calculates the stochastic oscillator histogram
	public static double calculateStochasticHist(Candlestick[] candles) {
		double stochastics = 0;
		for (int i = 0; i < STOCHASTIC_SIGNAL_DEFAULT_LENGTH; i ++) {
			stochastics += calculateStochastic(Arrays.copyOfRange(candles, 0, candles.length - i));
		}
		double signal = stochastics / STOCHASTIC_SIGNAL_DEFAULT_LENGTH;
		return calculateStochastic(Arrays.copyOfRange(candles, 0, candles.length)) - signal;
	}

	//Calculates the relative vigor index
	public static double calculateRVI(Candlestick[] candles) {
		Candlestick last = candles[candles.length - 1];
		if (last.high == last.low) { return 0; }
		return (last.close - last.open) / (last.high - last.low);
	}

	//Calculates the relative vigor index histogram
	public static double calculateRVIHist(Candlestick[] candles) {
		double green = Numerics.SMA(closePrices(Arrays.copyOfRange(candles, candles.length - RVI_GREEN_DEFAULT_LENGTH, candles.length)));
		double red = 0;
		double vol = 0;
		for (int i = candles.length - 1; i > candles.length - RVI_RED_DEFAULT_LENGTH - 1; i --) {
			red += candles[i].close * candles[i].volume;
			vol += candles[i].volume;
		}
		if (vol == 0) {
			return 0;
		} else {
			return green - (red / vol);
		}
	}

	//Calculates the lower bollinger band with default bollinger period
	public static double calculateBollingerLower(Candlestick[] candles) {
		return calculateBollingerLower(candles, BOLLINGER_DEFAULT_PERIODS);
	}

	//Calculates the lower bollinger band
	public static double calculateBollingerLower(Candlestick[] candles, int length) {
		double middle = calculateBollingerMiddle(candles, length);
		Stat s = Numerics.getStat(closePrices(Arrays.copyOfRange(candles, candles.length - length, candles.length)));
		return middle - s.std * 2;
	}

	//Calculates the upper bollinger band with default bollinger period
	public static double calculateBollingerUpper(Candlestick[] candles) {
		return calculateBollingerUpper(candles, BOLLINGER_DEFAULT_PERIODS);
	}

	//Calculates the upper bollinger band
	public static double calculateBollingerUpper(Candlestick[] candles, int length) {
		double middle = calculateBollingerMiddle(candles, length);
		Stat s = Numerics.getStat(closePrices(Arrays.copyOfRange(candles, candles.length - length, candles.length)));
		return middle + s.std * 2;
	}

	//Calculates the middle bollinger band with default bollinger period
	public static double calculateBollingerMiddle(Candlestick[] candles) {
		return calculateBollingerMiddle(candles, BOLLINGER_DEFAULT_PERIODS);
	}

	//Calculates the middle bollinger band
	public static double calculateBollingerMiddle(Candlestick[] candles, int length) {
		return Numerics.SMA(closePrices(Arrays.copyOfRange(candles, candles.length - length, candles.length)));
	}

	//Calculates the percentage of time the stock has spend outside it's bollinger band
	public static double percOutsideBollingerBand(Candlestick[] candles, int length) {
		return Numerics.lockRange(0, percAboveBollingerBand(candles, length) + percBelowBollingerBand(candles, length), 100);
	}

	//Calculates the percentage of time the stock has spent above it's bollinger band
	public static double percAboveBollingerBand(Candlestick[] candles, int length) {
		double outside = 0;
		double inside = 0;
		double upper;
		Candlestick c;
		double stickSize;
		for (int i = 0; i < length; i ++) {
			c = candles[candles.length - length + i];
			upper = calculateBollingerUpper(Arrays.copyOfRange(candles, 0, candles.length + 1 - length + i));
			upper = Math.min(c.high, upper);
			stickSize = c.high - c.low;
			if (stickSize != 0) {
				outside += (c.high - upper) / stickSize;
				inside += (upper - c.low) / stickSize;
			} else {
				if (c.high > upper) {
					outside += 1;
					inside += 0;
				} else {
					outside += 0;
					inside += 1;
				}
			}
		}
		return Numerics.lockRange(0, outside * 100D / (outside + inside), 100);
	}

	//Calculates the percentage of time the stock has spend below it's bollinger band
	public static double percBelowBollingerBand(Candlestick[] candles, int length) {
		double outside = 0;
		double inside = 0;
		double lower;
		Candlestick c;
		double stickSize;
		for (int i = 0; i < length; i ++) {
			c = candles[candles.length - length + i];
			lower = calculateBollingerLower(Arrays.copyOfRange(candles, 0, candles.length + 1 - length + i));
			lower = Math.max(lower, 0);
			lower = Math.max(c.low, lower);
			stickSize = c.high - c.low;
			if (stickSize != 0) {
				outside += (lower - c.low) / stickSize;
				inside += (c.high - lower) / stickSize;
			} else {
				if (c.low < lower) {
					outside += 1;
					inside += 0;
				} else {
					outside += 0;
					inside += 1;
				}
			}
		}
		return Numerics.lockRange(0, outside * 100D / (outside + inside), 100);
	}

	public static double[] calculateTMFGraph(Candlestick[] candles, int periods) {
		double[] tmfs = new double[periods];
		for (int i = 0; i < tmfs.length; i ++) {
			tmfs[tmfs.length - 1 - i] = calculateTwiggsMoneyFlow(Arrays.copyOfRange(candles, 0, candles.length - i));
		}
		return tmfs;
	}

	//Calculates the TMF with default TMF period
	public static double calculateTwiggsMoneyFlow(Candlestick[] candles) {
		return calculateTwiggsMoneyFlow(candles, TMF_DEFAULT_PERIODS);
	}

	//Calculates the TMF
	public static double calculateTwiggsMoneyFlow(Candlestick[] candles, int length) {
		double volume[] = new double[length * 2];
		double ad[] = new double[length * 2];

		double lastClose;
		double close;
		double high;
		double trh;
		double low;
		double trl;

		for(int i = 0; i < volume.length; i ++) {
			volume[i] = candles[candles.length - volume.length + i].volume;
			lastClose = candles[candles.length - volume.length + i - 1].close;
			high = candles[candles.length - volume.length + i].high;
			close = candles[candles.length - volume.length + i].close;
			trh = Math.max(lastClose, high);
			low = candles[candles.length - volume.length + i].low;
			trl = Math.min(lastClose, low);
			ad[i] = volume[i] * (((close - trl) - (trh - close)) / (trh - trl));
		}

		if (Double.isNaN(ad[0])) {
			for (int i = 1; i < ad.length; i ++) {
				if (!Double.isNaN(ad[i])) {
					ad[0] = ad[i];
				}
			}
		}

		if (Double.isNaN(ad[0])) {
			return 0;
		}

		if (Double.isNaN(ad[ad.length - 1])) {
			for (int i = ad.length - 2; i > -1; i --) {
				if (!Double.isNaN(ad[i])) {
					ad[ad.length - 1] = ad[i];
				}
			}
		}

		for (int i = 0; i < ad.length; i ++) {
			if (Double.isNaN(ad[i])) {
				int i2 = i + 1;
				double interpolate = 0;
				for (; i2 < ad.length; i2 ++) {
					if (!Double.isNaN(ad[i2])) {
						interpolate = ad[i2];
						break;
					}
				}
				i2 -= i;
				double prior = ad[i - 1];
				ad[i] = prior + ((interpolate - prior) / (i2 + 1));
			}
		}

		double volumeEMA = Numerics.EMA(volume, 21);
		double adEMA = Numerics.EMA(ad, 21);

		return Numerics.lockRange(-1, adEMA / volumeEMA, 1);
	}

	//Calculates the stochastic RSI using default RSI period
	public static double calculateStochRSI(Candlestick[] candles) {
		double rsi = calculateRSI(candles);
		double highestRSI = highestRSI(candles);
		double lowestRSI = lowestRSI(candles);
		double rsiDiff = (highestRSI - lowestRSI);
		if (rsiDiff == 0) { rsiDiff = Double.MIN_VALUE; }
		return Numerics.lockRange(0, (rsi - lowestRSI) / rsiDiff, 1);
	}

	//Finds the lowest RSI in a period
	public static double lowestRSI(Candlestick[] candles) {
		double min = 100D;
		double rsi;
		for (int i = 0; i < 14; i ++) {
			rsi = calculateRSI(Arrays.copyOfRange(candles, 0, candles.length - i));
			min = Math.min(rsi, min);
		}
		return min;
	}

	//Finds the highest RSI in a period
	public static double highestRSI(Candlestick[] candles) {
		double max = 0D;
		double rsi;
		for (int i = 0; i < 14; i ++) {
			rsi = calculateRSI(Arrays.copyOfRange(candles, 0, candles.length - i));
			max = Math.max(rsi, max);
		}
		return max;
	}

	//Calculates the RSI with default RSI period
	public static double calculateRSI(Candlestick[] candles) {
		return calculateRSI(candles, RSI_DEFAULT_PERIODS);
	}

	//Calculates the RSI
	public static double calculateRSI(Candlestick[] candles, int length) {
		double rs = calculateAverageGain(candles, length) / calculateAverageLoss(candles, length);
		return Numerics.lockRange(0, 100D - (100D / (1D + rs)), 100);
	}

	//Calculates the average gains over a period
	public static double calculateAverageGain(Candlestick[] candles, int length) {
		double diffs[] = new double[length*2];
		for (int i = 0; i < diffs.length; i++) {
			diffs[diffs.length - 1 - i] = Math.max(0, candles[candles.length - i - 1].close - candles[candles.length - i - 2].close);
		}
		return Numerics.EMA(diffs, length);
	}

	//Calculates the average losses over a period
	public static double calculateAverageLoss(Candlestick[] candles, int length) {
		double diffs[] = new double[length*2];
		for (int i = 0; i < diffs.length; i++) {
			diffs[diffs.length - 1 - i] = Math.abs(Math.min(0, candles[candles.length - i - 1].close - candles[candles.length - i - 2].close));
		}
		return Numerics.EMA(diffs, length);
	}

	//Calculates the aroon up with default aroon period
	public static double calculateAroonUp(Candlestick[] candles) {
		return calculateAroonUp(candles, AROON_DEFAULT_PERIODS);
	}

	//Calculates the aroon up
	public static double calculateAroonUp(Candlestick[] candles, int length) {
		return (((double)length - periodsSinceHigh(Arrays.copyOfRange(candles, candles.length - length, candles.length))) / (double)length) * 100D;
	}

	//Calculates the aroon down with default aroon period
	public static double calculateAroonDown(Candlestick[] candles) {
		return calculateAroonDown(candles, AROON_DEFAULT_PERIODS);
	}

	//Calculates the aroon down
	public static double calculateAroonDown(Candlestick[] candles, int length) {
		return (((double)length- periodsSinceLow(Arrays.copyOfRange(candles, candles.length - length, candles.length))) / (double)length) * 100D;
	}

	//The number of periods since the last high
	public static double periodsSinceHigh(Candlestick[] candles) {
		double high = 0D;
		double day = 0D;
		Candlestick c;
		for (int i = 0; i < candles.length; i ++) {
			c = candles[candles.length - i - 1];
			if (c.high > high) {
				day = i;
				high = c.high;
			}
		}
		return day;
	}

	//The number of periods since the last low
	public static double periodsSinceLow(Candlestick[] candles) {
		double low = 0D;
		double day = 0D;
		Candlestick c;
		for (int i = 0; i < candles.length; i ++) {
			c = candles[candles.length - i - 1];
			if (low == 0) {
				low = c.low;
			} else {
				if (c.low < low) {
					day = i;
					low = c.low;
				}
			}
		}
		return day;
	}

	//The position of the weighted average
	public double weightAveragePos() {
		return (high-low) == 0D ? 0D : (((weightedAverage - low) / (high-low))-0.5D)*2D;
	}

	//On average how many std deviations is everything from the mean. Weighted
	public static double stability(Candlestick[] candles) {
		Stat stat = Numerics.getStat(weightedAverages(candles));
		if (stat.std == 0) { return 0; }
		double distance = 0;
		double total = 0;

		for (Candlestick c : candles) {
			distance += Math.abs((c.weightedAverage - stat.mean) / stat.std) * c.volume;
			total += c.volume;
		}

		if (Double.isNaN(distance / total)) { return 0; }
		return distance / total;
	}

	//Returns the size of the shadow relative to the body (%wise)
	public double shadowToBody() {
		return Math.min(Math.abs((high - low) / (close - open) == 0D ? ((high - low) / 5D) : (close - open)), 5D);
	}

	//Returns the position of the shadow relative to the body. 1 if all above, -1 if all below
	public double shadowPosition() {
		if (close > open) {
			double above = high - open;
			double below = close - low;
			if (above + below == 0) {
				return 0;
			} else {
				return (above - below) / (above + below);
			}

		} else if (open > close) {
			double above = high - close;
			double below = open - low;
			if (above + below == 0) {
				return 0;
			} else {
				return (above - below) / (above + below);
			}
		} else {
			double above = high - open;
			double below = high - close;
			if (above + below == 0) {
				return 0;
			} else {
				return Math.abs((above - below) / (above + below));
			}
		}
	}

	//returns the total number of pumps in a set of candlesticks
	public static double totalPumps(Candlestick[] candles) {
		double total = 0D;
		for (Candlestick c : candles) {
			total += c.delta() >= PUMP_DELTA ? 1D : 0D;
		}
		return total;
	}

	//returns the total number of dumps in a set of candlesticks
	public static double totalDumps(Candlestick[] candles) {
		double total = 0D;
		for (Candlestick c : candles) {
			total += c.delta() <= DUMP_DELTA ? 1D : 0D;
		}
		return total;
	}

	//Returns a Stan object (mean, std) for the volumes of a set of candles
	public static Stat getVolStat(Candlestick[] candles) {
		return Numerics.getStat(volumes(candles));
	}

	//The sum of volumes over a period of time
	public static double getVolSum(Candlestick[] candles) {
		return Numerics.sum(volumes(candles));
	}

	//Returns the weighted average for a set of Candles
	public static double weightedAverage(Candlestick[] candles) {
		double vol = 0D;
		double total = 0D;
		for (Candlestick c : candles) {
			total += c.volume * c.weightedAverage;
			vol += c.volume;
		}
		if (vol == 0) { return candles[candles.length - 1].weightedAverage; }
		return total / vol;
	}

	//Calculates a standardised MACD giving a picture of momentum that is unbiased by price
	public static double calculateStanMACDHist(Candlestick[] candles) {
		double[] macds = new double[18];
		for (int i = 0; i < macds.length; i ++) {
			macds[macds.length - i - 1] = MACD(Arrays.copyOfRange(candles, 0, candles.length - i)) / candles[candles.length - 1].close;
		}
		return macds[macds.length - 1] - Numerics.SMA(macds);
	}

	//The number of buy signals generated in a certain MACD peak
	public static int calculateMACDHistSells(Candlestick[] candles) {
		boolean up = false;
		int peaks = 0;
		int index = 0;
		double curHist;
		double prevHist;
		double lastHist;
		while (!(up && calculateMACDHist(Arrays.copyOfRange(candles, 0, candles.length - index)) < 0)) {
			curHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, candles.length - index));
			if (curHist > 0) {
				up = true;
				prevHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, candles.length - index - 1));
				lastHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, candles.length - index - 2));
				if (curHist > 0 && prevHist > 0 && lastHist > 0) {
					if (curHist < prevHist && prevHist > lastHist) {
						peaks++;
					}
				}
			}
			index++;
		}
		return peaks;
	}

	//The number of buy signals generated in a certain MACD trough
	public static int calculateMACDHistBuys(Candlestick[] candles) {
		int troughs = 0;
		int index = 0;
		double curHist;
		double prevHist;
		double lastHist;
		while ((curHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, candles.length - index))) < 0) {
			curHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, candles.length - index));
			prevHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, candles.length - index - 1));
			lastHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, candles.length - index - 2));
			if (curHist < 0 && prevHist < 0 && lastHist < 0) {
				if (curHist > prevHist && prevHist < lastHist) {
					troughs++;
				}
			}
			index++;
		}
		return troughs;
	}

	//Finds the offset where a previous MACD histogram  trough is found
	public static int calculateMACDHistBuysOffset(Candlestick[] candles) {
		int trough = 0;
		int index = 0;
		double curHist;
		double prevHist;
		double lastHist;
		while (trough == 0) {
			curHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, candles.length - index));
			prevHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, candles.length - index - 1));
			lastHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, candles.length - index - 2));
			if (curHist < 0 && prevHist < 0 && lastHist < 0) {
				if (curHist > prevHist && prevHist < lastHist) {
					trough = index;
				}
			}
			index++;
		}
		return trough;
	}

	//Finds the offset where a previous MACD histogram peak is found
	public static int calculateMACDHistSellsOffset(Candlestick[] candles) {
		int peak = 0;
		int index = 0;
		double curHist;
		double prevHist;
		double lastHist;
		while (peak == 0) {
			curHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, candles.length - index));
			if (curHist > 0) {
				prevHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, candles.length - index - 1));
				lastHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, candles.length - index - 2));
				if (curHist > 0 && prevHist > 0 && lastHist > 0) {
					if (curHist < prevHist && prevHist > lastHist) {
						peak = index;
					}
				}
			}
			index++;
		}
		return peak;
	}

	//Finds the next index where a MACD histogram peak is found
	public static int calculateMACDHistNextSellsIndex(Candlestick[] candles, int startIndex) {
		boolean found = false;
		double curHist;
		double prevHist;
		double lastHist;

		while (!found) {
			startIndex++;
			lastHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, startIndex + 1));
			if (lastHist > 0) {
				prevHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, startIndex + 2));
				if (prevHist > 0 && prevHist > lastHist) {
					curHist = calculateMACDHist(Arrays.copyOfRange(candles, 0, startIndex + 3));
					if (curHist > 0 && prevHist > 0 && lastHist > 0) {
						if (curHist < prevHist && prevHist > lastHist) {
							found = true;
						}
					}
				}
			}
		}
		return startIndex + 2;
	}

	//Finds the next index where a RSI sell is found
	public static int calculateRSINextSellsIndex(Candlestick[] candles, int startIndex, int sellRSI) {
		boolean found = false;
		double rsi;

		while (!found) {
			startIndex++;
			rsi = calculateRSI(Arrays.copyOfRange(candles, 0, startIndex + 1));
			if (rsi > sellRSI) { found = true; }
		}
		return startIndex;
	}

	//Returns the lowest sell price for a set of candles
	public static double low(Candlestick[] candles) {
		return Numerics.low(lowPrices(candles));
	}

	//Returns the highest sell price for a set of candles
	public static double high(Candlestick[] candles) {
		return Numerics.high(highPrices(candles));
	}

	//Returns the highest opening price for a set of candles
	public static double highOpen(Candlestick[] candles) {
		return Numerics.high(openPrices(candles));
	}

	//Returns the lowest opening price for a set of candles
	public static double lowOpen(Candlestick[] candles) {
		return Numerics.low(openPrices(candles));
	}

	//Returns the index of the highest opening price for a set of candles
	public static int highOpenIndex(Candlestick[] candles) {
		return Numerics.highIndex(openPrices(candles));
	}

	//Returns the index of the lowest opening price for a set of candles
	public static int lowOpenIndex(Candlestick[] candles) {
		return Numerics.lowIndex(openPrices(candles));
	}

	//Calculates the MACD-histogram (distance between MACD and signal line)
	public static double calculateMACDHist(Candlestick[] candles) {
		double[] macds = new double[18];
		for (int i = 0; i < macds.length; i ++) {
			macds[macds.length - i - 1] = MACD(Arrays.copyOfRange(candles, 0, candles.length - i));
		}
		return macds[macds.length - 1] - Numerics.EMA(macds, 9);
	}

	//calculates the MACD from an array of candles
	public static double MACD(Candlestick[] candles) {
		double[] closePrices = closePrices(candles);
		return Numerics.EMA(closePrices, 10) - Numerics.EMA(closePrices, 26);
	}

	//returns an array of the close prices of a set of candlesticks
	public static double[] closePrices(Candlestick[] candles) {
		double[] closePrices = new double[candles.length];
		for (int i = 0; i < candles.length; i ++) {
			closePrices[i] = candles[i].close;
		}
		return closePrices;
	}

	//returns an array of the low prices of a set of candlesticks
	public static double[] lowPrices(Candlestick[] candles) {
		double[] lowPrices = new double[candles.length];
		for (int i = 0; i < candles.length; i ++) {
			lowPrices[i] = candles[i].low;
		}
		return lowPrices;
	}

	//returns an array of the high prices of a set of candlesticks
	public static double[] highPrices(Candlestick[] candles) {
		double[] highPrices = new double[candles.length];
		for (int i = 0; i < candles.length; i ++) {
			highPrices[i] = candles[i].high;
		}
		return highPrices;
	}

	//returns an array of the open prices of a set of candlesticks
	public static double[] openPrices(Candlestick[] candles) {
		double[] openPrices = new double[candles.length];
		for (int i = 0; i < candles.length; i ++) {
			openPrices[i] = candles[i].open;
		}
		return openPrices;
	}

	//returns an array of volumes for a set of candlesticks
	public static double[] volumes(Candlestick[] candles) {
		double[] volumes = new double[candles.length];
		for (int i = 0; i < candles.length; i ++) {
			volumes[i] = candles[i].volume;
		}
		return volumes;
	}

	//returns an array of weighted averages
	public static double[] weightedAverages(Candlestick[] candles) {
		double[] weightedAverages = new double[candles.length];
		for (int i = 0; i < candles.length; i ++) {
			weightedAverages[i] = candles[i].weightedAverage;
		}
		return weightedAverages;
	}
	
	public static double[] calculateIchimokuSignalGraph(Candlestick[] candles, int periods) {
		double[] ichimokus = new double[periods];
		for (int i = 0; i < ichimokus.length; i ++) {
			ichimokus[ichimokus.length - 1 - i] = ichimokuSignal(Arrays.copyOfRange(candles, 0, candles.length - i));
		}
		return ichimokus;
	}
	
	public static double ichimokuSignal(Candlestick[] candles) {
		double SMA = Numerics.SMA(closePrices(Arrays.copyOfRange(candles, candles.length - Candlestick.ICHIMOKU_BASE_LENGTH, candles.length)));
		if (SMA == 0) {
			return ichimokuConversion(candles) - ichimokuBase(candles);
		}
		return (ichimokuConversion(candles) - ichimokuBase(candles)) / SMA;
	}
	
	public static double ichimokuSignal(Candlestick[] candles, int conversionLength, int baseLength) {
		double SMA = Numerics.SMA(closePrices(Arrays.copyOfRange(candles, candles.length - baseLength, candles.length)));
		if (SMA == 0) {
			return ichimoku(candles, conversionLength) - ichimoku(candles, baseLength);
		}
		return (ichimoku(candles, conversionLength) - ichimoku(candles, baseLength)) / SMA;
	}
	
	public static double ichimokuConversion(Candlestick[] candles) { return ichimoku(candles, Candlestick.ICHIMOKU_CONVERSION_LENGTH); }
	
	public static double ichimokuBase(Candlestick[] candles) { return ichimoku(candles, Candlestick.ICHIMOKU_BASE_LENGTH); }
	
	public static double ichimoku(Candlestick[] candles, int length) {
		return (Candlestick.high(Arrays.copyOfRange(candles, candles.length - length, candles.length)) + Candlestick.low(Arrays.copyOfRange(candles, candles.length - length, candles.length))) / 2;
	}
}