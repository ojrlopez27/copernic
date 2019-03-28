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

import de.nec.nle.siafu.exceptions.PlaceNotFoundException;
import de.nec.nle.siafu.model.Agent;
import de.nec.nle.siafu.model.World;
import de.nec.nle.siafu.types.Text;

import java.util.ArrayList;
import java.util.Random;

/**
 * Utility class to generate agents fitting the Glasgow scenario.
 * 
 * @author Miquel Martin
 */
public final class AgentGenerator {

	/** Maximum agent's age. */
	private static final int MAX_AGE = 60;

	/** Minimum agent's age. */
	private static final int MIN_AGE = 10;

	/** A random number generator. */
	private static Random rand = new Random();

	/** Prevent instantiation of this class. */
	private AgentGenerator() {
	}

	/**
	 * Create a population made up of <code>size</code> random agents.
	 * 
	 * @param size
	 *            the population size
	 * @param world
	 *            the world object of the whole simulation
	 * @return an ArrayList with the collection of agents
	 */
	public static ArrayList<Agent> createRandomPopulation(final int size,
			final World world) {
		ArrayList<Agent> population = new ArrayList<>(size);

		for (int i = 0; i < size; i++) {
			population.add(createRandomAgent(world));
		}

		return population;
	}

	/**
	 * Create a random agent that fits the Glasgow simulation.
	 * 
	 * @param world
	 *            the world the agent will live in
	 * @return the new agent
	 */
	public static Agent createRandomAgent(final World world) {
		Agent a;
		try {
			a = new Agent(world.getRandomPlaceOfType("Building").getPos(),
					"HumanGreen", world);
		} catch (PlaceNotFoundException e) {
			throw new RuntimeException(
					"You didn't define the \"Building\" type of places", e);
		}

//		a.set(Constants.Fields.AGE, new IntegerNumber(MIN_AGE + rand.nextInt(MAX_AGE)));
//		a.set(Constants.Fields.CUISINE, getRandomType(Constants.CUISINE_TYPES));
//		a.set(Constants.Fields.WORKAREA, getRandomType(Constants.WORKAREA_TYPES));
//		a.set(Constants.Fields.ACQUISITIVELEVEL, getRandomType(Constants.ACQUISITIVELEVEL_TYPES));
//		a.set(Constants.Fields.MUSICGENDER, getRandomType(Constants.MUSICGENDER_TYPES));
//		a.set(Constants.Fields.GENDER, getRandomType(Constants.GENDER_TYPES));
//		if(!a.getName().equals("Agnes") && !a.getName().equals("Tonyo")) {
            a.set(Constants.Fields.ACTIVITY, Constants.Activity.WAITING);
//        }else {
            a.set(Constants.Fields.CONVERSATION, new Text(""));
//        }
		return a;
	}

	/**
	 * Return a random element from the given array list.
	 * 
	 * @param types
	 *            the ArrayList containing the types
	 * @return the randomly chosen type
	 */
	private static Text getRandomType(final ArrayList<Text> types) {
		return types.get(rand.nextInt(types.size()));
	}
}
