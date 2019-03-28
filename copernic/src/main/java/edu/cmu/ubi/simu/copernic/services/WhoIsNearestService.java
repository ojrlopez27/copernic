package edu.cmu.ubi.simu.copernic.services;

import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.composer.services.*;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;


/**
 * Created by oscarr on 4/30/18.
 */
public class WhoIsNearestService extends Service {

    public WhoIsNearestService(String deviceName, Behavior behavior, ConcurrentSkipListSet<String> state){
        super(deviceName, behavior, state);
    }

    @Override
    public boolean execute(Object... params) {
        network.triggerPostconditions(behavior, Arrays.asList("bob-is-closer-to-place"));
        return true;
    }
}
