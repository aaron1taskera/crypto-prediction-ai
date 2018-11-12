package stocks;

import java.util.Arrays;

import csvutils.Stat;

public class Numerics {

	//Locks a number between a range
	public static double lockRange(double lower, double num, double upper) {
		if (Double.isNaN(num)) { return (upper + lower) / 2D; }
		return Math.min(upper, Math.max(lower, num));
	}
	
	//Returns the stat (mean, std) for an array of nums
	public static Stat getStat(double[] nums) {
		Stat s = new Stat();
		s.mean = 0D;
		for (double d : nums) {
			s.mean += d;
		}
		s.mean /= (double) nums.length;

		for(double d : nums) {
			s.std += (d-s.mean)*(d-s.mean);
		}
		s.std = Math.pow(s.std/(double)(nums.length-1), 0.5);
		
		if (Double.isNaN(s.std)) { s.std = 0; }
		if (Double.isNaN(s.mean)) { s.mean = 0; }
		
		return s;
	}

	//Returns the index of the maximum number in an array
	public static int highIndex(double[] nums) {
		double high = 0D;
		int index = 0;
		for (int i = 0; i < nums.length; i ++) {
			if (nums[i] > high) {
				index = i;
				high = nums[i];
			}
		}
		return index;
	}

	//Returns the index of the minimum number in an array
	public static int lowIndex(double[] nums) {
		double low = 0D;
		int index = 0;
		boolean start = false;
		for (int i = 0; i < nums.length; i ++) {
			if (!start) {
				low = nums[i];
				index = i;
				start = true;
			} else if (nums[i] < low) {
				low = nums[i];
				index = i;
			}
		}
		return index;
	}

	//Returns the maximum number in an array
	public static double high(double[] nums) {
		double high = 0D;
		for (double d : nums) {
			high = Math.max(d, high);
		}
		return high;
	}

	//Returns the minimum number in an array
	public static double low(double[] nums) {
		double low = 0D;
		int i = 0;
		for (double d : nums) {
			if (i == 0) {
				i ++;
				low = d;
			} else {
				low = Math.min(d, low);
			}
		}
		return low;
	}

	//base call for calculating EMA. Calls recursive method below
	public static double EMA(double[] nums, int length) {
		return EMA(nums, length, length);
	}

	//recursive EMA
	private static double EMA(double[] nums, int length, double remainder) {
		double k = 2D / ((double)length + 1D);
		if (remainder != 1) {
			return nums[nums.length - 1] * k + (1D-k) * EMA(Arrays.copyOfRange(nums, 0, nums.length - 1), length, remainder - 1);
		} else {
			return nums[nums.length - 1] * k + (1D-k) * SMA(Arrays.copyOfRange(nums, nums.length - 1 - length, nums.length - 1));
		}
	}

	//calculates a simple moving average
	public static double SMA(double[] nums) {
		double total = 0D;
		for (int i = 0; i < nums.length; i ++) {
			total += nums[i];
		}
		return total / (double) nums.length;
	}

	//percentage change
	public static double pc(double first, double last) {
		if (first == 0) { return 0D; }
		return ((last - first) / first) * 100D;
	}

	//Returns the percentage of a number
	public static double perc(double percentage, double of) {
		if (of == 0) { return 100D; }
		return percentage * 100D / of;
	}

	//Sums an array
	public static double sum(double[] nums) {
		double sum = 0;
		for (double d : nums) { sum += d; }
		return sum;
	}
	
	public static double[] generateTime(double[] input) {
		double[] times = new double[input.length];
		for (int i = 0; i < times.length; i ++) { times[i] = i; }
		return times;
	}
	
	public static double mean(double[] input) {
		double sum = 0;
		for (Double d : input) { sum += d; }
		return sum / (double)input.length;
	}
	
	public static double pearsons(double[] x, double[] y) {
		double meanX = mean(x);
		double meanY = mean(y);
		double rXY = 0;
		double rXX = 0;
		double rYY = 0;
		for (int i = 0; i < x.length; i ++) {
			rXY += (x[i] - meanX) * (y[i] - meanY);
			rXX += (x[i] - meanX) * (x[i] - meanX);
			rYY += (y[i] - meanY) * (y[i] - meanY);
		}

		rXX = Math.pow(rXX, 0.5);
		rYY = Math.pow(rYY, 0.5);
		if (rXX == 0 && rYY == 0) {
			return 1;
		} else if (rXX == 0 || rYY == 0) {
			return 0;
		} else {
			return rXY / (rXX * rYY);
		}
	}
}