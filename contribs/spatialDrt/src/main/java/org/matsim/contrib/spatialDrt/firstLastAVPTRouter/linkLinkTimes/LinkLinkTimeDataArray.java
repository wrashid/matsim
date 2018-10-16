package org.matsim.contrib.spatialDrt.firstLastAVPTRouter.linkLinkTimes;

public class LinkLinkTimeDataArray implements LinkLinkTimeData {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//Attributes
	private double[] linkLinkTimeMeans;
	private double[] linkLinkTimeSquares;
	private int[] numTimes;

	//Constructors
	public LinkLinkTimeDataArray(int numSlots) {
		linkLinkTimeSquares = new double[numSlots];
		linkLinkTimeMeans = new double[numSlots];
		numTimes = new int[numSlots];
		resetLinkLinkTimes();
	}

	//Methods
	@Override
	public int getNumData(int timeSlot) {
		return numTimes[timeSlot<linkLinkTimeMeans.length?timeSlot:(linkLinkTimeMeans.length-1)];
	}
	@Override
	public double getLinkLinkTime(int timeSlot) {
		return linkLinkTimeMeans[timeSlot<linkLinkTimeMeans.length?timeSlot:(linkLinkTimeMeans.length-1)];
	}
	@Override
	public double getLinkLinkTimeVariance(int timeSlot) {
		int index = timeSlot<linkLinkTimeMeans.length?timeSlot:(linkLinkTimeMeans.length-1);
		return linkLinkTimeSquares[index]-Math.pow(linkLinkTimeMeans[index], 2);
	}
	@Override
	public synchronized void addLinkLinkTime(int timeSlot, double linkLinkTime) {
		linkLinkTimeSquares[timeSlot] = (linkLinkTimeSquares[timeSlot]*numTimes[timeSlot]+Math.pow(linkLinkTime, 2))/(numTimes[timeSlot]+1);
		linkLinkTimeMeans[timeSlot] = (linkLinkTimeMeans[timeSlot]*numTimes[timeSlot]+linkLinkTime)/++numTimes[timeSlot];		
	}
	@Override
	public void resetLinkLinkTimes() {
		for(int i=0; i<linkLinkTimeMeans.length; i++) {
			linkLinkTimeMeans[i] = 0;
			numTimes[i] = 0;
		}
	}

}
