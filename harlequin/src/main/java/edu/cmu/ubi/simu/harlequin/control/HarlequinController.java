package edu.cmu.ubi.simu.harlequin.control;


import de.nec.nle.siafu.model.World;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Pair;
import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;
import edu.cmu.inmind.multiuser.controller.composer.bn.CompositionController;
import edu.cmu.inmind.multiuser.controller.composer.devices.Device;
import edu.cmu.inmind.multiuser.controller.composer.services.Service;
import edu.cmu.inmind.multiuser.controller.composer.simulation.SimuConstants;
import edu.cmu.inmind.multiuser.controller.composer.ui.BNGUIVisualizer;
import edu.cmu.inmind.multiuser.controller.composer.ui.VisualizerObserver;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.muf.MUFLifetimeManager;
import edu.cmu.inmind.multiuser.controller.muf.MultiuserController;
import edu.cmu.inmind.multiuser.controller.muf.ShutdownHook;
import edu.cmu.inmind.multiuser.controller.session.CrossSessionChoreographer;
import edu.cmu.ubi.simu.Main;
import edu.cmu.ubi.simu.harlequin.orchestrator.SimuOrchestrator;
import edu.cmu.ubi.simu.harlequin.plugin.AgentSimuExecutor;
import edu.cmu.ubi.simu.harlequin.services.*;
import edu.cmu.ubi.simu.scenario.demo.Constants;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static edu.cmu.inmind.multiuser.controller.composer.group.User.ADMIN;
import static edu.cmu.ubi.simu.scenario.demo.Constants.SimSteps.*;


/**
 * Created by oscarr on 5/7/18.
 */
public class HarlequinController implements Runnable, VisualizerObserver {
    private static HarlequinController instance;
    private CompositionController compositionController;
    private MultiuserController multiuserController;
    private BNGUIVisualizer plot;
    private AgentSimuExecutor agentModel;
    private boolean shouldPlot = true;
    private Map<String, SimuOrchestrator> orchestrators;
    private World world;
    private boolean interactionHasStarted = false;
    private Constants.SimSteps currentStep;
    private Constants.SimSteps lastStep;
    private ConcurrentHashMap<String, List<Pair<String, String>>> simuActionsMap;
    private ConcurrentLinkedQueue<Pair<Long, Runnable>> actions = new ConcurrentLinkedQueue<>();
    private final static long FREQUENCY_BN_PLOT = TimeUnit.MILLISECONDS.toMillis(1000);
    private final static long DELAY_SERVICE_PROCESSING = TimeUnit.MILLISECONDS.toMillis(4000);
    private final static String MOVE = "[MOVE]";
    private final static String WANDER = "[WANDER]";


    private static List<String> correctSeqOfServices = Arrays.asList(
            "alice-phone-get-self-location",
            "bob-tablet-get-self-location",
            "alice-phone-find-place-location",
            "alice-phone-get-distance-to-place",
            "bob-phone-find-place-location",
            "bob-phone-get-distance-to-place",
            "server-admin-calculate-nearest-place",
            "alice-phone-share-grocery-list",
            "bob-phone-do-grocery-shopping",
            "alice-phone-find-place-location",
            "alice-phone-do-grocery-shopping",
            "bob-tablet-find-place-location",
            "bob-phone-do-beer-shopping",
            "bob-phone-find-place-location",
            "bob-phone-go-home-decor",
            "bob-phone-go-pharmacy",
            "bob-tablet-go-home-decor",
            "alice-phone-go-home-decor");
    private static int seqIdx = 0;

