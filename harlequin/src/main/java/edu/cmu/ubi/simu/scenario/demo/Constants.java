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

import de.nec.nle.siafu.types.*;

import java.util.ArrayList;

/**
 * A list of the constants used by this simulation. None of this is strictly
 * needed, but it makes referring to certain values easier and less error
 * prone.
 * 
 * @author Miquel Martin
 */
public class Constants {
	/**
	 * Probability that the agent hesitates right before stepping into a
	 * building.
	 */
	public static final float P_HESITATE = 0.9f;

	/** Number of minutes in an hour. Ahem. */
	public static final double MIN_PER_HOUR = 60d;

	/** Variance of the gaussian traffic distribution. */
	public static final int TRAFFIC_VARIANCE = 20;

	/** Mean of the gaussian traffic distribution. */
	public static final int TRAFFIC_MEAN = 14;

	/** Amplitude of the gaussian traffic distribution. */
	public static final double TRAFFIC_AMPLITUDE = 0.001d;

	/** Columns between Ralf and Andy's start. */
	public static final int RALF_ANDY_DIST_J = 7;

	/** Rows between Ralf and Andy's start. */
	public static final int RALF_ANDY_DIST_I = 15;

	/** Car speed. */
	public static final int CAR_SPEED = 5;

	/**
	 * Probability that the agent decides to move by car.
	 */
	public static final float P_GO_BY_CAR = 0.7f;

	/**
	 * Probability that the agent leaves his place and goes to another
	 * building.
	 */
	public static final float P_CHANGE_BUILDING = 0.3f;


	/**
	 * In the mode, the simulation will pause at the demo events. If you are
	 * going to run this simulation without a GUI, remember to disable this,
	 * to keep the simulation from pausing.
	 */
	public static final boolean VIDEO_MODE = false;

	/** Events. */
	public enum Events {

		S0_BOB_STARTS,

        S1_ALICE_LOCATION,

        S2_BOB_LOCATION,

        S3_ALICE_FIND_GROCERY,

        S4_ALICE_DIST_GROCERY,

        S5_BOB_FIND_GROCERY,

        S6_BOB_DIST_GROCERY,

        S7_CLOSER_TO_GROCERY,

        S8_ALICE_SHARE_SHOP_LIST,

        S9_BOB_DO_GROCERY,

        S9_1_BOB_MOVE_TO_GROCERY,

        S10_ALICE_ADD_PREF,

        S11_ALICE_DO_GROCERY,

        S11_1_ALICE_MOVE_TO_GROCERY,

        S12_BOB_FIND_BEER,

		S12_1_INTERMEDIATE, //let's wait for the next state

        S13_BOB_GO_BEER_SHOP,

        S13_1_BOB_MOVE_BEER_SHOP,

        S13_2_ALICE_AT_SUPERMARKET,

        S14_BOB_FIND_HOME_DECO,

        S15_BOB_GO_HOME_DECO,

        S15_1_BOB_MOVE_HOME_DECO,

        S16_ALICE_HEADACHE,

        S17_BOB_COUPONS,

        S17_1_INTERMEDIATE, //let's wait for the next state

        S18_BOB_GO_HOME_DECO,

        S19_ALICE_GO_HOME_DECO,

        S20_GO_HOME,

        S21_AT_HOME;

		public Events copy(){
			return values()[this.ordinal()];
		}

//		public Events increment(){
//			return values()[this.ordinal() + 1];
//		}
	}

	/** The period in which boats leave port. */
	public static final TimePeriod ANCHORED_BOAT_LEAVE_TIME =
			new TimePeriod(new EasyTime(8, 0), new EasyTime(13, 0));

	/** The period in which boats arrive at port. */
	public static final TimePeriod ANCHORED_BOAT_ARRIVAL_PERIOD =
			new TimePeriod(new EasyTime(20, 0), new EasyTime(1, 0));

	/** The time of the race. Notice that it shifts from day to day! */
	public static final EasyTime RACE_TIME = new EasyTime(17, 0);

	/** The amount of boats in the race. */
	public static final int AMOUNT_RACING_BOAT = 5;

	/** The amonut of boats that anchor at night. */
	public static final int AMOUNT_ANCHORING_BOAT = 5;

	/**
	 * Separation Bob-Alice at cinema. Decrease according to cultural
	 * limits.
	 */
	public static final int ALICE_SEPARATION = 5;

	/** Probability that a boat anchors at the docks, after nightfall. */
	public static final float P_BOAT_ANCHOR = 0.02f;

	/** Probability that a boat departs the docks, after sunrise. */
	public static final float P_BOAT_LEAVE = 0.02f;

	/** Maximum race start delay. */
	public static final int RACE_DELAY_MIN = 20;

	/** Speed of a racing boat. */
	public static final int BOAT_RACING_SPEED = 7;

