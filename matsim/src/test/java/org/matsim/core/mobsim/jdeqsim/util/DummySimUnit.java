package org.matsim.core.mobsim.jdeqsim.util;

import org.matsim.core.mobsim.jdeqsim.Message;
import org.matsim.core.mobsim.jdeqsim.SchedulerImpl;
import org.matsim.core.mobsim.jdeqsim.SimUnit;

public class DummySimUnit extends SimUnit{

	public DummySimUnit(SchedulerImpl scheduler) {
		super(scheduler);
	}

	public void handleMessage(Message m) {
	}

}
