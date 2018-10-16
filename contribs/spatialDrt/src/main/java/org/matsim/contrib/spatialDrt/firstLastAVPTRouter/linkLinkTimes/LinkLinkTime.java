package org.matsim.contrib.spatialDrt.firstLastAVPTRouter.linkLinkTimes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.Serializable;

public interface LinkLinkTime extends Serializable {

	//Methods
	public double getLinkLinkTime(Id<Link> linkOId, Id<Link> linkDId, double time);
	public double getLinkLinkTimeVariance(Id<Link> linkOId, Id<Link> linkDId, double time);
		
}
