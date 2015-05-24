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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.awt.GLCanvas;

import quantum.gfx.Renderer;
import quantum.math.Vector2D;

/** Planet class. A Planet is either free or in possession of a Player. A Planet has trees that produce creatures. The creature
 * properties are determined by the planets properties, namely its strength, health and speed. A Planet can only host a certain
 * number of trees depending on its radius.
 * 
 * @author Administrator */
public strictfp class Planet extends GameObject {
	int owner = -1;
	float radius;
	float strength;
	float health;
	float speed;
	int resources = 0;
	int full_resources = 0;
	boolean is_start_planet = false;
	int chain_planet = -1;

	List<Tree> trees = new LinkedList<Tree>();
	List<Creature> creatures = new ArrayList<Creature>();
	List<Creature> added_creatures = new LinkedList<Creature>();
	List<Creature> removed_creatures = new LinkedList<Creature>();
	List<Tree> removed_trees = new LinkedList<Tree>();

	float[] points = new float[361 * 2];
	HashSet<Integer> reachable_planets = new HashSet<Integer>();
	int friendly = 0;
	int enemy = 0;
	boolean is_regrowing = false;
	int unborn_friendly = 0;
	int orbiting_friendly = 0;

	float[] creature_points = new float[18];

	public void read (DataInputStream in) throws Exception {
		super.read(in);
		owner = in.readInt();
		radius = in.readFloat();
		strength = in.readFloat();
		health = in.readFloat();
		setSpeed(in.readFloat());
		setResources(in.readInt());
		full_resources = in.readInt();
		is_start_planet = in.readBoolean();
		last_idx = in.readInt();
		is_regrowing = in.readBoolean();
		chain_planet = in.readInt();

		int n = in.readInt();
		for (int i = 0; i < n; i++) {
			Tree tree = new Tree(sim);
			tree.read(in);
			trees.add(tree);
			sim.addObject(tree);
		}

		n = in.readInt();
		for (int i = 0; i < n; i++) {
			Creature creature = new Creature(sim);
			creature.read(in);
			creatures.add(creature);
			sim.addObject(creature);
		}

		createMesh();
	}

	public void write (DataOutputStream out) throws Exception {
		super.write(out);
		out.writeInt(owner);
		out.writeFloat(radius);
		out.writeFloat(strength);
		out.writeFloat(health);
		out.writeFloat(speed);
		out.writeInt(resources);
		out.writeInt(full_resources);
		out.writeBoolean(is_start_planet);
		out.writeInt(last_idx);
		out.writeBoolean(is_regrowing);
		out.writeInt(chain_planet);

		out.writeInt(trees.size());
		for (Tree tree : trees)
			tree.write(out);

		out.writeInt(creatures.size());
		for (Creature creature : creatures)
			creature.write(out);
	}

	public Planet (Simulation sim) {
		super(sim);
	}

	public Planet (Simulation sim, Vector2D pos, float radius, float strength, float health, float speed, int max_creatures) {
		super(sim, pos);
		this.radius = radius;
		this.strength = strength;
		this.health = health;
		setSpeed(speed);
		setResources(max_creatures);
		createMesh();
	}

	public boolean isRegrowing () {
		return is_regrowing;
	}

	public int getChainedPlanet () {
		return chain_planet;
	}

	public void setChainedPlanet (int chained_planet) {
		chain_planet = chained_planet;
	}

	public void clearReachablePlanets () {
		reachable_planets.clear();
	}

	public void addReachablePlanet (int id) {
		reachable_planets.add(id);
	}

	public Set<Integer> getReachablePlanets () {
		return reachable_planets;
	}

	public void setOwner (int id) {
		owner = id;
	}

	public boolean isStartPlanet () {
		return is_start_planet;
	}

	public void setStartPlanet (boolean val) {
		is_start_planet = val;
	}

	public void spawnCreature () {
		Vector2D pos = new Vector2D(new Vector2D(sim.rand() - sim.rand(), sim.rand() - sim.rand()).nor().mul(
			radius + Constants.BOID_MIN_ORBIT + (float)Math.random() * (Constants.BOID_MAX_ORBIT - Constants.BOID_MIN_ORBIT)));
		float angle = (float)Math.toDegrees(Math.atan2(pos.y, pos.x)) + 90;
		pos.add(this.pos);
		Creature creature = new Creature(sim, getId(), owner, pos.x, pos.y, angle, strength, health, speed);
		creatures.add(creature);
		sim.addObject(creature);
		creature.setScale(1);
	}

	public void addCreature (Creature creature) {
		added_creatures.add(creature);
		// creatures.add(creature);
	}

	public void removeCreature (Creature creature) {
		removed_creatures.add(creature);
		// creatures.remove(creature);
	}

	public void removeAllCreatures () {
		for (Creature creature : creatures)
			sim.removeObject(creature);
		creatures.clear();
	}

	public boolean requestResourceForCreature () {
		if (resources <= 0) {
			resources = 0;
			return false;
		} else {
			resources--;
			return true;
		}
	}

	int last_idx = 0;

	public void spawnTree () {
		int idx = last_idx + (int)(sim.rand(10, 110));
		last_idx = idx % 359;
		idx = idx % 359;
		Vector2D pos = new Vector2D(points[idx * 2] * radius, points[idx * 2 + 1] * radius);
		pos.add(this.pos);
		Tree tree = new Tree(sim, pos, id);
		trees.add(tree);
		sim.addObject(tree);
	}

	public void removeTree (Tree tree) {
		removed_trees.add(tree);
	}

	public void removeAllTrees () {
		for (Tree tree : trees)
			sim.removeObject(tree);
		trees.clear();
	}

	public int getOwner () {
		return owner;
	}

	public float getStrength () {
		return strength;
	}

	public void setStrength (float strength) {
		this.strength = strength;
		for (Creature creature : creatures)
			if (creature.getOwner() == owner) creature.setStrength(strength);
		createMesh();
	}

	public float getHealth () {
		return health;
	}

	public void setHealth (float health) {
		this.health = health;
		for (Creature creature : creatures)
			if (creature.getOwner() == owner) creature.setHealth(health);

		createMesh();
	}

	public float getSpeed () {
		return speed;
	}

	public void setSpeed (float speed) {
		this.speed = Math.max(Math.min(speed, 1), Constants.BOID_MIN_SPEED_FRAC);
		for (Creature creature : creatures)
			if (creature.getOwner() == owner) creature.setSpeed(speed);
		createMesh();
	}

	public int getResources () {
		return resources;
	}

	public void setResources (int resources) {
		this.resources = resources;
		this.full_resources = resources;
		if (resources >= Constants.PLANET_MAX_CREATURES) this.resources = Constants.PLANET_MAX_CREATURES;

		this.radius = Constants.PLANET_MIN_SIZE + (Constants.PLANET_MAX_SIZE - Constants.PLANET_MIN_SIZE) * getResources()
			/ Constants.PLANET_MAX_CREATURES;
		createMesh();
	}

	public float getRadius () {
		return radius;
	}

	public List<Creature> getCreatures () {
		return creatures;
	}

	public int getFriendlyCreatures (int owner) {
		int n = 0;
		for (Creature c : creatures)
			if (c.getOwner() == owner && c.isBorn() && !c.isDying()) n++;
		return n;
	}

	public int getMoveableCreatures (int owner) {
		int n = 0;
		for (Creature c : creatures)
			if (c.getOwner() == owner && c.isBorn() && !c.isDying() && (c.isOrbiting() || c.isAttacking())) n++;

		return n;
	}

	public int getEnemeyCreatures (int owner) {
		int n = 0;
		for (Creature c : creatures)
			if (c.getOwner() != owner) n++;
		return n;
	}

	public List<Tree> getTrees () {
		return trees;
	}

	public void update () {
		orbiting_friendly = 0;
		unborn_friendly = 0;
		friendly = 0;
		enemy = 0;
		Iterator<Creature> c_iter = creatures.iterator();

		while (c_iter.hasNext()) {
			Creature creature = c_iter.next();

			if (creature.getOwner() == owner) {
				if (creature.isBorn() && !creature.isDying()) friendly++;
				if (!creature.isBorn()) unborn_friendly++;
				if (creature.isMoving() == false && creature.isBorn()) orbiting_friendly++;

				if (chain_planet != -1 && creature.isOrbiting() && creature.getPosition().dst2(pos) < (radius * 2.6) * (radius * 2.6))
					creature.move(chain_planet);
			} else {
				if (creature.isBorn() && !creature.isDying()) enemy++;
			}
		}

		if (resources < full_resources && (trees.size() == 0 || (orbiting_friendly == 0 && unborn_friendly == 0))) {
			if (is_regrowing) {
				if (sim.getTurn() % Constants.PLANET_REGROWTH == 0) resources += 1;
			}
			is_regrowing = true;
		} else
			is_regrowing = false;

		c_iter = creatures.iterator();
		while (c_iter.hasNext()) {
			Creature creature = c_iter.next();

			if (creature.isDead()) {
				c_iter.remove();
				sim.removeObject(creature);
			} else
				creature.update();
		}

		for (Creature creature : removed_creatures)
			creatures.remove(creature);
		for (Creature creature : added_creatures)
			creatures.add(creature);

		if (friendly == 0 && enemy > 0 && trees.size() == 0) {
			if (creatures.size() > 0)
				this.owner = creatures.get(0).getOwner();
			else
				this.owner = -1;
			chain_planet = -1;
		}

		removed_creatures.clear();
		added_creatures.clear();

		for (Tree tree : trees) {
			tree.update();
			if (tree.isDead()) removed_trees.add(tree);
		}

		for (Tree tree : removed_trees)
			trees.remove(tree);

		removed_trees.clear();

	}

	protected void createMesh () {

		for (int i = 0; i < 361; i++) {
			double angle = Math.toRadians(i);

			points[i * 2] = (float)Math.cos(angle);
			points[i * 2 + 1] = (float)Math.sin(angle);
		}

		creature_points = Creature.generateTriangles(strength, health, speed);
	}

	public void renderMesh (GLCanvas canvas, float scale, float r, float g, float b) {
		GL2 gl = canvas.getGL().getGL2();

		gl.glColor3f(r, g, b);
		gl.glPushMatrix();
		gl.glTranslatef(pos.x, pos.y, 0);
		gl.glScalef(radius * scale, radius * scale, 0);

		gl.glBegin(GL.GL_LINE_STRIP);
		gl.glNormal3f(0, 0, 0);
		for (int i = 0; i < 364; i += 4) {
			double angle = Math.toRadians(i);
			gl.glVertex2f((float)Math.cos(angle), (float)Math.sin(angle));
		}
		gl.glEnd();
		gl.glPopMatrix();
	}

	public void render (GLCanvas canvas, Renderer renderer) {
		GL2 gl = canvas.getGL().getGL2();

		gl.glColor3f(0.7f, 0.7f, 1);
		gl.glPushMatrix();
		gl.glTranslatef(pos.x, pos.y, 0);
		gl.glScalef(radius, radius, 0);

		gl.glBegin(GL.GL_LINE_STRIP);
		for (int i = 0; i < 364; i += 4) {
			double angle = Math.toRadians(i);
			gl.glVertex2f((float)Math.cos(angle), (float)Math.sin(angle));
		}
		gl.glEnd();
		gl.glPopMatrix();
	}

	public void renderCreature (GLCanvas canvas, Renderer renderer) {
		for (int i = 0; i < 18; i += 2)
			canvas.getGL().getGL2().glVertex2f(creature_points[i + 1] * (Constants.PLANET_MIN_SIZE - 10) + pos.x,
				creature_points[i] * (Constants.PLANET_MIN_SIZE - 10) + pos.y);
	}

	public boolean isReachablePlanet (int destinationPlanet) {
		for (Integer id : reachable_planets)
			if (id == destinationPlanet) return true;

		return false;
	}

	public boolean hasEnemyCreatures () {
		return enemy > 0;
	}

	public float getCreatureStrength (int id, int units) {
		float strength = 0;
		int send = 0;
		for (Creature creature : getCreatures()) {
			if (creature.getOwner() == id && creature.isBorn() && creature.isDead() == false
				&& (creature.isOrbiting() || creature.isAttacking())) {
				strength += creature.getHealth() + creature.getSpeed() + creature.getStrength();
				send++;
			}

			if (send == units) break;
		}

		return strength;
	}

	public float getCreatureStrengthExclude (int id) {
		float strength = 0;
		for (Creature creature : getCreatures()) {
			if (creature.getOwner() != id && creature.isBorn() && creature.isDead() == false
				&& (creature.isOrbiting() || creature.isAttacking())) {
				strength += creature.getHealth() + creature.getSpeed() + creature.getStrength();
			}
		}

		return strength;
	}

	public int getMaxResources () {
		return full_resources;
	}

	public int getUnbornCreatures () {
		return unborn_friendly;
	}

	public int getOrbitingCreatures () {
		return orbiting_friendly;
	}
}
