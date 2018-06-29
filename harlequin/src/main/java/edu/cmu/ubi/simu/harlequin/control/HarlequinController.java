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
import java.util.concurrent.atomic.AtomicBoolean;

import static edu.cmu.inmind.multiuser.controller.composer.group.User.CLOUD;
import static edu.cmu.ubi.simu.harlequin.util.ServiceConstants.*;
import static edu.cmu.ubi.simu.scenario.demo.Constants.SimSteps.*;


/**
 * Created by oscarr on 5/7/18.
 */
public class HarlequinController implements Runnable, VisualizerObserver {
    private boolean interactionHasStarted = false;
    private boolean shouldPlot = true;
    private CompositionController compositionController;
    private MultiuserController multiuserController;
    private BNGUIVisualizer plot;
    private AgentSimuExecutor agentModel;
    private Map<String, SimuOrchestrator> orchestrators;
    private World world;
    private Constants.SimSteps currentStep;
    private Constants.SimSteps lastStep;
    private Constants.SimSteps lastStepExecuted = S0_BOB_STARTS;
    private ConcurrentHashMap<String, List<Pair<String, String>>> simuActionsMap;
    private ConcurrentLinkedQueue<Pair<Long, Runnable>> actions = new ConcurrentLinkedQueue<>();
    private AtomicBoolean stopServiceTriggering = new AtomicBoolean(false);
    private static HarlequinController instance;
    private final static long FREQUENCY_BN_PLOT = TimeUnit.MILLISECONDS.toMillis(1000);
    private final static long DELAY_SERVICE_PROCESSING = TimeUnit.MILLISECONDS.toMillis(4000);


    private static List<String> correctSeqOfServices = Arrays.asList(
            alice_phone_get_self_location,
            bob_tablet_get_self_location,
            alice_phone_find_place_location,
            alice_phone_get_distance_to_place,
            bob_phone_find_place_location,
            bob_phone_get_distance_to_place,
            cloud_calculate_nearest_place,
            alice_phone_share_grocery_list,
            bob_phone_do_grocery_shopping,
            alice_phone_find_place_location,
            alice_phone_do_grocery_shopping,
            bob_tablet_find_place_location,
            bob_phone_do_beer_shopping,
            bob_phone_find_place_location,
            bob_phone_go_home_decor,
            bob_phone_go_pharmacy,
            bob_tablet_go_home_decor,
            alice_phone_go_home_decor);
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
        compositionController.createUsers(alice, bob);

        // create devices
        compositionController.createDevice(bob, Device.TYPES.PHONE).setGPSturnedOn(false);
        compositionController.createDevice(bob, Device.TYPES.TABLET).setBatteryLevel(6);
        compositionController.createDevice(alice, Device.TYPES.PHONE);
        compositionController.createDevice( CLOUD, Device.TYPES.SERVER);

        // create services
        compositionController.instantiateServices( getMapOfServices(),
                new Pair<>(Arrays.asList(bob, alice), getUserServices()),
                new Pair<>(Arrays.asList(CLOUD), getServerServices() ));

