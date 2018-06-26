package edu.cmu.ubi.simu.harlequin.services;

import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;

import java.util.concurrent.ConcurrentSkipListSet;

import static edu.cmu.inmind.multiuser.controller.composer.simulation.SimuConstants.S19_BOB_GO_HOME_DECO;

/**
 * Created by oscarr on 5/25/18.
 */
public class GoHomeDecoService extends edu.cmu.inmind.multiuser.controller.composer.services.Service {

    public GoHomeDecoService(String deviceName, Behavior behavior, ConcurrentSkipListSet<String> state){
        super(deviceName, behavior, state);
    }

    @Override
    public boolean execute(int simulationStep) {
        if (simulationStep >= S19_BOB_GO_HOME_DECO) {
            network.triggerPostconditions(behavior);
        }
        return true;
    }
}
