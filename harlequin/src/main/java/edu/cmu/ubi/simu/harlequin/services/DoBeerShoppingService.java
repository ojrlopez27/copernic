package edu.cmu.ubi.simu.harlequin.services;

import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.composer.services.*;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by oscarr on 5/25/18.
 */
public class DoBeerShoppingService extends edu.cmu.inmind.multiuser.controller.composer.services.Service {

    public DoBeerShoppingService(String deviceName, Behavior behavior, ConcurrentSkipListSet<String> state){
        super(deviceName, behavior, state);
    }

    @Override
    public boolean execute(int simulationStep) {
        network.triggerPostconditions(behavior);
        return true;
    }
}