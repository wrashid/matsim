package org.matsim.vis.snapshotwriters;

import org.matsim.api.core.v01.network.Link;

import java.util.Collection;

/**Interface that is meant to replace the direct "QueueLink" accesses in the visualizer
 * 
 * @author nagel
 *
 */
public interface VisLink {

	Link getLink() ;
	
	Collection<? extends VisVehicle> getAllVehicles() ;
	
	VisData getVisData() ;
}
