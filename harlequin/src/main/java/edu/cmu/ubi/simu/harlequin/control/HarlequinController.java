package edu.cmu.ubi.simu.harlequin.control;


import de.nec.nle.siafu.model.World;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Pair;
import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;
import edu.cmu.inmind.multiuser.controller.composer.bn.CompositionController;
import edu.cmu.inmind.multiuser.controller.composer.devices.Device;
import edu.cmu.inmind.multiuser.controller.composer.services.Service;
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


/**
 * Created by oscarr on 5/7/18.
 */
public class HarlequinController implements Runnable, VisualizerObserver {
    private boolean interactionHasStarted = false;
    private boolean shouldPlot = true;
    private CompositionController compositionController;
    private Constants.Events currentEvent;
    private MultiuserController multiuserController;
    private BNGUIVisualizer plot;
    private AgentSimuExecutor agentModel;
    private Map<String, SimuOrchestrator> orchestrators;
    private World world;
    private ConcurrentHashMap<String, List<Action>> simuActionsMap;
    private ConcurrentLinkedQueue<Action> actions = new ConcurrentLinkedQueue<>();
    private AtomicBoolean stopServiceTriggering = new AtomicBoolean(false);
    private static HarlequinController instance;
    private final static long FREQUENCY_BN_PLOT = TimeUnit.MILLISECONDS.toMillis(1000);
    private final static long DELAY_SERVICE_PROCESSING = TimeUnit.MILLISECONDS.toMillis(2000);
    private final static long DELAY_SEND_TO_CHOREOGRAPHER = TimeUnit.MILLISECONDS.toMillis(2000);
    private static final long DELAY_SEND_TO_DEVICE = 2000;

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
        compositionController.instantiateServices( getMapOfServices(), getMapOfServicesPerUser() );

        // set system/user goals and states
        addStates(bob_party_not_organized,
                alice_party_not_organized,
                bob_grocery_shopping_not_done,
                alice_grocery_shopping_not_done );
        addGoals( grocery_shopping_done, "whatever" ); // "organize_party_done"
        // let's extract xxx-required preconditions
        compositionController.endMeansAnalysis();

        // create the gui
        if(shouldPlot){
            plot = createGUI( compositionController.getNetwork() );
            plot.addObserver(this);
        }
        compositionController.getNetwork().shouldWaitForUserSelection(true);
        compositionController.getNetwork().shouldWaitForSync(true);

