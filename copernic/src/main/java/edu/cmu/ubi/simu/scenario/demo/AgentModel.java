/*
 * Copyright NEC Europe Ltd. 2006-2007
 * 
 * This file is part of the context simulator called Siafu.
 * 
 * Siafu is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * Siafu is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.cmu.ubi.simu.scenario.demo;

import de.nec.nle.siafu.behaviormodels.BaseAgentModel;
import de.nec.nle.siafu.exceptions.*;
import de.nec.nle.siafu.graphics.markers.Marker;
import de.nec.nle.siafu.graphics.markers.SpotMarker;
import de.nec.nle.siafu.model.Agent;
import de.nec.nle.siafu.model.Place;
import de.nec.nle.siafu.model.Position;
import de.nec.nle.siafu.model.World;
import de.nec.nle.siafu.types.EasyTime;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.ubi.simu.copernic.control.ActionCallback;
import edu.cmu.ubi.simu.copernic.control.CopernicController;
import edu.cmu.ubi.simu.copernic.plugin.AgentSimuExecutor;
import edu.cmu.ubi.simu.copernic.util.SimuUtils;

import java.util.*;

import static edu.cmu.ubi.simu.scenario.demo.Constants.*;

/**
 * Behavior of the agents in the Glasgow simulation. Two users, Andy and Ralf
 * stay put, and can be moved around. We connected context-aware applications
 * to their context. There's a number of other agents that move autonomously
 * from one building to another, with a peak traffic at 14h00 and a gaussian
 * distribution.
 * 
 * @author Miquel Martin
 * 
 */
public class AgentModel extends BaseAgentModel implements AgentSimuExecutor {

	/**
	 * Marks the steps in the simulation.
	 */
	private Events event = Events.S0_BOB_STARTS;

	/** The agent playing Alice. */
	private Agent Alice;

	/** The agent playing Bob. */
	private Agent Bob;

	/** Bob's initial position. */
	private Position startBob;

	/** Alice's initial position. */
	private Position startAlice;

	/** Bob and Alice's markets of choice. */
	private Place target, wholefoods;

	/** Bob and Alice's pharmacies of choice. */
	private Place rite_aid, cvs;

	/** Bob and Alice's homedecos of choice. */
	private Place ikea, art_deco;

	/** Bob and Alice's beershops of choice. */
	private Place beershop1, beershop2;

	private Place home;

	/** Now. Refreshed every iteration. */
	private EasyTime now;

	private Marker markAlice;
    private Marker markBob;

    /** when it flips, the balloon's color changes **/
    private boolean flip = false;

    private CopernicController copernicController;



	/**
	 * Instantiate the agent model.
	 * 
	 * @param world the world the agents live in
	 */
	public AgentModel(final World world) {
		super(world);
		copernicController = CopernicController.getInstance();
		copernicController.setAgentModel(this);
		copernicController.setWorld(world);
	}

	/**
	 * A random number generator.
	 */
	private static final Random RAND = new Random();

	/** The amount of people in Glasgow :). */
	private final int population = 50;

	private boolean firstTime = true;

