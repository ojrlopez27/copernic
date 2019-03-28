package edu.cmu.ubi.simu.copernic.services;

import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.composer.services.Service;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;


/**
 * Created by oscarr on 5/24/18.
 */
public class DoGroceryShoppingService extends Service {

    public DoGroceryShoppingService(String deviceName, Behavior behavior, ConcurrentSkipListSet<String> state){
        super(deviceName, behavior, state);
    }

    @Override
    public boolean execute(Object... params) {
        if(user.equals("alice")) {
            network.triggerPostconditions(behavior, Arrays.asList("grocery-shopping-done"));
            network.resetState();
        }
        return true;
    }
}