        CommonUtils.execute( new ExecuteActions() );
    }


    private List<String> getUserServices(String user, Device.TYPES deviceType){
        if(user.equals("bob") && deviceType.equals(Device.TYPES.TABLET))
            return Arrays.asList(get_self_location);
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
        return world != null && !world.isPaused() && !plot.isPaused() && interactionHasStarted;
    }


    /**
     * This method invokes the BehaviorNetwork, check the correct sequence (according to the scenario),
     * execute the service on the corresponding device, add events to the state according to the step, and
     * plots the results on the chart.
     */
    public void runOneStep() {
        if( !stopServiceTriggering.get() ) {
            compositionController.updateDeviceState();
            int idx = compositionController.selectService()[0];
            Log4J.debug(this, "States: " +
                    Arrays.toString(compositionController.getNetwork().getState().toArray()));
            if (compositionController.isExecutable()) {
                compositionController.executeService(idx);
                String serviceName = getActivatedServiceName(idx);
                Log4J.error(this, "Executing service: " + serviceName);
                List<Action> actions = simuActionsMap.remove(serviceName);
                if (actions != null) {
                    for (Action action : actions) {
                        processAction(action);
                    }
                }
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

    public void addAgentModel(AgentSimuExecutor agentModel) {
        this.agentModel = agentModel;
    }

    public void addOrchestrator(String sessionId, SimuOrchestrator orchestrator) {
        orchestrators.put(sessionId, orchestrator);
    }

    private void sendToOrchestrator(final Action action) {
        if(action.getWhenToBeTriggered() > 0)
            CommonUtils.sleep(action.getWhenToBeTriggered());
        orchestrators.get(action.getUser()).sendInMindResponse(action.getMessage(), action.getNotificationMessage());
    }

    public String executeEvent(String sessionId, String command, Constants.Events event) {
        currentEvent = event.copy();
        String response = "";
        switch (event){
            case S0_BOB_STARTS:
                response = "Definitively, I can help you guys with that! let me check available services...";
                interactionHasStarted = true;
                break;

            case S9_BOB_DO_GROCERY:
                response = "Sounds perfect Bob!";
                addStates(getInfoFromStorage(event));
                addStates(bob_is_willing_to_do_grocery_shopping);
                break;

            case S11_ALICE_DO_GROCERY:
                response = "A little change in plans, thanks Alice!";
                addStates(alice_is_willing_to_do_grocery_shopping);
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

            case S15_BOB_GO_HOME_DECO:
                response = "Ok Bob, you can meet Alice there";
                break;

            case S16_ALICE_HEADACHE:
                response = "I'm sorry to hear that Alice, let me look for some pharmacies near Bob";
                break;

            case S19_BOB_GO_HOME_DECO:
                response = "Ok, now you guys can meet at IKEA";
                break;

            case S21_GO_HOME:
                response = "Thanks for letting me know";
                break;
        }
        executeUserCommand(sessionId, command, event);
        addNextTriggerActions(event);

        if ( !response.isEmpty() ) {
            final String finalResponse = response;
            CommonUtils.execute(() -> {
                CommonUtils.sleep( DELAY_SEND_TO_DEVICE );
                Action action = new Action( sessionId, finalResponse);
                sendToOrchestrator(action);
                sendToChoreographer(action);
            });
        }
        return response;
    }

    /**
     * This method simulates retrieving information from the local/remote storage, e.g.,
     * a DB, the phone's SD card, etc.
     * @param event
     * @return
     */
    private String[] getInfoFromStorage(Constants.Events event){
        switch (event){
            case S9_BOB_DO_GROCERY:
                return new String[]{alice_has_shopping_list};
        }
        return null;
    }


    private void addStates(String... states) {
        compositionController.getNetwork().setState( Arrays.asList(states) );
    }

    private void removeStates(String... states){
        compositionController.removeStates(states);
    }

    private void addGoals(String... goals) {
        for(String goal : goals) {
            compositionController.addGoal(goal);
        }
    }

    /**
     * This is called externally from a PlugabbleComponent or an Orchestrator when a user action/command
     * happens.
     * @param sessionId
     * @param command
     * @param event
     */
    public void executeUserCommand(String sessionId, String command, Constants.Events event) {
        if(Main.useSimu) agentModel.runStep(sessionId, command, event);
    }


    /**
     * This simulation actions will be executed later in the future, when the corresponding service (behavior)
     * is activated in runOneStep method.
     */
    private void addNextTriggerActions(Constants.Events event) {
        try {
            switch (event) {
                case S0_BOB_STARTS:
                    //S1_ALICE_LOCATION:
                    addToMap(alice_get_self_location,
                            new Action(alice, "InMind: Alice, I got your current location"));
                    //S2_BOB_LOCATION:
                    addToMap(bob_get_self_location,
                            new Action(bob, "InMind: Bob, I got your current location"));

                    //S3_ALICE_FIND_GROCERY:
                    addToMap(alice_find_place_location,
                            new Action( alice,"InMind: Searching for supermarkets near Alice..."));
                    //S4_ALICE_DIST_GROCERY:
                    addToMap(alice_get_distance_to_place,
                            new Action(alice,"InMind: Alice, WholeFoods is 3.6 miles away"));
                    //S5_BOB_FIND_GROCERY:
                    addToMap(bob_find_place_location,
                            new Action( bob, "InMind: Searching for supermarkets near Bob..."));
                    //S6_BOB_DIST_GROCERY:
                    addToMap(bob_get_distance_to_place,
                            new Action(bob, "InMind: Bob, Target is 2 miles away"));
                    //S7_CLOSER_TO_GROCERY:
                    addToMap(cloud_calculate_nearest_place,
                            new Action(bob, "InMind: Bob, you are closer than Alice. " +
                                    "Do you want to do the grocery shopping?"));
                    //S8_ALICE_SHARE_SHOP_LIST:
                    addToMap(alice_share_grocery_list,
                            new Action(alice, "InMind: Sharing shopping list from Alice"));
                    break;

                case S9_BOB_DO_GROCERY:
                    //S9_1_BOB_MOVE_TO_GROCERY:
                    addToMap(bob_do_grocery_shopping, new Action(bob, MOVE, () -> {
                        removeStates(bob_is_willing_to_do_grocery_shopping);
                        //it simulates getting a notification from an organic supermarket
                        addStates(alice_close_to_organic_supermarket);
                        runAction( new Action(alice, "InMind: Alice, at WholeFoods you can find organic food. "+
                                "Do you want to do the grocery shopping?", ORGANIC) );
                    }));
                    break;

                case S11_ALICE_DO_GROCERY:
                    //S11_1_ALICE_MOVE_TO_GROCERY:
                    addToMap(alice_do_grocery_shopping, new Action(alice, MOVE, () -> {
                        removeStates(bob_grocery_shopping_required,
                                bob_is_closer_to_place,
                                bob_is_willing_to_do_grocery_shopping,
                                bob_place_location_provided);
                        addStates(bob_place_location_required,
                                bob_place_name_provided,
                                bob_self_location_provided); // beer shop
                        addGoals(organize_party_done);
                    }));
                    //S12_BOB_FIND_BEER:
                    addToMap(bob_find_place_location,
                            new Action(bob, "InMind: Bob, do you carry your driver license?"));
                    break;

                case S13_BOB_GO_BEER_SHOP:
                    addStates(bob_driver_license_provided,
                        bob_is_closer_to_place,
                        bob_beer_shopping_not_done,
                        bob_beer_shopping_required);
                    addToMap(bob_do_beer_shopping,
                            new Action(bob, "InMind: Bob, there's a beer shop nearby you"));
                    //S13_1_BOB_MOVE_BEER_SHOP:
                    addToMap(bob_do_beer_shopping, new Action(bob, MOVE));
                    break;

                case S14_BOB_FIND_HOME_DECO:
                    //S15_BOB_GO_HOME_DECO:
                    addStates(bob_place_location_required);
                    removeStates(bob_place_location_provided);
                    addToMap(bob_find_place_location, new Action(bob, "InMind: Bob, IKEA is on your way home"));
                    break;

                case S15_BOB_GO_HOME_DECO:
                    addStates(bob_buy_decoration_required, bob_is_closer_to_place);
                    addToMap(bob_go_home_decor, new Action(bob, MOVE));
                    break;

                case S16_ALICE_HEADACHE:
                    //it simulates getting a notification from an CVS pharmacy
                    //S17_BOB_COUPONS:
                    runAction( new Action(bob, "InMind: Bob, Rite Aid is closer, but you have some coupons " +
                            "for CVS", PHARMACY, 4000) );
                    break;

                case S18_BOB_GO_PHARMACY:
                    removeStates(bob_place_location_provided, bob_buy_decoration_required);
                    addStates( bob_place_name_provided,
                            bob_somebody_has_headache,
                            bob_no_medication_at_home,
                            bob_is_closer_to_place,
                            bob_has_coupons);
                    addToMap(bob_go_pharmacy, new Action(bob, MOVE ));
                    break;

                case S19_BOB_GO_HOME_DECO:
                    addStates(bob_buy_decoration_required,
                            alice_buy_decoration_required,
                            alice_is_closer_to_place);
                    addToMap(bob_go_home_decor, new Action(bob, MOVE ));
                    addToMap(alice_go_home_decor, new Action(alice, MOVE ));
                    break;

                case S21_GO_HOME:
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void addToMap(String serviceName, Action newAction) {
        List<Action> actions = simuActionsMap.get(serviceName);
        if(actions == null) actions = new ArrayList<>();
        actions.add(newAction);
        simuActionsMap.put(serviceName, actions);
    }

    /**
     * This method adds new actions that are later processed by the ExecuteAction class or calls the AgentModel
     * class to run simulation steps (like move, etc.)
     * @param action
     */
    private void processAction(Action action) {
        if(action.getMessage().equals(MOVE)){
            agentModel.move(currentEvent, action.getCallback());
        }else{
            actions.add( action);
        }
    }


    public void sendToChoreographer(Action action){
        sendToChoreographer(action.getUser(), action.getMessage());
    }

    /**
     * All messages that InMind shows on Bob's devices have to be shown on Alice's devices and vice versa
     * @param sessionFrom
     * @param message
     */
    public void sendToChoreographer(final String sessionFrom, final String message){
        if( message != null && !message.isEmpty() ) {
            CommonUtils.execute(() -> {
                CrossSessionChoreographer.getInstance().passMessage(sessionFrom, message, null);
            });
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


    /**
     * This class runs a step on the simulation and send messages to orchestrator so it
     * can send it back to the device (client). We use a delay to simulate the time services
     * spend doing some processing
     */
    class ExecuteActions implements Runnable{
        @Override
        public void run() {
            while(!Thread.interrupted()){
                if(!actions.isEmpty() && canRun()){
                    Action action = actions.poll();
                    if( !compositionController.getNetwork().isWaitingForUserSelection() ){
                        stopServiceTriggering.set(true);
                        CommonUtils.sleep(DELAY_SERVICE_PROCESSING);
                        stopServiceTriggering.set(false);
                    }
                    runAction(action);
                }
                CommonUtils.sleep(50);
            }
        }
    }

    /**
     * This method:
     * 1. executes a specific action on the simulation model
     * 2. sends the action to the orchestrator (i.e., the device corresponding to action.getUser())
     * 3. sends the actions to the choreographer (i.e., other users and devices involved in the plan composition)
     * @param action
     */
    private void runAction(Action action) {
        CommonUtils.execute(() -> {
            if( action.getWhenToBeTriggered() > 0 )
                CommonUtils.sleep(action.getWhenToBeTriggered());
            if (Main.useSimu) agentModel.runStep(0, action.getUser(), action.getMessage());
            sendToOrchestrator(action);
            sendToChoreographer(action);
        });
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

    private Map<Pair<String, Device.TYPES>, List<String>> getMapOfServicesPerUser() {
        Map<Pair<String, Device.TYPES>, List<String>> map = new HashMap<>();
        map.put(new Pair("bob", Device.TYPES.PHONE), getUserServices("bob", Device.TYPES.PHONE));
        map.put(new Pair("bob", Device.TYPES.TABLET), getUserServices("bob", Device.TYPES.TABLET));
        map.put(new Pair("alice", Device.TYPES.PHONE), getUserServices("alice", Device.TYPES.PHONE));
        map.put(new Pair(CLOUD, Device.TYPES.SERVER), getServerServices());
        return map;
    }
}
