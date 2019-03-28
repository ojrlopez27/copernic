package edu.cmu.ubi.simu.scenario.demo;

import de.nec.nle.siafu.control.Controller;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;

import java.util.HashMap;

/**
 * Created by oscarr on 5/7/18.
 */
public class SimuController {
    private static SimuController instance;

    private SimuController(){}

    public static SimuController getInstance(){
        if(instance == null){
            instance = new SimuController();
        }
        return instance;
    }

    public void start(){
        String scenario = "demo";
        String simulationPath = String.format("%s/%s/", CommonUtils.getProperty("copernic.simu.scenarios.path"), scenario);
        HashMap<String, Class> map = new HashMap<>();
        try {
            for (String className : new String[]{"AgentModel", "ContextModel", "WorldModel"}) {
                map.put(className, Class.forName( String.format("edu.cmu.ubi.simu.scenario.%s.%s", scenario, className) ));
//                map.put(className, Class.forName(String.format("de.nec.nle.siafu.simulation.valencia.%s", className)));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        new Controller(null, simulationPath, map);
    }
}
