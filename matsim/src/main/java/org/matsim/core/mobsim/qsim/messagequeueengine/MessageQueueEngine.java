package org.matsim.core.mobsim.qsim.messagequeueengine;

import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.jdeqsimengine.SteppableSchedulerImpl;

import javax.inject.Inject;

class MessageQueueEngine implements MobsimBeforeSimStepListener {

	private final SteppableSchedulerImpl scheduler;

	@Inject
	MessageQueueEngine(final SteppableSchedulerImpl scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		scheduler.doSimStep(e.getSimulationTime());
	}

}
