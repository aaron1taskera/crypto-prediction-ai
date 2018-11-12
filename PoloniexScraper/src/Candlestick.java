import java.io.DataOutputStream;

public class Candlestick {
	public long date;
	public double high;
	public double low;
	public double open;
	public double close;
	public double volume;
	public double quoteVolume;
	public double weightedAverage;
	
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
}