        // set system/user goals and states
        compositionController.addState(Arrays.asList(bob_party_not_organized, alice_party_not_organized ));
        compositionController.setGoals( Arrays.asList(  grocery_shopping_done, "whatever" )); // "organize_party_done"
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
                get_self_location,
                find_place_location,
                get_distance_to_place,
                share_grocery_list,
                do_grocery_shopping,
                do_beer_shopping,
                go_home_decor,
                go_pharmacy);
    }

    private List<String> getServerServices(){
        return Arrays.asList(
                calculate_nearest_place,
                organize_party);
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
    public void runOneStep() {
        if( !stopServiceTriggering.get() ) {
            compositionController.updateDeviceState();
            int idx = compositionController.selectService()[0];
            boolean stepIsValid = isStateValid();
            if (stepIsValid) {
                if (compositionController.isExecutable()) {
                    if (currentStep != null) lastStep = currentStep.copy();
                    checkCorrectSequence(idx);
                    compositionController.executeService(idx, currentStep.ordinal());
                    String serviceName = getActivatedServiceName(idx);
                    Log4J.error(this, "Executing service: " + serviceName);
                    List<Pair<String, String>> actions = simuActionsMap.remove(serviceName);
                    if (actions != null) {
                        for (Pair<String, String> action : actions) {
                            runStep(DELAY_SERVICE_PROCESSING, action.fst, action.snd);
                        }
                    }
                }
                addEventToState();
            }
            if(shouldPlot) refreshPlot();
        }
    }

    private String getActivatedServiceName(int idx) {
        String serviceName = compositionController.getServices().get(idx).getName();
        serviceName = serviceName.replace("phone-", "")
                .replace("tablet-", "")
                .replace("smartwatch-", "");
        return serviceName;
    }

    //TODO: we need to remove this in the future, this is just for demoing purposes
    private boolean isStateValid(){
        //if(stopServiceTriggering.get()) return false;
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

    public void sendToOrchestrator(String sessionId, String message, String messageId) {
        orchestrators.get(sessionId).sendInMindResponse(message, messageId);
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
                response = "I'm Sorry to hear that Alice, let me look for some pharmacies near Bob";
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
                    Log4J.debug(this, "addNextTriggerActions: " + currentStep);
                    //S1_ALICE_LOCATION:
                    addToMap(alice_get_self_location,
                            new Pair<>("Alice", "InMind: Alice, I got your current location"));
                    //S2_BOB_LOCATION:
                    addToMap(bob_get_self_location,
                            new Pair<>("Bob", "InMind: Bob, I got your current location"));

                    //S3_ALICE_FIND_GROCERY:
                    addToMap(alice_find_place_location,
                            new Pair<>( "Alice","InMind: Searching for supermarkets near Alice..."));
                    //S4_ALICE_DIST_GROCERY:
                    addToMap(alice_get_distance_to_place,
                            new Pair<>("Alice","InMind: Alice, WholeFoods is 3.6 miles away"));
                    //S5_BOB_FIND_GROCERY:
                    addToMap(bob_find_place_location,
                            new Pair<>( "Bob", "InMind: Searching for supermarkets near Bob..."));
                    //S6_BOB_DIST_GROCERY:
                    addToMap(bob_get_distance_to_place,
                            new Pair<>("Bob", "InMind: Bob, Target is 2 miles away"));
                    //S7_CLOSER_TO_GROCERY:
                    addToMap(cloud_calculate_nearest_place,
                            new Pair<>("Bob", "InMind: Bob, you are closer than Alice. " +
                                    "Do you want to do the grocery shopping?"));
                    //S8_ALICE_SHARE_SHOP_LIST:
                    addToMap(alice_share_grocery_list,
                            new Pair<>("Alice", "InMind: Sharing shopping list from Alice"));
                    break;

                case S9_BOB_DO_GROCERY:
                    Log4J.debug(this, "addNextTriggerActions: " + currentStep);
                    //S9_1_BOB_MOVE_TO_GROCERY:
                    addToMap(bob_do_grocery_shopping,
                            new Pair<>("Bob", MOVE));
                    //S10_ALICE_ADD_PREF:
                    addToMap(alice_find_place_location,
                            new Pair<>("Alice", "InMind: Alice, at WholeFoods you can find organic food. " +
                                    "Do you want to do the grocery shopping?"));
                    break;

                case S11_ALICE_DO_GROCERY:
                    Log4J.debug(this, "addNextTriggerActions: " + currentStep);
                    addToMap(alice_do_grocery_shopping,
                            new Pair<>("Bob", WANDER));
                    //S11_1_ALICE_MOVE_TO_GROCERY:
                    addToMap(alice_do_grocery_shopping,
                            new Pair<>("Alice", MOVE));
                    //S12_BOB_FIND_BEER:
                    addToMap(bob_find_place_location,
                            new Pair<>("Bob", "InMind: Bob, do you carry your driver license?"));
                    break;

                case S13_BOB_GO_BEER_SHOP:
                    Log4J.debug(this, "addNextTriggerActions: " + currentStep);
                    addToMap(bob_do_beer_shopping,
                            new Pair<>("Bob", "InMind: Bob, there's a beer shop nearby you"));
                    //S13_1_BOB_MOVE_BEER_SHOP:
                    addToMap(bob_do_beer_shopping,
                            new Pair<>("Bob", MOVE));
                    break;

                case S14_BOB_FIND_HOME_DECO:
                    Log4J.debug(this, "addNextTriggerActions: " + currentStep);
                    //S15_BOB_GO_HOME_DECO:
                    addToMap(bob_find_place_location,
                            new Pair<>("Bob", "InMind: Bob, IKEA is on your way home"));
                    //S15_1_BOB_MOVE_HOME_DECO:
                    addToMap(bob_go_home_decor,
                            new Pair<>("Bob", MOVE));
                    break;

                case S16_ALICE_HEADACHE:
                    Log4J.debug(this, "addNextTriggerActions: " + currentStep);
                    //S17_BOB_COUPONS:
                    addToMap(bob_go_pharmacy,
                            new Pair<>("Bob", "InMind: Bob, Rite Aid is closer, but you have some coupons for CVS"));
                    addToMap(bob_go_pharmacy, new Pair<>("Bob", MOVE ));
                    break;

                case S18_BOB_GO_HOME_DECO:
                    Log4J.debug(this, "addNextTriggerActions: " + currentStep);
                    addToMap(bob_go_home_decor, new Pair<>("Bob", MOVE ));
                    addToMap(alice_go_home_decor, new Pair<>("Alice", MOVE ));
                    break;

                case S20_GO_HOME:
                    Log4J.debug(this, "addNextTriggerActions: " + currentStep);
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
            Log4J.error(this, "move: " + currentStep);
            agentModel.move(currentStep);
        }else {
            final String messageId = getMessageId( false );
            actions.add( new Pair<>(delay, () -> {
                Log4J.debug("HarlequinController","InMind action: " + message);
                if (Main.useSimu) agentModel.runStep(0, user, message);
                sendToOrchestrator(user, message, messageId);
                sendToChoreographer(user, message);
            }));
        }
        currentStep = currentStep.increment();
    }


    /**
     * This method returns a message id that has to be send back to the phone
     * @return
     */
    private String getMessageId(boolean invokedFromChoreographer){
        if(currentStep.equals(S9_1_BOB_MOVE_TO_GROCERY) && !invokedFromChoreographer)
            return ORGANIC;
        else if(currentStep.equals(S16_ALICE_HEADACHE) && invokedFromChoreographer)
            return PHARMACY;
        return "";
    }




    public void sendToChoreographer(final String sessionFrom, final String message){
        final String messageId = getMessageId( true );
        CommonUtils.execute(() -> {
            //we need to wait few seconds in order to show synchronized messages in the other phone
            CommonUtils.sleep(2000);
            CrossSessionChoreographer.getInstance().passMessage(sessionFrom, message, messageId);
        });
    }


    private void checkCorrectSequence(int idx) {
        if( seqIdx < correctSeqOfServices.size() &&
                !compositionController.getServices().get(idx).getName().equals( correctSeqOfServices.get(seqIdx) ) ) {
            Log4J.error(this, String.format("Incorrect sequence of behaviors/services. It should be '%s' and it received '%s'",
                    correctSeqOfServices.get(seqIdx), compositionController.getServices().get(idx).getName()));
        }
        seqIdx++;
    }


    private void addEventToState() {
        if( !lastStepExecuted.equals(currentStep) ) {
            if (currentStep.ordinal() < SimuConstants.SimSteps.values().length) {
                switch (currentStep) {
                    case S7_CLOSER_TO_GROCERY:
                        compositionController.addState(Arrays.asList(bob_grocery_shopping_not_done,
                                alice_grocery_shopping_not_done));
                        compositionController.removeState(calculate_nearest_place_required);
                        compositionController.removeState(bob_distance_to_place_provided);
                        compositionController.removeState(alice_distance_to_place_provided);
                        compositionController.addGoal(organize_party_done);
                        setLastStepExecuted();
                        break;
                    case S9_BOB_DO_GROCERY:
                        compositionController.addState(Arrays.asList(bob_is_closer_to_place));
                        setLastStepExecuted();
                        break;

                    case S9_1_BOB_MOVE_TO_GROCERY:
                        compositionController.addState(Arrays.asList(bob_is_closer_to_place));
                        compositionController.removeState(alice_is_closer_to_place);
                        compositionController.removeState(bob_is_closer_to_place);
                        compositionController.removeState(alice_place_location_provided);
                        compositionController.addState(Arrays.asList(alice_place_location_required,
                                alice_place_name_provided));
                        setLastStepExecuted();
                        break;
                    case S10_ALICE_ADD_PREF:
                        compositionController.addState(Arrays.asList(alice_close_to_organic_supermarket,
                                bob_place_location_required));
                        compositionController.removeState(alice_grocery_shopping_required);
                        compositionController.removeState(bob_place_location_provided);
                        setLastStepExecuted();
                        break;
                    case S11_ALICE_DO_GROCERY:
                        compositionController.addState(Arrays.asList(alice_grocery_shopping_required,
                                bob_place_location_provided));
                        setLastStepExecuted();
                        break;
                    case S12_BOB_FIND_BEER:
                        compositionController.removeState(bob_place_location_provided);
                        compositionController.removeState(bob_grocery_shopping_required);
                        compositionController.removeState(alice_grocery_shopping_required);
                        compositionController.addState(Arrays.asList(bob_place_location_required,
                                bob_place_name_provided,
                                bob_beer_shopping_not_done,
                                bob_beer_shopping_required));
                        setLastStepExecuted();
                        break;
                    case S13_BOB_GO_BEER_SHOP:
                        compositionController.removeState(bob_grocery_shopping_required);
                        compositionController.removeState(alice_grocery_shopping_required);
                        compositionController.removeState(bob_grocery_shopping_not_done);
                        compositionController.addState(Arrays.asList(bob_driver_license_provided,
                                bob_is_closer_to_place,
                                bob_beer_shopping_not_done,
                                bob_beer_shopping_required));
                        setLastStepExecuted();
                        break;
                    case S14_BOB_FIND_HOME_DECO:
                        compositionController.removeState(bob_place_location_provided);
                        compositionController.addState(Arrays.asList(bob_place_location_required,
                                bob_place_name_provided));
                        setLastStepExecuted();
                        break;
                    case S15_BOB_GO_HOME_DECO:
                        compositionController.addState(Arrays.asList(bob_is_closer_to_place,
                                bob_buy_decoration_required));
                        setLastStepExecuted();
                        break;
                    case S15_1_BOB_MOVE_HOME_DECO:
                        compositionController.removeState(bob_is_closer_to_place);
                        compositionController.removeState(bob_buy_decoration_required);
                        setLastStepExecuted();
                        break;
                    case S16_ALICE_HEADACHE:
                        compositionController.removeState(bob_place_location_provided);
                        compositionController.removeState(bob_buy_decoration_required);
                        compositionController.addState(Arrays.asList(
                                bob_place_name_provided,
                                bob_somebody_has_headache,
                                bob_no_medication_at_home,
                                bob_is_closer_to_place,
                                bob_has_coupons));
                        setLastStepExecuted();
                        break;
                    case S17_BOB_COUPONS:
                        compositionController.addState(Arrays.asList(bob_has_coupons));
                        setLastStepExecuted();
                        break;
                    case S18_BOB_GO_HOME_DECO:
                        compositionController.addState(Arrays.asList(bob_buy_decoration_required));
                        setLastStepExecuted();
                        break;
                    case S19_ALICE_GO_HOME_DECO:
                        compositionController.addState(Arrays.asList(alice_buy_decoration_required,
                                alice_is_closer_to_place));
                        setLastStepExecuted();
                        break;
                }
            }
        }
    }

    private void setLastStepExecuted() {
        Log4J.debug(this, "addEventToState: " + currentStep);
        lastStepExecuted = currentStep.copy();
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
                    if( !compositionController.getNetwork().isWaitingForUserSelection() ) {
                        stopServiceTriggering.set(true);
                        CommonUtils.sleep(action.fst);
                        stopServiceTriggering.set(false);
                    }
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
        map.put(get_self_location, LocationService.class);
        map.put(find_place_location, FindPlaceService.class);
        map.put(get_distance_to_place, DistanceCalculatorService.class);
        map.put(calculate_nearest_place, WhoIsNearestService.class);
        map.put(share_grocery_list, ShareGroceryListService.class);
        map.put(do_grocery_shopping, DoGroceryShoppingService.class);
        map.put(do_beer_shopping, DoBeerShoppingService.class);
        map.put(go_home_decor, GoHomeDecoService.class);
        map.put(organize_party, OrganizePartyService.class);
        map.put(go_pharmacy, GoPharmacyService.class);
        return map;
    }
}