	/**
	 * Create the agents, where most are zombies, except for Ralf and Andy,
	 * who won't move.
	 * 
	 * @return a list with the simulation's agents.
	 */
	public ArrayList<Agent> createAgents() {
        Log4J.info(this,"Creating " + population + " people");
		ArrayList<Agent> people =
				AgentGenerator.createRandomPopulation(population, world);

		for (Agent a : people) {
			//a.set(Constants.Fields.ACTIVITY, Constants.Activity.WAITING);
			a.setSpeed(1 + RAND.nextInt(2));
			a.setVisible(false);
		}

		// Kidnap two guys and make them our special guys
		Alice = people.get(0);
		Bob = people.get(1);

		try {
            beershop1 = world.getPlaceByName("BeerShop-666.681");
            beershop2 = world.getPlaceByName("BeerShop-213.584");
            target = world.getPlaceByName("Market-625.567");
            wholefoods = world.getPlaceByName("Market-259.686");
            rite_aid = world.getPlaceByName("Pharmacy-488.238");
            cvs = world.getPlaceByName("Pharmacy-243.442");
            ikea = world.getPlaceByName("HomeDeco-206.210");
            art_deco = world.getPlaceByName("HomeDeco-281.78");
            home = world.getPlaceByName("Home-26.264");

			startAlice = world.getPlacesOfType("StartAlice").iterator().next().getPos();
			startBob = world.getPlacesOfType("StartBob").iterator().next().getPos();

            markAlice = new SpotMarker(Alice, "#1111CC");
            markBob = new SpotMarker(Alice, "#1111CC");

            Alice.setName("Alice");
            Alice.setPos(startAlice);
            Alice.setImage("HumanYellow");
            Alice.setVisible(true);
            Alice.setSpeed(3);

            Bob.setName("Bob");
            Bob.setPos(startBob);
            Bob.setImage("HumanBlue");
            Bob.setVisible(true);
            Bob.setSpeed(3);

            world.stopSpinning(true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return people;
	}

	/**
	 * Move the user from the current place to another building, with a
	 * certain probability of doing so with a car.
	 * 
	 * @param a the agent that must change buildings
	 */
	protected void changeBuilding(final Agent a) {
		a.setVisible(true);

		if (RAND.nextFloat() < P_GO_BY_CAR) {
			a.setImage("CarGreen");
			a.setSpeed(CAR_SPEED);
		}
		try {
			a.setDestination(world.getRandomPlaceOfType("Building"));
		} catch (PlaceNotFoundException e) {
			throw new RuntimeException(
					"You didn't define Building place types", e);
		}

		a.set(Constants.Fields.ACTIVITY, Constants.Activity.WALKING);
	}

	/**
	 * Move the agents from a building on to the next, with 14h00 being the
	 * busiest hour.
	 * 
	 * @param agents the agents to work on
	 */
	public void doIteration(final Collection<Agent> agents) {
	    if( firstTime ){
	        try{
                world.addMarker( new DoubleLineMarker(Alice, getColor(), new String[]{"I am Alice!"}));
                world.addMarker( new DoubleLineMarker(Bob, getColor(), new String[]{"I am Bob!"}) );
            }catch (Exception e){
	            e.printStackTrace();
            }
	        if(VIDEO_MODE) CommonUtils.sleep(10000);
	        firstTime = false;
        }
		Calendar time = world.getTime();
		now = new EasyTime(time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE));

		for (Agent a : agents) {
			if (!a.isOnAuto() || a.equals(Alice) || a.equals(Bob)) {
				continue; // This guy's being managed by the user interface
			}
			handlePerson(a);
		}

		if (VIDEO_MODE) {
			try {
				switch (event) {
                    case S0_BOB_STARTS:
						basicSimuStep(now.isAfter(S0_TIME), Bob,
                                new String[]{ "Bob: InMind, Alice and I", "are organizing a party..."}, true, true, false);
						break;
					case S1_ALICE_LOCATION:
						basicSimuStep(now.isAfter(S1_TIME), Alice,
                                new String[]{ "InMind: I got your", "current location"});
                        break;

                    case S2_BOB_LOCATION:
                        basicSimuStep(now.isAfter(S2_TIME), Bob,
                                new String[]{ "InMind: I got your current location" } );
                        break;

                    case S3_ALICE_FIND_GROCERY:
                        basicSimuStep(now.isAfter(S3_TIME), Alice,
                                new String[]{ "InMind: Searching", "markets around..."});
                        break;

                    case S4_ALICE_DIST_GROCERY:
                        basicSimuStep(now.isAfter(S4_TIME), Alice,
                                new String[]{ "InMind: Market2", "is close to you"});
                        break;

                    case S5_BOB_FIND_GROCERY:
                        basicSimuStep(now.isAfter(S5_TIME), Bob,
                                new String[]{ "InMind: Searching for markets..." });
                        break;

                    case S6_BOB_DIST_GROCERY:
                        basicSimuStep(now.isAfter(S6_TIME), Bob,
                                new String[]{ "InMind: Market1", "is close to you" });
                        break;

                    case S7_CLOSER_TO_GROCERY:
                        basicSimuStep(now.isAfter(S7_TIME), Bob,
                                new String[]{ "InMind: You are closer than Alice" });
                        break;

                    case S8_ALICE_SHARE_SHOP_LIST:
                        basicSimuStep(now.isAfter(S8_TIME), Alice,
                                new String[]{ "InMind: Sharing", "shopping list" });
                        break;

                    case S9_BOB_DO_GROCERY:
                        basicSimuStep(now.isAfter(S9_TIME), Bob,
                                new String[]{ "Bob: OK, I will do the grocery shopping"});
                        break;

                    case S9_1_BOB_MOVE_TO_GROCERY:
                        movingSimuStep(now.isAfter(S9_1_TIME), Bob, target);
                        break;

                    case S10_ALICE_ADD_PREF:
                        basicSimuStep(now.isAfter(S10_TIME), Alice,
                                new String[]{ "InMind: an organic", "market is nearby" });
                        break;

                    case S11_ALICE_DO_GROCERY:
                        if( basicSimuStep(now.isAfter(S11_TIME), Alice,
                                new String[]{ "Alice: OK, I'll do", "the grocery shop."}) ) {
                            Bob.wanderAround(rite_aid, 10);
                        }
                        break;

                    case S11_1_ALICE_MOVE_TO_GROCERY:
                        movingSimuStep(now.isAfter(S11_1_TIME), Alice, wholefoods);
                        break;

                    case S12_BOB_FIND_BEER:
                        basicSimuStep(now.isAfter(S12_TIME), Bob,
                                new String[]{ "InMind: Did you carry your driver license?"});
                        break;

                    case S13_BOB_GO_BEER_SHOP:
                        basicSimuStep(now.isAfter(S13_TIME), Bob,
                                new String[]{ "InMind: there's a beer shop nearby here" });
                        break;

                    case S13_1_BOB_MOVE_BEER_SHOP:
                        movingSimuStep(now.isAfter(S13_1_TIME), Bob, beershop1);

                    case S13_2_ALICE_AT_SUPERMARKET:
                        basicSimuStep(Alice.isAtDestination(), Alice,
                                new String[]{ "Alice: I'm at the super market" }); //, false, true, false
                        break;

                    case S14_BOB_FIND_HOME_DECO:
                        basicSimuStep(now.isAfter(S14_TIME), Bob,
                                new String[]{ "Bob: I'm done, what's next?" });
                        break;

                    case S15_BOB_GO_HOME_DECO:
                        basicSimuStep(now.isAfter(S15_TIME), Bob,
                                new String[]{ "InMind: there's a HomeDeco", "on your way home"});
                        break;

                    case S15_1_BOB_MOVE_HOME_DECO:
                        movingSimuStep(now.isAfter(S15_1_TIME), Bob, ikea);
                        break;

                    case S16_ALICE_HEADACHE:
                        if( basicSimuStep(now.isAfter(S16_TIME), Bob,
                                new String[]{ "Alice: could you stop", "by a pharmacy?" }) ) {
                            Bob.wanderAround(target, 1);
                        }
                        break;

                    case S17_BOB_COUPONS:
                        if( basicSimuStep(now.isAfter(S17_TIME), Bob,
                                new String[]{"InMind: pharmacy 1 is closer,", "but you have coupons for pharmacy 2"} ) ) {
                            Bob.setDestination(cvs);
                        }
                        break;

                    case S19_BOB_GO_HOME_DECO:
                        if( basicSimuStep(now.isAfter(S18_TIME) && Bob.isAtDestination(), Bob,
                                new String[]{ "InMind: you can meet with Alice at Home Deco 1"}) ) {
                            Bob.setDestination(ikea);
                        }
                        break;

                    case S20_ALICE_GO_HOME_DECO:
                        if( basicSimuStep(now.isAfter(S19_TIME), Alice,
                                new String[]{ "InMind: you can meet with Bob at Home Deco 1"}) ) {
                            Alice.setDestination(ikea);
                        }
                        break;

                    case S21_GO_HOME:
                        if( basicSimuStep(now.isAfter(S20_TIME), Bob, new String[]{ "Alice: I'm here" }) ){
                            Alice.setDestination(home);
                            Bob.setDestination(home);
                        }
                        break;
					default:
						throw new RuntimeException("Invalid simulation state");
				}
			} catch (Exception e) {
                Log4J.error(this,"GUI not ready");
			}
		}
	}

    private boolean movingSimuStep(boolean conditionMet, Agent user, Place destination) {
        try{
            if( conditionMet ){
                //world.stopSpinning(true);
                Marker marker = new SpotMarker(user, "#da270c");
                world.addMarker(marker);
                user.setDestination(destination);
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /**
     *
     * @param conditionMet
     * @param user
     * @param text
     * @param params params[0] =    whether to clean up secondary user's (otherUser) caption
     *               params[1] =    whether to stop spinning the world simulation
     *               params[2] =    whether to run BN GUI
     */
    private boolean basicSimuStep(boolean conditionMet, Agent user, String[] text, boolean... params) {
	    try {
            if (conditionMet) {
                world.unMarkAll();
                Agent otherUser = user.equals(Alice) ? Bob : Alice;
                //world.stopSpinning(true);
                Marker mainMarker = new DoubleLineMarker(user, getColor(), text);
                world.addMarker(mainMarker);
                if( params.length == 0 || params[0] ) {
                    Marker secMarker = new SpotMarker(otherUser, "#2d2973"); //  "#da270c"
                    world.addMarker(secMarker);
                }
                if(params.length == 0 || (params.length >= 3 && params[2]))
                    copernicController.runOneStep();
                //event = event.increment();
                // only for recording the video: if(VIDEO_MODE) CommonUtils.sleep(4000);
                //world.stopSpinning(false);
                return true;
            }
        }catch (Exception e){
	        e.printStackTrace();
        }
        return false;
    }

    private String getColor() {
        String color;
        if( flip ) color = "#2d2973"; //#ffa500 orange.   "#2d2973"; //purple
        else color = "#da270c"; //red
        flip = !flip;
        return color;
    }


    /**
	 * Handle the "zombie" people in the simulation, by walking them from one
	 * building to the next. The traffic distribution is gaussian.
	 *
	 * @param a the agent to handle
	 */
	private void handlePerson(final Agent a) {
		switch ((Constants.Activity) a.get(Constants.Fields.ACTIVITY)) {
			case WAITING:
				double t =
						now.getHour()
								+ ((double) now.getMinute() / MIN_PER_HOUR);
				double gaussianThreshold =
						TRAFFIC_AMPLITUDE
								* Math.exp(-Math.pow((t - TRAFFIC_MEAN), 2)
								/ TRAFFIC_VARIANCE);

				if (RAND.nextDouble() < gaussianThreshold) {
					changeBuilding(a);
					// p.setAppearance("HumanMagenta");
				}
				break;

			case WALKING:
				if (a.isAtDestination()) {
					if (!(RAND.nextFloat() < P_HESITATE)) {
						// Hesitate on coming in
						a.setVisible(false);
						a.set(Constants.Fields.ACTIVITY, Constants.Activity.WAITING);
					}
					a.setSpeed(1 + RAND.nextInt(2));
				}
				break;
			default:
				throw new RuntimeException("Unable to handle activity "
						+ a.get(Constants.Fields.ACTIVITY));
		}
	}

    /**
     * This method is called only by CopernicController. It executes one step of the simulation immediately after
     * being called.
     * @param userId
     * @param message
     * @param event
     */
    @Override
    public void runStep(String userId, String message, Events event) {
        Agent agent = userId.equalsIgnoreCase("Bob")? Bob : Alice;
        String[] messages = SimuUtils.breakIntoMessages(message);
        try{
            basicSimuStep(true, agent, messages, true, true, false);
        } catch (Exception e) {
            Log4J.error(this,"GUI not ready");
        }
    }

    /**
     * This method is called only by CopernicController and executes one step of the simulation after a delay (if any)
     * to sync messages that are shown in the simulation frame. Also, it sends messages from one user to another using
     * the sendToOrchestrator method.
     * @param delay
     * @param userId
     * @param message
     */
    @Override
    public void runStep(long delay, String userId, String message) {
        CommonUtils.sleep(delay);
        Agent agent = userId.equalsIgnoreCase("Bob")? Bob : Alice;
        basicSimuStep(true, agent, SimuUtils.breakIntoMessages(message), true, true, false);
    }

    /***
     * It executes a "move" action on the simulation world. Some of these actions may require to do something after
     * some time has passed (a delay) so a callback is provided and invoked when needed.
     * @param event
     * @param callback
     */
    @Override
    public void move(Events event, ActionCallback callback) {
        switch (event) {
            case S9_BOB_DO_GROCERY:
                moveBobToGrocery(callback);
                break;

            case S9_1_BOB_MOVE_TO_GROCERY:
                moveBobToGrocery(callback);
                break;

            case S11_ALICE_DO_GROCERY:
                moveAliceToGrocery(callback);
                break;

            case S11_1_ALICE_MOVE_TO_GROCERY:
                moveAliceToGrocery(callback);
                break;

            case S13_BOB_GO_BEER_SHOP:
                movingSimuStep(true, Bob, beershop1);
                break;

            case S15_BOB_GO_HOME_DECO:
                movingSimuStep(true, Bob, ikea);
                CommonUtils.execute(() -> {
                    CommonUtils.sleep(2000);
                    Bob.wanderAround(target, 1);
                });
                break;

            case S17_BOB_COUPONS:
                Bob.wanderAround(target, 1);
                break;

            case S18_BOB_GO_PHARMACY:
                movingSimuStep(true, Bob, cvs);
                break;

            case S19_BOB_GO_HOME_DECO:
                movingSimuStep(true, Bob, ikea);
                CommonUtils.execute(() -> {
                    CommonUtils.sleep(2000);
                    movingSimuStep(true, Alice, ikea);
                    CommonUtils.sleep(15000);
                    movingSimuStep(true, Alice, home);
                    movingSimuStep(true, Bob, home);
                });
                break;
            default:
                throw new RuntimeException("Invalid simulation state: " + event);
        }
    }

    /***
     * Executes a movement towards Target supermarket
     * @param callback
     */
    private void moveBobToGrocery(ActionCallback callback) {
        movingSimuStep(true, Bob, target);
        CommonUtils.execute(() -> {
            CommonUtils.sleep(2500);
            callback.execute();
            Bob.wanderAround(rite_aid, 10);
        });
    }

    /***
     * Executes a movement towards WholeFoods supermarket
     * @param callback
     */
    private void moveAliceToGrocery(ActionCallback callback) {
        callback.execute();
        movingSimuStep(true, Alice, wholefoods);
    }
}
