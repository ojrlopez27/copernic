package edu.cmu.ubi.simu;

import edu.cmu.ubi.simu.harlequin.control.HarlequinController;
import edu.cmu.ubi.simu.scenario.demo.SimuController;

/**
 * Created by oscarr on 5/4/18.
 */

public class Main{
    public static final boolean useMUF = true;
    public static final boolean useSimu = true;

    public static void main(String args[]){

        // let's start the MUF (server) first
        if(useMUF) HarlequinController.getInstance().start();

        // now, let's start the simulation
        if(useSimu) SimuController.getInstance().start();
    }
}

