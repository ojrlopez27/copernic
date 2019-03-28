package edu.cmu.ubi.simu.copernic.plugin;

import edu.cmu.ubi.simu.copernic.control.ActionCallback;
import edu.cmu.ubi.simu.scenario.demo.Constants;

/**
 * Created by oscarr on 6/14/18.
 */
public interface AgentSimuExecutor {
    void runStep(String userId, String message, Constants.Events simSteps);
    void runStep(long delay, String userId, String message);
    void move(Constants.Events step, ActionCallback callback);
}
