/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

/**
 * A container that maps Receivers.
 * 
 * @author wlbean
 *
 */
public class Receivers {
	
	private static Logger log = Logger.getLogger(Receivers.class);
	
	/**
	 * Create empty receiver collection.
	 */
	
	private Map<Id<Receiver>, Receiver> receivers = new HashMap<>();
	
	public Receivers(Collection<Receiver> receivers){
		makeMap(receivers);
	}
	
	/**
	 * Add receivers to the empty collection.
	 * @param receivers
	 */
	
	private void makeMap(Collection<Receiver> receivers){
		for (Receiver r : receivers){
			this.receivers.put(r.getId(), r);
		}
	}
	
	public Receivers(){
		
	}
	
	/**
	 * Returns the receivers in the collection.
	 * @return
	 */
	
	public Map<Id<Receiver>, Receiver> getReceivers(){
		return receivers;
	}
	
	/**
	 * Add new receivers to the collection.
	 * @param receiver
	 */
	
	public void addReceiver(Receiver receiver){
		if(!receivers.containsKey(receiver.getId())){
			receivers.put(receiver.getId(), receiver);
		}
		else log.warn("receiver" + receiver.getId() + "already exists.");
	}

}
