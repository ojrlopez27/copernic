package edu.cmu.ubi.simu.copernic.services;

import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.composer.services.Service;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by oscarr on 5/25/18.
 */
public class GoHomeDecoService extends Service {
    private int timesExecuted = 0;
    public GoHomeDecoService(String deviceName, Behavior behavior, ConcurrentSkipListSet<String> state){
        super(deviceName, behavior, state);
    }

    @Override
    public boolean execute(Object... params) {
        timesExecuted++;
        if (timesExecuted == 2 && user.equals("bob")) {
            network.triggerPostconditions(behavior);
        }
        return true;
    }
}
