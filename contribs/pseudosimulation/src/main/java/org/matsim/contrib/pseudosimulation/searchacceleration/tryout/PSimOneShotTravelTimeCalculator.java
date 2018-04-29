package org.matsim.contrib.pseudosimulation.searchacceleration.tryout;

import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;

/**
 * @author fouriep
 * @author Gunnar Flötteröd (extracting a subset of Pieter's original class).
 */
public class PSimOneShotTravelTimeCalculator 
// extends TravelTimeCalculator 
implements Provider<TravelTime> {

//	@Inject
//	PSimOneShotTravelTimeCalculator(TravelTimeCalculatorConfigGroup ttconfigGroup, EventsManager eventsManager,
//			Network network) {
//		super(network, ttconfigGroup);
//		eventsManager.addHandler(this);
//	}
	
	private final TravelTime travelTimes;
	
	PSimOneShotTravelTimeCalculator(TravelTime travelTimes) {
		this.travelTimes = travelTimes;
	}

//	@Override
//	public void reset(int iteration) {
//		// if (switcher == null || switcher.isQSimIteration()) {
//		// Logger.getLogger(this.getClass()).error("Calling reset on
//		// traveltimecalc");
//		// super.reset(iteration);
//		// } else {
//		// Logger.getLogger(this.getClass()).error("Not resetting travel times
//		// as this is a PSim iteration");
//		// }
//		throw new RuntimeException("should never be called");
//	}
//
//	@Override
//	public void handleEvent(LinkEnterEvent e) {
//		// if (switcher.isQSimIteration())
//		// super.handleEvent(e);
//	}
//
//	@Override
//	public void handleEvent(LinkLeaveEvent e) {
//		// if (switcher.isQSimIteration())
//		// super.handleEvent(e);
//	}
//
//	@Override
//	public void handleEvent(VehicleEntersTrafficEvent event) {
//		// if (switcher.isQSimIteration())
//		// super.handleEvent(event);
//	}
//
//	@Override
//	public void handleEvent(VehicleLeavesTrafficEvent event) {
//		// if (switcher.isQSimIteration())
//		// super.handleEvent(event);
//	}
//
//	@Override
//	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
//		// if (switcher.isQSimIteration())
//		// super.handleEvent(event);
//	}
//
//	@Override
//	public void handleEvent(VehicleAbortsEvent event) {
//		// if (switcher.isQSimIteration())
//		// super.handleEvent(event);
//	}

	@Override
	public TravelTime get() {
		return this.travelTimes;
	}
}