    private HarlequinController(){
        orchestrators = new HashMap<>();
        simuActionsMap = new ConcurrentHashMap<>();
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public static HarlequinController getInstance(){
        if(instance == null){
            instance = new HarlequinController();
        }
        return instance;
    }

    public void start(){
        try {
            initMUF();
            initComposition();
            new Thread(this).start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void initMUF() throws Exception{
        multiuserController = MUFLifetimeManager.startFramework(
                ModuleLoader.createComponents(),
                ModuleLoader.createConfig());
        multiuserController.addPostCreate(this);
        multiuserController.addShutDownHook(new ShutdownHook() {
            @Override
            public void execute() {
                MUFLifetimeManager.stopFramework(multiuserController);
            }
        });
    }


    private void initComposition(){
        // this is our composition controller
        compositionController = new CompositionController("behavior-network.json");

        // create users
        compositionController.createUsers("alice", "bob");

        // create devices
        compositionController.createDevice("bob", Device.TYPES.PHONE).setGPSturnedOn(false);
        compositionController.createDevice("bob", Device.TYPES.TABLET).setBatteryLevel(6);
        compositionController.createDevice("alice", Device.TYPES.PHONE);
        compositionController.createDevice( ADMIN, Device.TYPES.SERVER);

        // create services
        compositionController.instantiateServices( getMapOfServices(),
                new Pair<>(Arrays.asList("bob", "alice"), getUserServices()),
                new Pair<>(Arrays.asList(ADMIN), getServerServices() ));

        // set system/user goals and states
        compositionController.addState(Arrays.asList("bob-party-not-organized", "alice-party-not-organized" ));
        compositionController.setGoals( Arrays.asList(  "grocery-shopping-done", "whatever" )); // "organize-party-done"
        // let's extract xxx-required preconditions
        compositionController.endMeansAnalysis();

        // create the gui
        if(shouldPlot){
            plot = createGUI( compositionController.getNetwork() );
            plot.addObserver(this);
        }
        compositionController.getNetwork().shouldWaitForUserSelection(true);
        compositionController.getNetwork().shouldWaitForSync(true);

        new Thread(new ExecuteActions()).start();
    }


    private List<String> getUserServices(){
        return Arrays.asList(
                "get-self-location",
                "find-place-location",
                "get-distance-to-place",
                "share-grocery-list",
                "do-grocery-shopping",
                "do-beer-shopping",
                "go-home-decor",
                "go-pharmacy");
    }

    private List<String> getServerServices(){
        return Arrays.asList(
                "calculate-nearest-place",
                "organize-party");
    }

    private BNGUIVisualizer createGUI(BehaviorNetwork network) {
        String title = "Service Composition";
        List<Behavior> services = new ArrayList<>(network.getBehaviors());
        String[] series = new String[services.size() + 1];
        for(int i = 0; i < services.size(); i++){
            series[i] = services.get(i).getShortName();
        }
        series[series.length-1] = "Threshold";
        return BNGUIVisualizer.start(title, series, network);
    }

    @Override
    public void run() {
        while( !Thread.interrupted() ){
            if( canRun() ) {
                runOneStep();
            }
            CommonUtils.sleep(FREQUENCY_BN_PLOT);
        }
    }

    private boolean canRun() {
        return world != null && !world.isPaused() && !plot.isPaused() && interactionHasStarted && currentStep != null;
    }


    /**
     * This method invokes the BehaviorNetwork, check the correct sequence (according to the scenario),
     * execute the service on the corresponding device, add events to the state according to the step, and
     * plots the results on the chart.
     * @return
     */
    public int runOneStep() {
        compositionController.updateDeviceState();
        int idx = compositionController.selectService()[0];
        boolean stepIsValid = isStateValid();
        if( stepIsValid ){
            if( compositionController.isExecutable() ){
                if(currentStep != null) lastStep = currentStep.copy();
                checkCorrectSequence(idx);
                compositionController.executeService(idx, currentStep.ordinal());
                String serviceName = compositionController.getServices().get(idx).getName();
                serviceName = serviceName.replace("phone-", "").replace("tablet-", "");
                Log4J.warn(this, "serviceName: " + serviceName);
                List<Pair<String, String>> actions = simuActionsMap.remove(serviceName);
                if(actions == null)
                    System.out.println("");
                if( actions != null ) {
                    for (Pair<String, String> action : actions) {
                        runStep(DELAY_SERVICE_PROCESSING, action.fst, action.snd);
                    }
                }
            }
            addEventToState();
        }
        if(shouldPlot) refreshPlot();
        return idx;
    }

    //TODO: we need to remove this in the future, this is just for demoing purposes
    private boolean isStateValid(){
        if(lastStep == null) return true;
        if(lastStep.ordinal() != currentStep.ordinal()) return true;
        //exceptions
        if(lastStep.equals(S9_BOB_DO_GROCERY) || lastStep.equals(S11_ALICE_DO_GROCERY)
                || lastStep.equals(S13_BOB_GO_BEER_SHOP) || lastStep.equals(S14_BOB_FIND_HOME_DECO)
                || lastStep.equals(S16_ALICE_HEADACHE) || lastStep.equals(S18_BOB_GO_HOME_DECO))
            return true;
        return false;
    }

    public void addAgentModel(AgentSimuExecutor agentModel) {
        this.agentModel = agentModel;
    }

    public void addOrchestrator(String sessionId, SimuOrchestrator orchestrator) {
        orchestrators.put(sessionId, orchestrator);
    }

    public void sendToOrchestrator(String sessionId, String message) {
        orchestrators.get(sessionId).sendInMindResponse(message);
    }

    public String executeEvent(String sessionId, String command, Constants.SimSteps simuStep) {
        String response = "";
        switch (simuStep){
            case S0_BOB_STARTS:
                response = "Definitively, I can help you guys with that! let me check available services...";
                interactionHasStarted = true;
                break;

            case S9_BOB_DO_GROCERY:
                response = "Sounds perfect Bob!";
                break;

            case S11_ALICE_DO_GROCERY:
                response = "A little change in plans, thanks Alice!";
                break;

            case S13_2_ALICE_AT_SUPERMARKET:
                response = "Thanks for letting me know...";
                break;

            case S13_BOB_GO_BEER_SHOP:
                response = "Great!";
                break;

            case S14_BOB_FIND_HOME_DECO:
                response = "Bob, let me check what is pending to do.";
                break;

            case S16_ALICE_HEADACHE:
                response = "Sorry to hear that Alice, let me look for some pharmacies near Bob";
                break;

            case S18_BOB_GO_HOME_DECO:
                response = "Ok, now you guys can meet at Home Deco 1";
                break;

            case S20_GO_HOME:
                response = "Thanks for letting me know";
                break;
        }
        executeUserCommand(sessionId, command, simuStep);
        sendToChoreographer(sessionId, response);
        return response;
    }


    /**
     * This is called externally from a PlugabbleComponent or an Orchestrator when a user action/command
     * happens.
     * @param sessionId
     * @param command
     * @param simSteps
     */
    public void executeUserCommand(String sessionId, String command, Constants.SimSteps simSteps) {
        if(Main.useSimu) agentModel.runStep(sessionId, command, simSteps);
        currentStep = simSteps.copy();
        addNextTriggerActions();
    }


    /**
     * This simulation actions will be executed later in the future, when the corresponding service (behavior)
     * is activated in runOneStep method.
     */
    private void addNextTriggerActions() {
        try {
            switch (currentStep) {
                case S0_BOB_STARTS:
                    //S1_ALICE_LOCATION:
                    addToMap("alice-get-self-location",
                            new Pair<>("Alice", "InMind: Alice, I got your current location"));
                    //S2_BOB_LOCATION:
                    addToMap("bob-get-self-location",
                            new Pair<>("Bob", "InMind: Bob, I got your current location"));

                    //S3_ALICE_FIND_GROCERY:
                    addToMap("alice-find-place-location",
                            new Pair<>( "Alice","InMind: Searching for supermarkets near Alice..."));
                    //S4_ALICE_DIST_GROCERY:
                    addToMap("alice-get-distance-to-place",
                            new Pair<>("Alice","InMind: Alice, Market2 is close to you"));
                    //S5_BOB_FIND_GROCERY:
                    addToMap("bob-find-place-location",
                            new Pair<>( "Bob", "InMind: Searching for supermarkets near Bob..."));
                    //S6_BOB_DIST_GROCERY:
                    addToMap("bob-get-distance-to-place",
                            new Pair<>("Bob", "InMind: Bob, Market1 is close to you"));
                    //S7_CLOSER_TO_GROCERY:
                    addToMap("server-admin-calculate-nearest-place",
                            new Pair<>("Bob", "InMind: Bob, you are closer than Alice. " +
                                    "Do you want to do the grocery shopping?"));
                    //S8_ALICE_SHARE_SHOP_LIST:
                    addToMap("alice-share-grocery-list",
                            new Pair<>("Alice", "InMind: Sharing shopping list from Alice"));
                    break;

                case S9_BOB_DO_GROCERY:
                    //S9_1_BOB_MOVE_TO_GROCERY:
                    addToMap("bob-do-grocery-shopping",
                            new Pair<>("Bob", MOVE));
                    //S10_ALICE_ADD_PREF:
                    addToMap("alice-find-place-location",
                            new Pair<>("Alice", "InMind: Alice, there's an organic market nearby. " +
                                    "Do you want to do the grocery shopping?"));
                    break;

                case S11_ALICE_DO_GROCERY:
                    addToMap("alice-do-grocery-shopping",
                            new Pair<>("Bob", WANDER));
                    //S11_1_ALICE_MOVE_TO_GROCERY:
                    addToMap("alice-do-grocery-shopping",
                            new Pair<>("Alice", MOVE));
                    //S12_BOB_FIND_BEER:
                    addToMap("bob-find-place-location",
                            new Pair<>("Bob", "InMind: Bob, do you carry your driver license?"));
                    break;

                case S13_BOB_GO_BEER_SHOP:
                    addToMap("bob-do-beer-shopping",
                            new Pair<>("Bob", "InMind: Bob, there's a beer shop nearby you"));
                    //S13_1_BOB_MOVE_BEER_SHOP:
                    addToMap("bob-do-beer-shopping",
                            new Pair<>("Bob", MOVE));
                    break;

                case S14_BOB_FIND_HOME_DECO:
                    //S15_BOB_GO_HOME_DECO:
                    addToMap("bob-find-place-location",
                            new Pair<>("Bob", "InMind: Bob, there's a HomeDeco on your way home"));
                    //S15_1_BOB_MOVE_HOME_DECO:
                    addToMap("bob-go-home-decor",
                            new Pair<>("Bob", MOVE));
                    break;

                case S16_ALICE_HEADACHE:
                    //S17_BOB_COUPONS:
                    addToMap("bob-go-pharmacy",
                            new Pair<>("Bob", "InMind: Bob, pharmacy 1 is closer, but you have some coupons for pharmacy 2"));
                    addToMap("bob-go-pharmacy", new Pair<>("Bob", MOVE ));
                    break;

                case S18_BOB_GO_HOME_DECO:
                    addToMap("bob-go-home-decor", new Pair<>("Bob", MOVE ));
                    addToMap("alice-go-home-decor", new Pair<>("Alice", MOVE ));
                    break;

                case S20_GO_HOME:
                    System.out.println("");
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void addToMap(String serviceName, Pair<String, String> newAction) {
        List<Pair<String, String>> actions = simuActionsMap.get(serviceName);
        if(actions == null) actions = new ArrayList<>();
        actions.add(newAction);
        simuActionsMap.put(serviceName, actions);
    }

    /**
     * this method runs a step on the simulation and send messages to orchestrator so it
     * can send it back to the device (client). We use a delay to simulate the time services
     * spend doing some processing
     * @param delay
     * @param user
     * @param message
     */
    private void runStep(long delay, String user, String message) {
        if(message.equals(MOVE) || message.equals(WANDER)){
            Log4J.error(this, "currentStep: " + currentStep);
            agentModel.move(currentStep);
        }else {
            actions.add( new Pair<>(delay, () -> {
                Log4J.error("runStep", "inside action.run. user: " + user + ", message: " + message);
                if (Main.useSimu) agentModel.runStep(0, user, message);
                sendToOrchestrator(user, message);
                sendToChoreographer(user, message);
            }));
        }
        currentStep = currentStep.increment();
    }


    public void sendToChoreographer(final String sessionFrom, final String message){
        CommonUtils.execute(() -> {
            //we need to wait few seconds in order to show synchronized messages in the other phone
            CommonUtils.sleep(2000);
            Log4J.debug("sendToChoreographer", String.format("session: %s,  message: %s", sessionFrom, message));
            CrossSessionChoreographer.getInstance().passMessage(sessionFrom, message);
        });
    }


    private void checkCorrectSequence(int idx) {
        if( seqIdx < correctSeqOfServices.size() &&
                !compositionController.getServices().get(idx).getName().equals( correctSeqOfServices.get(seqIdx) ) ) {
//            throw new IllegalStateException(
//                    String.format("Incorrect sequence of behaviors/services. It should be '%s' and it received '%s'",
//                            correctSeqOfServices.get(seqIdx), compositionController.getServices().get(idx).getName()));
            Log4J.error(this, String.format("Incorrect sequence of behaviors/services. It should be '%s' and it received '%s'",
                    correctSeqOfServices.get(seqIdx), compositionController.getServices().get(idx).getName()));
        }
        seqIdx++;
    }


    private void addEventToState() {
        if( currentStep.ordinal() < SimuConstants.SimSteps.values().length ) {
            switch (currentStep) {
                case S7_CLOSER_TO_GROCERY:
                    compositionController.addState(Arrays.asList("bob-grocery-shopping-not-done",
                            "alice-grocery-shopping-not-done"));
                    compositionController.removeState("calculate-nearest-place-required");
                    compositionController.removeState("bob-distance-to-place-provided");
                    compositionController.removeState("alice-distance-to-place-provided");
                    compositionController.addGoal("organize-party-done");
                    break;
                case S9_BOB_DO_GROCERY:
                    compositionController.addState(Arrays.asList("bob-is-closer-to-place"));
                    break;

                case S9_1_BOB_MOVE_TO_GROCERY:
                    compositionController.addState(Arrays.asList("bob-is-closer-to-place"));
                    compositionController.removeState("alice-is-closer-to-place");
                    compositionController.removeState("bob-is-closer-to-place");
                    compositionController.removeState("alice-place-location-provided");
                    compositionController.addState(Arrays.asList("alice-place-location-required",
                            "alice-place-name-provided"));
                    break;
                case S10_ALICE_ADD_PREF:
                    compositionController.addState(Arrays.asList("alice-close-to-organic-supermarket"));
                    compositionController.removeState("alice-grocery-shopping-required");
                    break;
                case S11_ALICE_DO_GROCERY:
                    compositionController.addState(Arrays.asList("alice-grocery-shopping-required"));
                    break;
                case S12_BOB_FIND_BEER:
                    compositionController.removeState("bob-place-location-provided");
                    compositionController.removeState("bob-grocery-shopping-required");
                    compositionController.removeState("alice-grocery-shopping-required");
                    compositionController.addState(Arrays.asList("bob-place-location-required",
                            "bob-place-name-provided",
                            "bob-beer-shopping-not-done",
                            "bob-beer-shopping-required"));
                    break;
                case S13_BOB_GO_BEER_SHOP:
                    compositionController.removeState("bob-grocery-shopping-required");
                    compositionController.removeState("alice-grocery-shopping-required");
                    compositionController.removeState("bob-grocery-shopping-not-done");
                    compositionController.addState(Arrays.asList("bob-driver-license-provided",
                            "bob-is-closer-to-place",
                            "bob-beer-shopping-not-done",
                            "bob-beer-shopping-required"));
                    break;
                case S14_BOB_FIND_HOME_DECO:
                    compositionController.removeState("bob-place-location-provided");
                    compositionController.addState(Arrays.asList("bob-place-location-required",
                            "bob-place-name-provided"));
                    break;
                case S15_BOB_GO_HOME_DECO:
                    compositionController.addState(Arrays.asList("bob-is-closer-to-place",
                            "bob-buy-decoration-required"));
                    break;
                case S15_1_BOB_MOVE_HOME_DECO:
                    compositionController.removeState("bob-is-closer-to-place");
                    compositionController.removeState("bob-buy-decoration-required");
                    break;
                case S16_ALICE_HEADACHE:
                    compositionController.removeState("bob-place-location-provided");
                    compositionController.removeState("bob-buy-decoration-required");
                    compositionController.addState(Arrays.asList(
                            "bob-place-name-provided",
                            "bob-somebody-has-headache",
                            "bob-no-medication-at-home",
                            "bob-is-closer-to-place",
                            "bob-has-coupons"));
                    break;
                case S17_BOB_COUPONS:
                    compositionController.addState(Arrays.asList("bob-has-coupons"));
                    break;
                case S18_BOB_GO_HOME_DECO:
                    compositionController.addState(Arrays.asList("bob-buy-decoration-required"));
                    break;
                case S19_ALICE_GO_HOME_DECO:
                    compositionController.addState(Arrays.asList("alice-buy-decoration-required",
                            "alice-is-closer-to-place"));
                    break;
            }
        }
    }


    private void refreshPlot() {
        plot.setDataset(compositionController.getNormalizedActivations(),
                compositionController.getThreshold(),
                compositionController.getBehActivated(),
                compositionController.getActivationBeh(),
                compositionController.isExecutable());
        //we need to force sync here, not before, otherwise winner service is not plotted on chart
        compositionController.getNetwork().shouldWaitForSync(true);
    }

    @Override
    public void onPausedChanged(boolean paused) {
        world.stopSpinning(paused);
    }

    class ExecuteActions implements Runnable{
        @Override
        public void run() {
            while(!Thread.interrupted()){
                if(!actions.isEmpty() && canRun()){
                    Pair<Long, Runnable> action = actions.poll();
                    if( !compositionController.getNetwork().isWaitingForUserSelection() )
                        CommonUtils.sleep(action.fst);
                    // this is done when picking a service from the GUI
                    // compositionController.getNetwork().shouldWaitForSync(false);
                    action.snd.run();
                }
                CommonUtils.sleep(50);
            }
        }
    }


    private Map<String,Class<? extends Service>> getMapOfServices() {
        Map<String, Class<? extends Service>> map = new HashMap<>();
        map.put("get-self-location", LocationService.class);
        map.put("find-place-location", FindPlaceService.class);
        map.put("get-distance-to-place", DistanceCalculatorService.class);
        map.put("calculate-nearest-place", WhoIsNearestService.class);
        map.put("share-grocery-list", ShareGroceryListService.class);
        map.put("do-grocery-shopping", DoGroceryShoppingService.class);
        map.put("do-beer-shopping", DoBeerShoppingService.class);
        map.put("go-home-decor", GoHomeDecoService.class);
        map.put("organize-party", OrganizePartyService.class);
        map.put("go-pharmacy", GoPharmacyService.class);
        return map;
    }
}
