package org.matsim.core.mobsim.qsim.jdeqsimengine;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.jdeqsim.JDEQSimConfigGroup;
import org.matsim.core.mobsim.jdeqsim.MessageQueueImpl;
import org.matsim.core.mobsim.qsim.QSim;

public class JDEQSimModule {

    private JDEQSimModule() {}

    public static void configure(QSim qsim) {
        SteppableSchedulerImpl scheduler = new SteppableSchedulerImpl(new MessageQueueImpl());
        JDEQSimEngine jdeqSimEngine = new JDEQSimEngine(ConfigUtils.addOrGetModule(qsim.getScenario().getConfig(), JDEQSimConfigGroup.NAME, JDEQSimConfigGroup.class), qsim.getScenario(), qsim.getEventsManager(), qsim.getAgentCounter(), scheduler);
        qsim.addMobsimEngine(jdeqSimEngine);
        qsim.addActivityHandler(jdeqSimEngine);
    }

}
