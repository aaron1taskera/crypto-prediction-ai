package trigger;

import java.util.Arrays;

import stocks.Candlestick;
import stocks.Numerics;

public class TriggerSteadyPump extends TriggerSuddenPump {

	public TriggerSteadyPump() throws Exception { super("steadypump"); }

	public boolean isTriggered(Candlestick[] candles, Candlestick[] btcCandles, int index) {
		if (isSteadyPump(Arrays.copyOfRange(candles, 0, index + 1))) {
			try {
				return true;
			} catch (Exception e) { return false; }
		} else {
			return false;
		}
	}

	private boolean isSteadyPump(Candlestick[] candles) {
		boolean meets = wasSteadyPump(candles);
		Candlestick[] candles2;
		for (int i = 0; i < 48; i ++) {
			candles2 = Arrays.copyOfRange(candles, 0, candles.length - (i + 1));
			if (wasSteadyPump(candles2)) {
				return false;
			}
		}

		return meets;
	}

	private boolean wasSteadyPump(Candlestick[] candles) {
		boolean steady = true;
		for (int i = -3; i < 0; i ++) {
			if (candles[candles.length+i].delta() <= 0) { return false; }
			/*if (i != -3) {
				if (candles[candles.length+i].open < candles[candles.length+i-1].close) { steady = false; }
			}*/
		}
		if (Numerics.pc(candles[candles.length-3].getOpen(), candles[candles.length-1].getClose()) < Candlestick.PUMP_DELTA) {
			return false;
		}
		if (candles[candles.length-1].getClose() < Candlestick.high(Arrays.copyOfRange(candles, candles.length - 48, candles.length - 1))) {
			return false;
		}
		return steady;
	}
}