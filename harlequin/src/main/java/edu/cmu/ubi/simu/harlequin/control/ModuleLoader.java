package edu.cmu.ubi.simu.harlequin.control;

import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.ubi.simu.harlequin.orchestrator.SimuOrchestrator;
import edu.cmu.ubi.simu.harlequin.plugin.IntentExtractorPlugin;

import java.util.concurrent.TimeUnit;

/**
 * Created by oscarr on 5/7/18.
 */
public class ModuleLoader {

    public static Config createConfig() {
        return new Config.Builder()
                .setExceptionTraceLevel( Constants.SHOW_ALL_EXCEPTIONS)
                .setSessionManagerPort(Integer.parseInt( CommonUtils.getProperty("harlequin.muf.comm.session.manager.port")))
                .setPathLogs(CommonUtils.getProperty("harlequin.muf.logs.path.debug"))
                .setSessionTimeout(5, TimeUnit.MINUTES)
                .setServerAddress( String.format("tcp://%s", CommonUtils.getPublicIP()) ) //"tcp://127.0.0.1") //use IP instead of 'localhost'
                .build();
    }

    public static PluginModule[] createComponents() {
        return new PluginModule[]{
                new PluginModule.Builder(SimuOrchestrator.class,
                        IntentExtractorPlugin.class, "IntentExtractorPlugin")
                        .build()
        };
    }
}