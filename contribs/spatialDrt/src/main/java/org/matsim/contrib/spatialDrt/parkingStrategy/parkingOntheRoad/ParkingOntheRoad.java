package org.matsim.contrib.spatialDrt.parkingStrategy.parkingOntheRoad;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.spatialDrt.bayInfrastructure.BayManager;
import org.matsim.contrib.spatialDrt.parkingStrategy.ParkingStrategy;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.algorithms.NetworkCleaner;

import java.util.*;

public class ParkingOntheRoad implements ParkingStrategy, IterationStartsListener {
    private Map<Id<Link>, Integer> linkRecord = new HashMap<>(); // Interger counts the number of vehicles parks on the link
    @Inject
    private QSim qsim;
    private final Map<Id<Link>, Integer> supply = new HashMap<>();
    private final double vehicleLength = 8.0; //TODO: Later Move to the DRTConfig or vehicle File
    private Network cleanNetwork;
    private Network network;


    @Inject
    public ParkingOntheRoad(@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network cleanNetwork, Network network, BayManager bayManager, ControlerListenerManager manager) {
        //controlerListenerManager.addControlerListener(this);
        this.cleanNetwork = cleanNetwork;
        this.network = network;
        manager.addControlerListener(this);
        new NetworkCleaner().run(cleanNetwork);
        for (Link link : cleanNetwork.getLinks().values()){
            if (link.getNumberOfLanes() > 1 && !isTransitStop(link, bayManager) ) {
                supply.put(link.getId(), (int) Math.floor(link.getLength() / vehicleLength));
            } else {
                supply.put(link.getId(), 0);
            }
        }
    }


    private boolean isTransitStop(Link link, BayManager bayManager) {
        return bayManager.getStopIdByLinkId(link.getId()) != null;
    }

    @Override
    public ParkingStrategy.ParkingLocation parking(Vehicle vehicle, double time) {
        Link currentLink = ((DrtStayTask)vehicle.getSchedule().getCurrentTask()).getLink();
        if (supply.get(currentLink.getId()) > 0){
            if (!linkRecord.containsKey(currentLink.getId())){
                linkRecord.put(currentLink.getId(),0);
                modifyLanes(currentLink.getId(), time, -1.D);
            }
            if (supply.get(currentLink.getId()) > linkRecord.get(currentLink.getId())) {
                int num = linkRecord.get(currentLink.getId()) + 1;
                linkRecord.put(currentLink.getId(), num);
                return null;
            }
        }
        return new ParkingLocation(vehicle.getId(),nextLink(currentLink));
    }

    private Link nextLink(Link currentLink){
        Link cleanLink = cleanNetwork.getLinks().get(currentLink.getId());
        //Random random = new Random();
        Map<Id<Link>, ? extends Link> nextLinks = cleanLink.getToNode().getOutLinks();
        //ArrayList<Id<Link>> linksKey = new ArrayList<>(nextLinks.keySet());
        Link link = nextLinkWithProbability(nextLinks);
        return link;
    }

    private Link nextLinkWithProbability(Map<Id<Link>, ? extends Link> nextLinks) {
        List<Integer> prob = new ArrayList<>();
        List<Id<Link>> linkids = new ArrayList<>();
        int addProb;
        for (Link link: nextLinks.values()){
            linkids.add(link.getId());
            if (supply.get(link.getId()) == 0){
                addProb = 1;
            }else{
                addProb = 3;
            }
            if (prob.size() != 0) {
                prob.add(prob.get(prob.size() - 1) + addProb);
            }else{
                prob.add(addProb);
            }
        }
        Random random = new Random();
        int index = random.nextInt(prob.get(prob.size() - 1)) + 1;
        int low = 0;
        int high = prob.size() - 1;
        int mid;
        while (low < high ){
            mid = (low + high) >>> 1;
            if (prob.get(mid) < index || prob.get(mid) == 0){
                low = mid + 1;
            }else if (prob.get(mid) >= index){
                high = mid;
            }
        }
        return nextLinks.get(linkids.get(low));
    }

    @Override
    public void departing(Vehicle vehicle, double time) {
        int previousIdx = vehicle.getSchedule().getCurrentTask().getTaskIdx() - 1;
        Link link = ((DrtStayTask)vehicle.getSchedule().getTasks().get(previousIdx)).getLink();
        if (!linkRecord.containsKey(link.getId())){
            throw new RuntimeException("The departing vehicle has not registered in link records");
        }
        int num = linkRecord.get(link.getId()) - 1;
        linkRecord.put(link.getId(),num);
        if (num == 0){
            modifyLanes(link.getId(), time, 0.D);
        }
    }

    @Override
    public ParkingStrategy.Strategies getCurrentStrategy(Id<Vehicle> vehicleId) {
        return Strategies.ParkingOntheRoad;
    }

    public void modifyLanes(Id<Link> linkId, double time, double change){
        Link currentLink = network.getLinks().get(linkId);
        double numOfLanes = currentLink.getNumberOfLanes();
        NetworkChangeEvent event = new NetworkChangeEvent(time + Math.random());
        event.addLink(currentLink);
        NetworkChangeEvent.ChangeValue capacityChange = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, (numOfLanes + change)/numOfLanes * currentLink.getCapacity() / 3600.0);
        NetworkChangeEvent.ChangeValue lanesChange = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, numOfLanes + change);
        event.setLanesChange(lanesChange);
        event.setFlowCapacityChange(capacityChange);
        qsim.addNetworkChangeEvent(event);
    }



//    @Override
//    public void notifyMobsimInitialized(MobsimInitializedEvent e) {
//        linkRecord.clear();
//    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        linkRecord.clear();
    }
}

