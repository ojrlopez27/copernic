package edu.cmu.ubi.simu.copernic.control;


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
import edu.cmu.ubi.simu.copernic.orchestrator.SimuOrchestrator;
import edu.cmu.ubi.simu.copernic.plugin.AgentSimuExecutor;
import edu.cmu.ubi.simu.copernic.services.*;
import edu.cmu.ubi.simu.scenario.demo.Constants;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static edu.cmu.inmind.multiuser.controller.composer.group.User.CLOUD;
import static edu.cmu.ubi.simu.copernic.util.ServiceConstants.*;


/**
 *
 * This is a main and critical component in charge of:
 * 1).  Interacting with the CompositionController (e.g., create services, users and devices; invoke methods from the
 *      BehaviorNetwork class, etc.)
 * 2).  Interacting with the simulation controllers (i.e., AgentModel, World, etc.)
 * 3).  Plot the results of network's spreading activation dynamics
 * 4).  Coordinate the interaction with orchestrators and choreographers
 *
 * Created by oscarr on 5/7/18.
 */
public class CopernicController implements Runnable, VisualizerObserver {
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
    private static CopernicController instance;
    private final static long FREQUENCY_BN_PLOT = TimeUnit.MILLISECONDS.toMillis(1000);
    private final static long DELAY_SERVICE_PROCESSING = TimeUnit.MILLISECONDS.toMillis(2000);
    private static final long DELAY_SEND_TO_DEVICE = TimeUnit.MILLISECONDS.toMillis(2000);

    /***
     * Constructor is private since CopernicController uses the singleton pattern
     */
    private CopernicController(){
        orchestrators = new HashMap<>();
        simuActionsMap = new ConcurrentHashMap<>();
    }

    /**
     * Singleton guarranties to have a single instance of the controller
     * @return
     */
    public static CopernicController getInstance(){
        if(instance == null){
            instance = new CopernicController();
        }
        return instance;
    }


    /* **************************************************************************************************
    **************************** GETTERS, SETTERS AND ADDERS ********************************************
    *****************************************************************************************************/

    /**
     * Sets the simulation world which allows us to stop, pause, resume the simulation
     * @param world
     */
    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * Sets the agent model which allows us to manipulate agents and objects in the world, execute simulation actions,
     * etc.
     * @param agentModel
     */
    public void setAgentModel(AgentSimuExecutor agentModel) {
        this.agentModel = agentModel;
    }

    /**
     * Adds orchestrators involved in the composition. Each orchestrator is listening for messages comming from
     * user's devices.
     * @param sessionId
     * @param orchestrator
     */
    public void addOrchestrator(String sessionId, SimuOrchestrator orchestrator) {
        orchestrators.put(sessionId, orchestrator);
    }