    public static final EasyTime S0_TIME = new EasyTime(13, 55);
	/** The time at which Alice location is identified */
	public static final EasyTime S1_TIME = new EasyTime( getIncTime(5, S0_TIME) );
	/** The time at which Bob location is identified */
	public static final EasyTime S2_TIME = new EasyTime(getIncTime(5, S1_TIME) );
	/** The time at which find grocery close to alice */
	public static final EasyTime S3_TIME = new EasyTime(getIncTime(5, S2_TIME) );
	/** The time at which distance to grocery from alice */
	public static final EasyTime S4_TIME = new EasyTime(getIncTime(5, S3_TIME) );
	/** The time at which find grocery close to bob */
	public static final EasyTime S5_TIME = new EasyTime(getIncTime(5, S4_TIME) );
	/** The time at which distance to grocery from bob */
	public static final EasyTime S6_TIME = new EasyTime(getIncTime(5, S5_TIME) );
    /** The time at which who is closest one? bob */
	public static final EasyTime S7_TIME = new EasyTime(getIncTime(5, S6_TIME) );
    /** The time at which alice shares the grocery shopping list */
	public static final EasyTime S8_TIME = new EasyTime(getIncTime(5, S7_TIME) );
	/** The time at which Bob: ok, I will go to the grocery store */
	public static final EasyTime S9_TIME = new EasyTime(getIncTime(5, S8_TIME) );
	/** **/
    public static final EasyTime S9_1_TIME = new EasyTime(getIncTime(5, S9_TIME) );
	/** The time at which from Alice's preferences, she prefers organic food, a new condition is added to the state. There is an organic supermarket close to Alice */
	public static final EasyTime S10_TIME = new EasyTime(getIncTime(15, S9_1_TIME) );
	/** The time at which Alice: ok, so I will do the grocery shopping */
	public static final EasyTime S11_TIME = new EasyTime(getIncTime(5, S10_TIME) );
    public static final EasyTime S11_1_TIME = new EasyTime(getIncTime(5, S11_TIME) );
	/** The time at which Bob has his id and he is not doing any task. bob location is identified and find beer shop close */
	public static final EasyTime S12_TIME = new EasyTime( getIncTime(5, S11_1_TIME) );
	/** The time at which bob has to go to beer shop */
	public static final EasyTime S13_TIME = new EasyTime(getIncTime(5, S12_TIME) );
    public static final EasyTime S13_1_TIME = new EasyTime(getIncTime(5, S13_TIME) );
	/** The time at which Bob is free now, so next step is to go to a Home Decor. bob location is identified*/
	public static final EasyTime S14_TIME = new EasyTime(getIncTime(80, S13_1_TIME) );
	/** The time at which System recommends a home deco close to house */
	public static final EasyTime S15_TIME = new EasyTime(getIncTime(5, S14_TIME) );
    public static final EasyTime S15_1_TIME = new EasyTime(getIncTime(5, S15_TIME) );
	/** The time at which Alice has a headache, so she asks Bob to stop by a pharmacy first. Bob finds multiple pharmacies on his way */
	public static final EasyTime S16_TIME = new EasyTime(getIncTime(10, S15_1_TIME) );
	/** The time at which Bob have some coupons at CVS, so system recommends to stop by CVS. Go CVS*/
	public static final EasyTime S17_TIME = new EasyTime(getIncTime(5, S16_TIME) );
	/** The time at which Bob goes to Home Deco store */
	public static final EasyTime S18_TIME = new EasyTime(getIncTime(40, S17_TIME) );
	/** The time at which Alice is free, she goes to Home Deco */
	public static final EasyTime S19_TIME = new EasyTime(getIncTime(15, S18_TIME) );
    /** The time at which go home */
    public static final EasyTime S20_TIME = new EasyTime(getIncTime(45, S19_TIME) );


    private static String getIncTime(int incMin, EasyTime ref){
        int hour = ref.getHour();
        int min = ref.getMinute() + incMin;
        if(min >= 60){
            min -= 60;
            hour++;
        }
        return hour + ":" + min;
    }



	/**
	 * Population size, that is, how many agents should inhabit this
	 * simulation.
	 */
	public static final int POPULATION = 150;

	/** A small maximum distance to wander off a main point when wanderng. */
	public static final int SMALL_WANDER = 10;

	/** A big distance to wander off a main point when wanderng. */
	public static final int BIG_WANDER = 20;

	/** Probability that the agent has a car. */
	public static final float PROB_HAS_CAR = 0.3f;

	/** 120min time blur. */
	public static final int TWO_HOUR_BLUR = 120;

	/** 60min time blur. */
	public static final int ONE_HOUR_BLUR = 60;

	/** 30min time blur. */
	public static final int HALF_HOUR_BLUR = 30;

	/**
	 * The names of the fields in each agent object.
	 */
	static class Fields {

