package org.matsim.contrib.spatialDrt.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;

public class DrtQueueTask extends StayTaskImpl implements DrtTask {

    public DrtQueueTask(double beginTime, double endTime, Link link) {
        super(beginTime, endTime, link);
    }

    @Override
    public DrtTask.DrtTaskType getDrtTaskType() {
        return DrtTaskType.STAY;
    }
}
