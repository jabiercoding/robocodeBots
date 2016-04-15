package jab.avk;

@SuppressWarnings("serial")
public class Enemy implements java.io.Serializable {
		
	public String name;
	public double bearing;
	public double bearingRadians;
	public double headingRadians;
	public double previousHeadingRadians;
	public double distance;
	public double x;
	public double y;
	public double velocity;
	public double energy;
	public double previousEnergy;	
	public int timeSinceLastScan;
	public int timeScanned;
	
}
