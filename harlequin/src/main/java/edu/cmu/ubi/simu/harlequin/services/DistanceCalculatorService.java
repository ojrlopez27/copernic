package edu.cmu.ubi.simu.harlequin.services;

import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.composer.services.Service;

import java.util.concurrent.ConcurrentSkipListSet;


/**
 * Created by oscarr on 5/22/18.
 */
public class DistanceCalculatorService extends Service {
    public DistanceCalculatorService(String deviceName, Behavior behavior, ConcurrentSkipListSet<String> state){
        super(deviceName, behavior, state);
    }

    @Override
    public boolean execute(Object... params) {
        network.triggerPostconditions(behavior);
        return true;
    }
}
