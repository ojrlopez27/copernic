package edu.cmu.ubi.simu.harlequin.plugin;

import edu.cmu.ubi.simu.harlequin.control.ActionCallback;
import edu.cmu.ubi.simu.scenario.demo.Constants;

/**
 * Created by oscarr on 6/14/18.
 */
public interface AgentSimuExecutor {
    void runStep(String sessionId, String message, Constants.Events simSteps);
    void runStep(long delay, String sessionId, String message);
    void move(Constants.Events step, ActionCallback callback);
}
