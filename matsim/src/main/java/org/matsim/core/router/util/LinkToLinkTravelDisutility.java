package org.matsim.core.router.util;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public interface LinkToLinkTravelDisutility {

	public double getLinkToLinkTravelDisutility(final Link fromLink, final Link toLink, final double time, final Person person, final Vehicle vehicle);
	
	public double getLinkMinimumTravelDisutility(final Link fromLink);
}
