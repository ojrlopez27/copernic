package edu.cmu.ubi.simu.harlequin.services;

import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.composer.services.*;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by oscarr on 4/30/18.
 */
public class ShareGroceryListService extends edu.cmu.inmind.multiuser.controller.composer.services.Service {
    private boolean hasShoppingList;

    public ShareGroceryListService(String deviceName, Behavior behavior, ConcurrentSkipListSet<String> state){
        super(deviceName, behavior, state);
    }

    @Override
    public boolean execute(int simulationStep) {
        if( user.equals("bob") ) hasShoppingList = false;
        else hasShoppingList = true;

        if( hasShoppingList ) {
            network.triggerPostconditions(behavior,
                    Arrays.asList("bob-grocery-list-provided", "alice-grocery-list-provided"),
                    Arrays.asList("*-grocery-list-required") );
            return true;
        }
        return false;
    }
}