    /**
     * Adds actions to a map for further execution in the future, when particular conditions are met (more specifically,
     * when serviceName matches the service that has been selected by the Behavior Network)
     * @param serviceName
     * @param newAction
     */
    private void addToMap(String serviceName, Action newAction) {
        List<Action> actions = simuActionsMap.get(serviceName);
        if(actions == null) actions = new ArrayList<>();
        actions.add(newAction);
        simuActionsMap.put(serviceName, actions);
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
                // alice has a shopping list file stored on her phone
                return new String[]{alice_has_shopping_list};
        }
        return null;
    }



    /* **************************************************************************************************
    **************************** INITIALIZATION *********************************************************
    *****************************************************************************************************/

    /**
     * CopernicController runs two main processes:
     * 1).  MUF which is in charge of coordinating the communication and session management of the system
     * 2).  Service Composition process carried out by the CompositionController, the BeahviorNetwork, etc.
     */
    public void start(){
        try {
            initMUF();
            initComposition();
            CommonUtils.execute( this ); // here we call the CopernicController.run() method
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Let's initialize the MUF (multiuser framework) in charge of communication, logging handling, events, sessions
     * and more...
     * @throws Exception
     */
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

    /**
     * Let's initialize the composition controller in charge of:
     * 1).  load behaviors (abstract templates for services) from a json
     * 2).  create users involved in the composition
     * 3).  create the corresponding devices for each user
     * 4).  create and install specific services on each device
     * 5).  initialize the behavior network (e.g., initial states and goals)
     * 6).  initialize the visualization module which plots the behavior network's results
     * 7).  initialize ExecuteActions class which is a separate thread that processes events and actions asynchronously
     */
    private void initComposition(){
        // this is our composition controller. We pass the json containing the behaviors (generic templates for services)
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


    /* **************************************************************************************************
    ********************** CHARACTERIZATIONS OF SERVICES ACCORDING TO THE SCENARIO **********************
    *****************************************************************************************************/

    /**
     * It returns the services that must be installed according to the user and kind of device.
     * @param user
     * @param deviceType
     * @return
     */
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

    /**
     * It returns those services that will be installed in the server
     * @return
     */
    private List<String> getServerServices(){
        return Arrays.asList(
                calculate_nearest_place,
                organize_party);
    }

    /**
     * This method returns a map where keys are service names (as in behavior-network.json) and values are
     * classes corresponding to those service names (for further instantiation)
     * @return map
     */
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

    /**
     * This method returns a map of users, devices and a list of services that must be installed on the corresponding
     * user-device.
     * @return
     */
    private Map<Pair<String, Device.TYPES>, List<String>> getMapOfServicesPerUser() {
        Map<Pair<String, Device.TYPES>, List<String>> map = new HashMap<>();
        map.put(new Pair("bob", Device.TYPES.PHONE), getUserServices("bob", Device.TYPES.PHONE));
        map.put(new Pair("bob", Device.TYPES.TABLET), getUserServices("bob", Device.TYPES.TABLET));
        map.put(new Pair("alice", Device.TYPES.PHONE), getUserServices("alice", Device.TYPES.PHONE));
        map.put(new Pair(CLOUD, Device.TYPES.SERVER), getServerServices());
        return map;
    }

    /**
     * This is a util method that removes the device prefix from the service name, e.g., if service name is
     * phone-get-self-location, it will return: get-self-location/
     * @param idx
     * @return
     */
    private String getActivatedServiceName(int idx) {
        String serviceName = compositionController.getServices().get(idx).getName();
        serviceName = serviceName.replace("phone-", "")
                .replace("tablet-", "")
                .replace("smartwatch-", "");
        return serviceName;
    }



    /* **************************************************************************************************
    ******************************************* MAIN EXECUTION ******************************************
    *****************************************************************************************************/

    /**
     * This is the main thread where CopernicController is executed. We add a delay (FREQUENCY_BN_PLOT) so
     * the plot can be shown at the right refresh frequency, otherwise, results will be plotted so fast that
     * would be hard to see the spreading activation dynamics.
     */
    @Override
    public void run() {
        while( !Thread.interrupted() ){
            if( canRun() ) {
                runOneStep();
            }
            CommonUtils.sleep(FREQUENCY_BN_PLOT);
        }
    }

    /**
     * If system is not ready (e.g., is paused, or interaction hasn't started) then the system shouldn't be run.
     * @return
     */
    private boolean canRun() {
        return world != null && !world.isPaused() && !plot.isPaused() && interactionHasStarted;
    }

    /**
     * This method does:
     * 1).  invokes the BehaviorNetwork
     * 2).  check the correct sequence of service activation (according to the scenario),
     * 3).  execute the service on the corresponding device,
     * 4).  add events to the state according to the step, and
     * 5).  plots the results on the chart.
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


    /**
     * This method adds new actions that are:
     * 1).  later processed by the ExecuteAction class or
     * 2).  processed by the AgentModel class as simulation steps (like move, etc.)
     * @param action
     */
    private void processAction(Action action) {
        if(action.getMessage().equals(MOVE)){
            moveCharacterOnSimuWorld(action);
        }else{
            actions.add( action);
        }
    }


    /* **************************************************************************************************
    **************************************** ACTION EXECUTION *******************************************
    *****************************************************************************************************/

    /**
     * This class runs on its own thread. It continuously checks whether there are actions to be executed
     * (actions are added by multiple means: user commands, events, etc.). If there are actions to be
     * executed, then it checks whether it needs to stop for a while (delay) to simulate the time a service
     * processes a request, and then runs the action.
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
            executeActionOnSimuWorld(action);
            sendToOrchestrator(action);
            sendToChoreographer(action);
        });
    }


    /* **************************************************************************************************
    ******************************************** EVENT HANDLERS *****************************************
    *****************************************************************************************************/

    /**
     * This method is called when the spreading activation dynamics are paused or resumed, in that case,
     * the simulation has to be also paused or resumed.
     * @param paused
     */
    @Override
    public void onPausedChanged(boolean paused) {
        world.stopSpinning(paused);
    }

    /***
     * This event is triggered when the orchestrator (SimuOrchestrator), and more specifically the
     * IntentExtractorComponent, receives a message from any of the user's device. This method does:
     * 1).  generates a response that will be sent back to user's device
     * 2).  executes an action (if any) into the simulation world that matches the command and event
     * 3).  add actions to a map for future execution (onNextTriggerActions)
     * 4).  sends an action to the orchestrator (the one that coordinates the interaction with the device where command
     *      was generated)
     * 5).  sends an action to the choreographer (the one that coordinates the interaction with other users and devices)
     * @param sessionId     user's session id
     * @param command       whatever the user says to his/her device
     * @param event         specific event triggered depending on user intention
     * @return
     */
    public void onUserCommandEvent(String sessionId, String command, Constants.Events event) {
        currentEvent = event.copy();
        String response = "";
        switch (event){
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
        onNextTriggerActions(event);

        if ( !response.isEmpty() ) {
            final String finalResponse = response;
            CommonUtils.execute(() -> {
                // we need this delay in order to simulate copernic is processing info and making decisions (since we
                // are not using a real NLU, user intents are recognized almost instantaneously, which is not how it
                // uses to happen)
                CommonUtils.sleep( DELAY_SEND_TO_DEVICE );
                executeCommandOnSimuWorld(sessionId, command, event);
                Action action = new Action( sessionId, finalResponse);
                sendToOrchestrator(action);
                sendToChoreographer(action);
            });
        }
    }


    /**
     * This method adds simulation actions that will be executed later in the future, when the corresponding service
     * (behavior) is activated in runOneStep method. Also, states can be added or removed form the network's working
     * memory
     */
    private void onNextTriggerActions(Constants.Events event) {
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
                    addStates( getInfoFromStorage(event) );
                    addStates( bob_is_willing_to_do_grocery_shopping);
                    //S9_1_BOB_MOVE_TO_GROCERY:
                    addToMap( bob_do_grocery_shopping, new Action(bob, MOVE, () -> {
                        removeStates(bob_is_willing_to_do_grocery_shopping);
                        //it simulates getting a notification from an organic supermarket
                        addStates(alice_close_to_organic_supermarket);
                        runAction( new Action(alice, "InMind: Alice, at WholeFoods you can find organic food. "+
                                "Do you want to do the grocery shopping?", ORGANIC) );
                    }));
                    break;

                case S11_ALICE_DO_GROCERY:
                    addStates(alice_is_willing_to_do_grocery_shopping);
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


    /* **************************************************************************************************
    ********************************* INTERACTION WITH THE SIMULATION WORLD *****************************
    *****************************************************************************************************/

    /**
     * This method executes an action in the simulation world that corresponds to a specific user command (something
     * that user says or does) and an event (an specific enum element that matches user intention).
     * @param userId
     * @param commandMessage
     * @param event
     */
    public void executeCommandOnSimuWorld(String userId, String commandMessage, Constants.Events event) {
        if(Main.useSimu) agentModel.runStep(userId, commandMessage, event);
    }

    /***
     * This method has a pretty similar purpose as executeCommandOnSimuWorld, the only difference is that this method
     * can be scheduled for execution in the future by using a delay
     * @param action
     */
    private void executeActionOnSimuWorld(Action action) {
        if (Main.useSimu) agentModel.runStep(0, action.getUser(), action.getMessage());
    }

    /**
     * This method executes a "move" action on the simulation world, and after this movement, it returns the control to
     * CopernicController (after some delay) by invoking the action callback
     * @param action
     */
    private void moveCharacterOnSimuWorld(Action action) {
        agentModel.move(currentEvent, action.getCallback());
    }


    /* **************************************************************************************************
    ********************************* INTERACTION WITH BEHAVIOR NETWORK ELEMENTS ************************
    *****************************************************************************************************/

    /**
     * Adds one or many states to the network's working memory
     * @param states
     */
    private void addStates(String... states) {
        compositionController.getNetwork().setState( Arrays.asList(states) );
    }

    /***
     * Removes one or many states from the network's working memory
     * @param states
     */
    private void removeStates(String... states){
        compositionController.removeStates(states);
    }

    /**
     * Adds goals to the network's working memory
     * @param goals
     */
    private void addGoals(String... goals) {
        for(String goal : goals) {
            compositionController.addGoal(goal);
        }
    }


    /* **************************************************************************************************
    **************************************** SENDING OUT MESSAGES ***************************************
    *****************************************************************************************************/

    /**
     * It sends an action to the corresponding orchestrator (the one that matches with action.getUser(), which is the
     * sessionId). When the action is send out through the orchestrator, the user will see that action on his/her device
     * @param action
     */
    private void sendToOrchestrator(final Action action) {
        if(action.getWhenToBeTriggered() > 0)
            CommonUtils.sleep(action.getWhenToBeTriggered());
        orchestrators.get(action.getUser()).sendInMindResponse(action.getMessage(), action.getNotificationMessage());
    }

    /**
     * This method sends an action to a choreographer, that is, a component which is in charge of forwarding messages
     * to other users involved in the composition, just for notification purposes.
     * @param action
     */
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


     /* **************************************************************************************************
    ******************************************** VISUALIZATION ******************************************
    *****************************************************************************************************/


    /**
     * This method creates and launches a chart where behavior network dynamics will be plotted
     * @param network
     * @return
     */
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

    /**
     * This method refreshes the values plotted in the chart (e.g., activation level of each service, thresholds, etc).
     */
    private void refreshPlot() {
        plot.setDataset(compositionController.getNormalizedActivations(),
                compositionController.getThreshold(),
                compositionController.getBehActivated(),
                compositionController.getActivationBeh(),
                compositionController.isExecutable());
        //we need to force sync here, not before, otherwise winner service is not plotted on chart
        compositionController.getNetwork().shouldWaitForSync(true);
    }

}

