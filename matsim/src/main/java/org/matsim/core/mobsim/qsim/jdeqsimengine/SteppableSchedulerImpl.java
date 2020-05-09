package org.matsim.core.mobsim.qsim.jdeqsimengine;

import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.mobsim.jdeqsim.Message;
import org.matsim.core.mobsim.jdeqsim.MessageQueueImpl;
import org.matsim.core.mobsim.jdeqsim.SchedulerImpl;

import javax.inject.Inject;

public class SteppableSchedulerImpl extends SchedulerImpl implements Steppable {

    private Message lookahead;
    private boolean finished = false;

    @Inject
    public SteppableSchedulerImpl(MessageQueueImpl queue) {
        super(queue);
    }

    @Override
    public void doSimStep(double time) {
        finished = false; // I don't think we can restart once the queue has run dry, but just in case.
        if (lookahead != null && time < lookahead.getMessageArrivalTime()) {
            return;
        }
        if (lookahead != null) {
            lookahead.processEvent();
            lookahead.handleMessage();
            lookahead = null;
        }
        while (!queue.isEmpty()) {
            Message m = queue.getNextMessage();
            if (m != null && m.getMessageArrivalTime() <= time) {
                m.processEvent();
                m.handleMessage();
            } else {
                lookahead = m;
                return;
            }
        }
        finished = true; // queue has run dry.
    }

    public boolean isFinished() {
        return finished;
    }

}
