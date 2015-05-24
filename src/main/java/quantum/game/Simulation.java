//
// Copyright (c) 2009 Mario Zechner.
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the GNU Lesser Public License v2.1
// which accompanies this distribution, and is available at
// http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
// 
// Contributors:
//     Mario Zechner - initial API and implementation
//

package quantum.game;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;

import quantum.game.commands.ChainCommand;
import quantum.game.commands.Command;
import quantum.game.commands.CommandBuffer;
import quantum.game.commands.MoveCreatureCommand;
import quantum.game.commands.PlantTreeCommand;
import quantum.math.Vector2D;
import quantum.net.Client;
import quantum.net.messages.CommandBufferMessage;
import quantum.net.messages.GameOverMessage;
import quantum.net.messages.Message;
import quantum.utils.Log;
import quantum.utils.Random;
import quantum.utils.Timer;
import quantum.utils.delaunay.Delaunay;

/** class representing a simulation. responsible for buffering commands and sending them out to the server on time as well as
 * executing received buffers. commands are buffered for Constants.COMMAND_TURNS turns and then send to the server. When the
 * server received all command buffers from all clients for a command turn it sends the combined command buffers back to each
 * client where they get executed. Additionally the length of a command turn is adapted with respect to the average largest ping
 * of a client.
 * 
 * @author marzec */