		/** The agent's current activity. */
		public static final String ACTIVITY = "Activity";

		/** Agent's age. */
		public static final String AGE = "Age";

		/** Agent's preferred cuisine. */
		public static final String CUISINE = "Cuisine";

		/** Agent's work area. */
		public static final String WORKAREA = "WorkArea";

		/** Agent's gender. */
		public static final String GENDER = "Gender";

		/** Agent's acquisitive level. */
		public static final String ACQUISITIVELEVEL = "AcquisitiveLevel";

		/** Agents prefered music gender. */
		public static final String MUSICGENDER = "MusicGender";

		public static final String CONVERSATION = "Conversation";

	}

	/**
	 * Enumeration of the possible activities agents engage in.
	 */
	public enum Activity implements Publishable {
		/** Walking. */
		WALKING("Walking"),
		/** Working. */
		WAITING("Working");
		/** Human readable desription of the activity. */
		private String description;

		/**
		 * Get the description of the activity.
		 * 
		 * @return a string describing the activity
		 */
		public String toString() {
			return description;
		}

		/**
		 * Build an instance of Activity which keeps a human readable
		 * description for when it's flattened.
		 * 
		 * @param description the humanreadable description of the activity
		 */
		private Activity(final String description) {
			this.description = description;
		}

		/**
		 * Flatten the description of the activity.
		 * 
		 * @return a flatenned text with the description of the activity
		 */
		public FlatData flatten() {
			return new Text(description).flatten();
		}
	}

	/** List of possible cuisine types. */
	public static final ArrayList<Text> CUISINE_TYPES =
			new ArrayList<Text>();

	/** A list with the possible genders, to ensure uniform spelling/form. */
	public static final ArrayList<Text> GENDER_TYPES =
			new ArrayList<Text>();

	/** List of possible languages that an agent speaks. */
	public static final ArrayList<Text> LANGUAGE_TYPES =
			new ArrayList<Text>();

	/** List of possible work areas. */
	public static final ArrayList<Text> WORKAREA_TYPES =
			new ArrayList<Text>();

	/** List of positive acquisitive levels. */
	public static final ArrayList<Text> ACQUISITIVELEVEL_TYPES =
			new ArrayList<Text>();

	/** List of possible music gender types. */
	public static final ArrayList<Text> MUSICGENDER_TYPES =
			new ArrayList<Text>();

	static {
		CUISINE_TYPES.add(new Text("Italian"));
		CUISINE_TYPES.add(new Text("Thai"));
		CUISINE_TYPES.add(new Text("Spanish"));
		CUISINE_TYPES.add(new Text("French"));
		CUISINE_TYPES.add(new Text("African"));
		CUISINE_TYPES.add(new Text("Indian"));
		CUISINE_TYPES.add(new Text("German"));

		GENDER_TYPES.add(new Text("Male"));
		GENDER_TYPES.add(new Text("Female"));

		LANGUAGE_TYPES.add(new Text("Catalan"));
		LANGUAGE_TYPES.add(new Text("Italian"));
		LANGUAGE_TYPES.add(new Text("Spanish"));
		LANGUAGE_TYPES.add(new Text("French"));
		LANGUAGE_TYPES.add(new Text("German"));
		LANGUAGE_TYPES.add(new Text("Suomi"));
		LANGUAGE_TYPES.add(new Text("Swedish"));
		LANGUAGE_TYPES.add(new Text("Czech"));
		LANGUAGE_TYPES.add(new Text("Dutch"));

		WORKAREA_TYPES.add(new Text("engineer"));
		WORKAREA_TYPES.add(new Text("athlete"));
		WORKAREA_TYPES.add(new Text("housewife"));
		WORKAREA_TYPES.add(new Text("carpenter"));
		WORKAREA_TYPES.add(new Text("plumber"));
		WORKAREA_TYPES.add(new Text("shopkeeper"));
		WORKAREA_TYPES.add(new Text("researcher"));
		WORKAREA_TYPES.add(new Text("geek"));
		WORKAREA_TYPES.add(new Text("enologist"));

		ACQUISITIVELEVEL_TYPES.add(new Text("none"));
		ACQUISITIVELEVEL_TYPES.add(new Text("low"));
		ACQUISITIVELEVEL_TYPES.add(new Text("average"));
		ACQUISITIVELEVEL_TYPES.add(new Text("high"));
		ACQUISITIVELEVEL_TYPES.add(new Text("extravagant"));

		MUSICGENDER_TYPES.add(new Text("Rock"));
		MUSICGENDER_TYPES.add(new Text("Classic"));
		MUSICGENDER_TYPES.add(new Text("Dance"));
		MUSICGENDER_TYPES.add(new Text("Folk"));
		MUSICGENDER_TYPES.add(new Text("Electronic"));
	}

}
