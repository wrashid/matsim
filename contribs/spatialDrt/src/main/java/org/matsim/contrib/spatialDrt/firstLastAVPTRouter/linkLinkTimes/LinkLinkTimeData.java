package org.matsim.contrib.spatialDrt.firstLastAVPTRouter.linkLinkTimes;

import java.io.Serializable;

public interface LinkLinkTimeData extends Serializable {

	int getNumData(int i);
	double getLinkLinkTime(int i);
	void addLinkLinkTime(final int timeSlot, final double linkLinkTime);
	void resetLinkLinkTimes();
	double getLinkLinkTimeVariance(int timeSlot);

}
