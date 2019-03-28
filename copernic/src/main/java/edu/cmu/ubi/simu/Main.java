package edu.cmu.ubi.simu;

import edu.cmu.ubi.simu.copernic.control.CopernicController;
import edu.cmu.ubi.simu.scenario.demo.SimuController;

/**
 * This is a main class that starts two main processes:
 * 1) the process orchestration carried out by CopernicController and
 * 2) the simulation performed by Siafu simulator
 * Created by oscarr on 5/4/18.
 */
public class Main{
    public static final boolean useMUF = true;
    public static final boolean useSimu = true;

    public static void main(String args[]){

        // let's start the MUF (server) first
        if(useMUF) CopernicController.getInstance().start();

        // now, let's start the simulation
        if(useSimu) SimuController.getInstance().start();
    }
}

