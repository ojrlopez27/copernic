package edu.cmu.ubi.simu.harlequin.plugin;

import edu.cmu.ubi.simu.scenario.demo.Constants;

/**
 * Created by oscarr on 6/14/18.
 */
public interface AgentSimuExecutor {
    void runStep(String sessionId, String message, Constants.SimSteps simSteps);
    Constants.SimSteps runStep(long delay, String sessionId, String message);
    void move(Constants.SimSteps step);
}