public strictfp class Simulation {
	/** the current turn number **/
	int turn;

	/** the number of the next command turn **/
	private int next_cmd_turn = Constants.COMMAND_TURNS;

	/** the number of turns added to the current command turn giving the next command turn will be executed **/
	private int cmd_turn_inc = Constants.COMMAND_TURNS;

	/** queue for received commandbuffer turns. the queue is ordered by the turn id of the command buffers */
	LinkedList<CommandBufferMessage> cmd_queue = new LinkedList<CommandBufferMessage>();

	/** buffer to which all actions of this command turn are written to */
	CommandBuffer turn_cmd_buffer = new CommandBuffer(Constants.COMMAND_TURNS * 2);

	/** list of listeners called upon arrival of a new command turn message */
	List<CommandTurnListener> listeners = new ArrayList<CommandTurnListener>();

	/** client to be used to send command buffers */
	Client client;

	/** planets **/
	List<Planet> planets = new LinkedList<Planet>();

	/** all game objects **/
	HashMap<Integer, GameObject> objects = new HashMap<Integer, GameObject>();

	/** the delaunay triangulation of the planets **/
	Delaunay delaunay;

	/** randomizer to be used throughout the whole simulation, seeded by server **/
	Random rand;
	long seed = System.nanoTime();
	private int random_calls = 0;

	/** next object id **/
	int next_id = 0;

	/** player stats **/
	HashMap<Integer, Integer> player_stats = new HashMap<Integer, Integer>();

	/** total creatures currently in game **/
	HashMap<Integer, Integer> total_creatures = new HashMap<Integer, Integer>();

	/** timer **/
	Timer timer = new Timer();
	double elapsed_time = 0;

	/** do we receive local input? e.g. in case of the tutorial or when replaying **/
	boolean local_input = false;

	Map<Integer, Set<Integer>> allies = new HashMap<Integer, Set<Integer>>();

	/** author of the simulation **/
	String author = "";

	/** description of the simulation **/
	String description = "";

	/** name of the simulation **/
	String name = "";

	/** constructor, sets the client to be used. in case client is null no commands will be buffered or send making this only a
	 * non-interactive simulation fed by enqueued buffers.
	 * @param client */
	public Simulation (boolean local_input) {
		rand = new Random(seed);
		this.local_input = local_input;

	}

	/** sets the client to be used by this simulation. in case no client is set this simulation will only be interpreting enqueued
	 * buffers.
	 * 
	 * @param client */
	public void setClient (Client client) {
		this.client = client;
	}

	/** returns the planet with the specified id
	 * 
	 * @param planet
	 * @return */
	public Planet getPlanet (int planet) {
		return (Planet)getObject(planet);
	}

	public AliveGameObject getAliveGameObject (int object) {
		return (AliveGameObject)getObject(object);
	}

	public Creature getCreature (int creature) {
		return (Creature)getObject(creature);
	}

	public Tree getTree (int tree) {
		return (Tree)getObject(tree);
	}

	public GameObject getObject (int object) {
		return objects.get(object);
	}

	public int getObjectCount () {
		return objects.size();
	}

	public void addObject (GameObject obj) {
		objects.put(obj.getId(), obj);
		if (obj instanceof Planet) {
			planets.add((Planet)obj);
		}
	}

	public void removeObject (GameObject obj) {
		objects.remove(obj.getId());

		if (obj instanceof Planet) {
			Planet planet = (Planet)obj;

			for (int reachable : planet.getReachablePlanets())
				getPlanet(reachable).getReachablePlanets().remove(planet.getId());

			for (Creature creature : planet.getCreatures())
				objects.remove(creature.getId());
			for (Tree tree : planet.getTrees())
				objects.remove(tree);
			planets.remove(planet);
		}

	}

	private void updateStats () {
		player_stats.clear();

		for (Planet planet : planets) {
			if (!player_stats.containsKey(planet.getOwner())) {
				player_stats.put(planet.getOwner(), 0);
				total_creatures.put(planet.getOwner(), 0);
			}

			player_stats.put(planet.getOwner(), player_stats.get(planet.getOwner()) + planet.getResources());

			for (Creature creature : planet.getCreatures()) {
				if (!player_stats.containsKey(creature.getOwner())) {
					player_stats.put(creature.getOwner(), 0);
					total_creatures.put(creature.getOwner(), 1);
				} else {
					player_stats.put(creature.getOwner(), player_stats.get(creature.getOwner()) + 1);
					total_creatures.put(creature.getOwner(), total_creatures.get(creature.getOwner()) + 1);
				}
			}

			player_stats.put(planet.getOwner(), player_stats.get(planet.getOwner()) + planet.getTrees().size() * 10);
		}
	}

	public String getStats () {
		String text = "";

		text += "planets: " + planets.size() + "\n";
		text += "objects: " + objects.size() + "\n";

		return text;
	}

	public Planet getPlanet (float x, float y) {
		Vector2D v = new Vector2D(x, y);
		for (Planet planet : planets)
			if (planet.getPosition().dst2(v) <= planet.getRadius() * planet.getRadius()) return planet;

		return null;
	}

	public float rand () {
		setRandomCalls(getRandomCalls() + 1);
		return (float)rand.rand();
	}

	public float rand (float min, float max) {
		setRandomCalls(getRandomCalls() + 1);
		return (float)rand.rand(min, max);
	}

	public void calculatePaths () {
		float[][] pos = new float[2][planets.size()];

		if (planets.size() == 0) return;

		for (Planet planet : planets)
			planet.getReachablePlanets().clear();

		if (planets.size() < 3) {
			if (planets.size() == 2) {
				planets.get(0).clearReachablePlanets();
				planets.get(1).clearReachablePlanets();
				planets.get(0).addReachablePlanet(planets.get(1).getId());
				planets.get(1).addReachablePlanet(planets.get(0).getId());
			} else
				planets.get(0).clearReachablePlanets();

			return;
		}

		for (int i = 0; i < planets.size(); i++) {
			planets.get(i).clearReachablePlanets();
			pos[0][i] = planets.get(i).getPosition().x;
			pos[1][i] = planets.get(i).getPosition().y;
		}

		try {
			delaunay = Delaunay.triangulate(pos);

			for (int i = 0; i < delaunay.Tri.length; i++) {
				int tri[] = delaunay.Tri[i];
				planets.get(tri[0]).addReachablePlanet(planets.get(tri[1]).getId());
				planets.get(tri[0]).addReachablePlanet(planets.get(tri[2]).getId());

				planets.get(tri[1]).addReachablePlanet(planets.get(tri[0]).getId());
				planets.get(tri[1]).addReachablePlanet(planets.get(tri[2]).getId());

				planets.get(tri[2]).addReachablePlanet(planets.get(tri[1]).getId());
				planets.get(tri[2]).addReachablePlanet(planets.get(tri[0]).getId());
			}
		} catch (Exception ex) {
			Log.println("[Simulation] couldn't triangulate planets: " + Log.getStackTrace(ex));
			ex.printStackTrace();
		}
	}

	public void renderPaths () {
		if (delaunay != null) {
			GL2 gl = GLContext.getCurrent().getGL().getGL2();
			gl.glEnable(GL.GL_BLEND);
			gl.glColor4f(1, 1, 1, 0.5f);
			gl.glBegin(GL.GL_LINES);

			for (int i = 0; i < delaunay.Tri.length; i++) {
				int tri[] = delaunay.Tri[i];
				Vector2D p1 = planets.get(tri[0]).getPosition();
				Vector2D p2 = planets.get(tri[1]).getPosition();
				Vector2D p3 = planets.get(tri[2]).getPosition();

				gl.glVertex2f(p1.x, p1.y);
				gl.glVertex2f(p2.x, p2.y);

				gl.glVertex2f(p2.x, p2.y);
				gl.glVertex2f(p3.x, p3.y);

				gl.glVertex2f(p1.x, p1.y);
				gl.glVertex2f(p3.x, p3.y);
			}
			gl.glEnd();
			gl.glDisable(GL.GL_BLEND);
		}
	}

	/** updates the simulation state in client mode.
	 * @return
	 * @throws Exception */
	public boolean update () throws Exception {
		synchronized (this) {
			timer.start();
			if (processCommandBufferMessage() == false) return false;

			for (Planet planet : planets)
				planet.update();

			updateStats();
			turn++;
			elapsed_time = timer.getElapsedSeconds();
			timer.stop();
			return true;
		}
	}

	public void updatePlanets () {
		for (Planet planet : planets)
			planet.update();
	}

	public double getSimulationUpdateTime () {
		return elapsed_time * 1000;
	}

	public float getAdjustedGrowth (int owner) {
		if (total_creatures.containsKey(owner) == false)
			return Constants.BOID_GROWTH;
		else
			return total_creatures.get(owner) > Constants.MAX_CREATURES ? Constants.BOID_GROWTH / 20 : Constants.BOID_GROWTH;
	}

	public int getCreatureCount (int owner) {
		if (total_creatures.containsKey(owner) == false)
			return 0;
		else
			return total_creatures.get(owner);
	}

	/** checks wheter the current turn is a command buffer turn. if so it checks wheter an adequat commandbuffermessage is enqueue,
	 * removes it from the queue and executes it. in case no adequat command buffer is available it returns false indicating that
	 * no update will be executed. Also adjusts the command turn increment in accordance to the value set in the
	 * commandbuffermessage.
	 * 
	 * @return wheter or not an update should be executed
	 * @throws Exception in case the command buffer id of the polled buffer is wrong */
	protected boolean processCommandBufferMessage () throws Exception {
		if (turn == getNextCommandTurn()) {
			int old_cmd_turn_inc = getCommandTurnIncrement();
			if (getNextCommandTurn() >= Constants.COMMAND_TURNS * 2) {
				if (cmd_queue.peek() == null) return false;

				if (cmd_queue.peek().getCommandBuffer().getTurnId() != turn)
					throw new Exception("SEVERE ERROR!!! command buffer turn id invalid! current turn: " + turn + ", received turn: "
						+ cmd_queue.peek().getCommandBuffer().getTurnId());

				CommandBufferMessage msg = cmd_queue.poll();
				executeCommandBuffer(msg.getCommandBuffer());
				setCommandTurnIncrement(msg.getNextCommandTurns());
				for (CommandTurnListener listener : listeners)
					listener.commandTurn(msg);
			}

			if (client != null) {
				CommandBufferMessage msg = new CommandBufferMessage(client.getPlayer().getId(), turn_cmd_buffer);
				client.sendMessage(msg);

				if (isGameOver()) client.sendMessage(new GameOverMessage());

				turn_cmd_buffer.setTurnId(turn_cmd_buffer.getTurnId() + getCommandTurnIncrement());
				turn_cmd_buffer.clear();
			} else {
				if (local_input) {
					CommandBuffer buffer = new CommandBuffer(turn_cmd_buffer.getTurnId());
					buffer.add(turn_cmd_buffer);
					CommandBufferMessage msg = new CommandBufferMessage(0, buffer);
					msg.setCommandTurns(old_cmd_turn_inc);
					enqueueTurnCommandBufferMessage(msg);
					turn_cmd_buffer.setTurnId(turn_cmd_buffer.getTurnId() + getCommandTurnIncrement());
					turn_cmd_buffer.clear();
				} else {
					turn_cmd_buffer.setTurnId(turn_cmd_buffer.getTurnId() + getCommandTurnIncrement());
					turn_cmd_buffer.clear();
				}
			}

			setNextCommandTurn(getNextCommandTurn() + old_cmd_turn_inc);
		}

		return true;
	}

	/** executes the commands in the command buffer
	 * @param buffer */
	protected void executeCommandBuffer (CommandBuffer buffer) {
		for (Command cmd : buffer) {
			if (cmd instanceof MoveCreatureCommand) {
				MoveCreatureCommand move_cmd = (MoveCreatureCommand)cmd;
				Planet planet = getPlanet(move_cmd.getSourcePlanet());

				if (!planet.isReachablePlanet(move_cmd.getDestinationPlanet())) continue;

				int send = 0;
				for (Creature creature : planet.getCreatures()) {
					if (creature.getOwner() == move_cmd.getId() && creature.isBorn() && creature.isDead() == false
						&& (creature.isOrbiting() || creature.isAttacking())) {
						creature.move(move_cmd.getDestinationPlanet());
						send++;
					}

					if (send == move_cmd.getUnits()) break;
				}
			}

			if (cmd instanceof PlantTreeCommand) {
				PlantTreeCommand plant_command = (PlantTreeCommand)cmd;
				Planet planet = getPlanet(plant_command.getPlanet());

				if (planet.getOwner() != plant_command.getId()) continue;

				if (planet.getFriendlyCreatures(planet.getOwner()) < Constants.TREE_COST) continue;

				planet.spawnTree();
				int removed = 0;
				for (Creature creature : planet.getCreatures()) {
					if (creature.getOwner() == plant_command.getId() && creature.isDying() == false) {
						creature.setHealth(0);
						removed++;
					}

					if (removed == Constants.TREE_COST) break;
				}
			}

			if (cmd instanceof ChainCommand) {
				ChainCommand chain_cmd = (ChainCommand)cmd;
				int id = chain_cmd.getId();
				Planet source = getPlanet(chain_cmd.getSource());
				Planet target = getPlanet(chain_cmd.getTarget());

				if (chain_cmd.getTarget() == -1 && source.getOwner() == id) {
					source.setChainedPlanet(-1);
					continue;
				}

				if (source.isReachablePlanet(target.getId()) && target.getChainedPlanet() != source.getId()) {
					source.setChainedPlanet(target.getId());
				}
			}

		}
	}

	/** enqueues the commandbuffermessage for execution when its turn is reached.
	 * @param buffer */
	public void enqueueTurnCommandBufferMessage (CommandBufferMessage buffer) {
		synchronized (this) {
			cmd_queue.addLast(buffer);
		}
	}

	/** @return all planets */
	public synchronized List<Planet> getPlanets () {
		return planets;
	}

	/** writes the complete simulation state to the output stream. can be read in by the readState method.
	 * @param out
	 * @throws IOException */
	public void writeState (DataOutputStream out) throws Exception {
		out.writeLong(seed);
		out.writeInt(planets.size());
		for (Planet planet : planets)
			planet.write(out);
		out.writeInt(next_id);

		for (Planet planet : planets) {
			out.writeInt(planet.getReachablePlanets().size());
			for (int neighbour : planet.getReachablePlanets())
				out.writeInt(neighbour);
		}

		Message.writeString(out, author);
		Message.writeString(out, name);
		Message.writeString(out, description);
	}

	/** reads a simulation state from the input stream
	 * @param in
	 * @throws IOException */
	public void readState (DataInputStream in) throws Exception {
		planets.clear();
		objects.clear();

		seed = in.readLong();
		rand = new Random(seed);
		int n = in.readInt();
		for (int i = 0; i < n; i++) {
			Planet planet = new Planet(this);
			planet.read(in);
			addObject(planet);
		}

		next_id = in.readInt();

		for (Planet planet : planets) {
			n = in.readInt();
			for (int i = 0; i < n; i++)
				planet.getReachablePlanets().add(in.readInt());
		}

		try {
			author = Message.readString(in);
			name = Message.readString(in);
			description = Message.readString(in);
		} catch (Exception ex) {
			author = "";
			description = "";
			name = "";
		}

		turn = 0;
		setNextCommandTurn(Constants.COMMAND_TURNS);
		setCommandTurnIncrement(Constants.COMMAND_TURNS);
		cmd_queue.clear();
		turn_cmd_buffer.clear();
		turn_cmd_buffer.setTurnId(Constants.COMMAND_TURNS * 2);

		resetRandom();
	}

	/** returns the current turn number
	 * @return */
	public int getTurn () {
		return turn;
	}

	public void setTurn (int turn) {
		this.turn = turn;
	}

	/** returns the length in turns for a command turn.
	 * @return */
	public int getCommandTurnLength () {
		return getCommandTurnIncrement();
	}

	/** moves units creatures from planet src_planet_id to planet dst_planet_id
	 * @param id
	 * @param id2
	 * @param units */
	public void moveCreatures (int owner_id, int src_planet_id, int dst_planet_id, int units) {
		turn_cmd_buffer.add(new MoveCreatureCommand(owner_id, src_planet_id, dst_planet_id, units));
	}

	/** plants a tree at the given planet removing 10 creatures in exchange
	 * 
	 * @param owner_id
	 * @param planet_id */
	public void plantTree (int owner_id, int planet_id) {
		turn_cmd_buffer.add(new PlantTreeCommand(owner_id, planet_id));
	}

	/** chains/unchains a planet pair **/

	public void chainPlanets (int owner_id, int source, int target) {
		turn_cmd_buffer.add(new ChainCommand(owner_id, source, target));
	}

	/** @return the stat of each player by id. stat is composed of the number of creatures, planets and trees */
	public HashMap<Integer, Integer> getPlayerStats () {
		return player_stats;
	}

	public boolean isGameOver () {
		synchronized (this) {
			int active = 0;
			for (int key : player_stats.keySet()) {
				if (key == -1) continue;

				int creatures = 0;
				int trees = 0;
				for (Planet planet : planets) {
					for (Creature creature : planet.getCreatures())
						if (creature.getOwner() == key) creatures++;

					if (planet.getOwner() == key) trees += planet.getTrees().size();
				}

				if (creatures != 0 || trees != 0) active++;
			}

			if (active <= 1)
				return true;
			else
				return false;
		}
	}

	public void setCommandTurnIncrement (int cmd_turn_inc) {
		this.cmd_turn_inc = cmd_turn_inc;
	}

	public int getCommandTurnIncrement () {
		return cmd_turn_inc;
	}

	public void setNextCommandTurn (int next_cmd_turn) {
		this.next_cmd_turn = next_cmd_turn;
	}

	public int getNextCommandTurn () {
		return next_cmd_turn;
	}

	public void setRandomCalls (int random_calls) {
		this.random_calls = random_calls;
	}

	public int getRandomCalls () {
		return random_calls;
	}

	public void resetRandom () {
		rand = new Random(seed);
		random_calls = 0;
	}

	public int getNextId () {
		return next_id++;
	}

	public void clear () {
		this.objects.clear();
		this.planets.clear();
	}

	public void setLocalInput (boolean b) {
		local_input = b;
	}

	public void load (String string) throws Exception {
		DataInputStream in = new DataInputStream(new FileInputStream(string));
		readState(in);
		in.close();
	}

	public void load (InputStream stream) throws Exception {
		DataInputStream in = new DataInputStream(stream);
		readState(in);
		in.close();
	}

	public void addCommandTurnListener (CommandTurnListener listener) {
		listeners.add(listener);
	}

	public void removeCommandTurnListener (CommandTurnListener listener) {
		listeners.remove(listener);
	}

	public Collection<GameObject> getGameObjects () {
		return this.objects.values();
	}

	public int getActivePlayers () {
		int count = 0;
		for (int id : player_stats.keySet())
			if (id != -1) count++;

		return count;
	}

	public void setName (String name) {
		this.name = name;
	}

	public String getName () {
		return name;
	}

	public void setAuthor (String author) {
		this.author = author;
	}

	public String getAuthor () {
		return author;
	}

	public void setDescription (String description) {
		this.description = description;
	}

	public String getDescription () {
		return description;
	}
}
