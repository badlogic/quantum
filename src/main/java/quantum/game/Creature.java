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

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;

import quantum.gfx.Color;
import quantum.math.Vector2D;

public strictfp class Creature extends Boid implements AliveGameObject {
	int planet;
	int owner;
	float strength;
	float health;
	float speed;
	float scale = 0.0f;
	boolean dead = false;
	int death_time = 0;

	public static int ACTION_ORBIT = 0;
	public static int ACTION_MOVE = 1;
	public static int ACTION_ATTACK = 2;

	int action = ACTION_ORBIT;
	int planet_target = 0;
	int could_attack = 0;

	private float[] points = new float[18 * 2];
	private float r = 1, g, b;
	int shot = 0;

	protected Creature (Simulation sim) {
		super(sim);
	}

	public Creature (Simulation sim, int planet, int owner, float x, float y, float angle, float strength, float health,
		float speed) {
		super(sim, x, y, angle);
		this.planet = planet;
		this.owner = owner;
		this.strength = strength;
		this.health = health;
		this.speed = Constants.BOID_MIN_SPEED_FRAC + (1 - Constants.BOID_MIN_SPEED_FRAC) * Math.max(Math.min(speed, 1), 0);
		max_speed = Constants.BOID_MAX_SPEED * speed;
		vel.nor().mul(max_speed);
		this.steer(SteeringBehaviour.Circle, planet);
		generateTriangles();
	}

	public String getActionAsString () {
		if (action == ACTION_ORBIT) return "Orbiting";
		if (action == ACTION_MOVE) return "Moving";
		if (action == ACTION_ATTACK) return "Attack";

		return "unkown";
	}

	public float getMaxSpeed () {
		return action == ACTION_ORBIT
			&& sim.getPlanet(planet).getPosition().dst2(pos) < (sim.getPlanet(planet).getRadius() + Constants.BOID_MAX_ORBIT)
				* (sim.getPlanet(planet).getRadius() + Constants.BOID_MAX_ORBIT) ? max_speed * 0.6f : max_speed;
	}

	public void setColor (Color col) {
		if (col != null) {
			r = col.getR();
			g = col.getG();
			b = col.getB();
		}
	}

	private void generateTriangles () {
		Vector2D p1 = new Vector2D();
		Vector2D p2 = new Vector2D();
		Vector2D p3 = new Vector2D();

		float a = Constants.BOID_SIZE * health;
		float h = (float)Math.sqrt(3) / 2 * a;
		p1.x = h / 2;
		p1.y = a / 2;
		p2.x = h / 2;
		p2.y = a / -2;
		p3.x = -h / 2;
		p3.y = 0;

		points[0] = p1.x;
		points[1] = p1.y;
		points[2] = p2.x;
		points[3] = p2.y;
		points[4] = h / 2 + Constants.BOID_SIZE * strength;

		points[6] = p1.x;
		points[7] = p1.y;
		points[8] = p3.x;
		points[9] = p3.y;
		points[10] = (p1.x - p3.x) / 2 - Constants.BOID_SIZE * speed;
		points[11] = (p1.y - p3.y) / 2 + Constants.BOID_SIZE * speed;

		points[12] = p2.x;
		points[13] = p2.y;
		points[14] = p3.x;
		points[15] = p3.y;
		points[16] = (p2.x - p3.x) / 2 - Constants.BOID_SIZE * speed;
		points[17] = (p2.y - p3.y) / 2 - Constants.BOID_SIZE * speed;
	}

	public static float[] generateTriangles (float strength, float health, float speed) {
		Vector2D p1 = new Vector2D();
		Vector2D p2 = new Vector2D();
		Vector2D p3 = new Vector2D();

		float a = health;
		float h = (float)Math.sqrt(3) / 2 * a;
		p1.x = h / 2;
		p1.y = a / 2;
		p2.x = h / 2;
		p2.y = a / -2;
		p3.x = -h / 2;
		p3.y = 0;

		float points[] = new float[18];
		points[0] = p1.x;
		points[1] = p1.y;
		points[2] = p2.x;
		points[3] = p2.y;
		points[4] = h / 2 + strength;

		points[6] = p1.x;
		points[7] = p1.y;
		points[8] = p3.x;
		points[9] = p3.y;
		points[10] = (p1.x - p3.x) / 2 - speed * 1.2f;
		points[11] = (p1.y - p3.y) / 2 + speed * 0.7f;

		points[12] = p2.x;
		points[13] = p2.y;
		points[14] = p3.x;
		points[15] = p3.y;
		points[16] = (p2.x - p3.x) / 2 - speed * 1.2f;
		points[17] = (p2.y - p3.y) / 2 - speed * 0.7f;
		return points;
	}

	public void render (GLCanvas canvas, boolean use_lod) {
		GL gl = canvas.getGL();

		float angle_rad = (float)Math.toRadians(angle);

		if (!dead) {
			gl.glColor3f(r, g, b);

			if (!use_lod) {
				gl.glNormal3f(angle_rad, pos.x, pos.y);
				for (int i = 0; i < 18; i += 2)
					gl.glVertex3f(points[i], points[i + 1], scale);
			} else
				gl.glVertex2f(pos.x, pos.y);
		} else {
			float dx = vel.x * death_time;
			float dy = vel.y * death_time;

			gl.glColor4f(r, g, b, scale);

			if (!use_lod) {
				gl.glNormal3f(angle_rad, pos.x, pos.y);
				for (int i = 0; i < 6; i += 2)
					gl.glVertex3f(points[i] + dx, points[i + 1] + dy, 1);

				for (int i = 6; i < 12; i += 2)
					gl.glVertex3f(points[i] + dy, points[i + 1] + dx, 1);

				for (int i = 12; i < 18; i += 2)
					gl.glVertex3f(points[i] + dy, points[i + 1] - dx, 1);
			} else
				gl.glVertex2f(pos.x, pos.y);
		}
	}

	public float getHealth () {
		return health;
	}

	public float getSpeed () {
		return speed;
	}

	public float getStrength () {
		return strength;
	}

	public int getPlanet () {
		return planet;
	}

	public void setPlanet (int planet) {
		this.planet = planet;
	}

	public int getOwner () {
		return owner;
	}

	public void read (DataInputStream in) throws Exception {
		super.read(in);
		planet = in.readInt();
		owner = in.readInt();
		strength = in.readFloat();
		health = in.readFloat();
		speed = Math.max(Math.min(in.readFloat(), 1), Constants.BOID_MIN_SPEED_FRAC);
		max_speed = in.readFloat();
		scale = in.readFloat();
		dead = in.readBoolean();
		death_time = in.readInt();
		action = in.readInt();
		planet_target = in.readInt();
		shot = in.readInt();
		generateTriangles();
	}

	public void write (DataOutputStream out) throws Exception {
		super.write(out);
		out.writeInt(planet);
		out.writeInt(owner);
		out.writeFloat(strength);
		out.writeFloat(health);
		out.writeFloat(speed);
		out.writeFloat(max_speed);
		out.writeFloat(scale);
		out.writeBoolean(dead);
		out.writeInt(death_time);
		out.writeInt(action);
		out.writeInt(planet_target);
		out.writeInt(shot);
	}

	public void update () {
		//
		// growth and death
		//

		if (!dead) {
			if (scale < 1)
				scale += sim.getAdjustedGrowth(this.owner);
			else {
				scale = 1;
				super.update();
			}

			if (health <= 0) dead = true;
		} else {
			scale -= 2 / (float)Constants.BOID_DEATH_TIME;
			death_time++;
			return;
		}

		//
		// action state
		//
		if (action == ACTION_ORBIT) {
			Planet planet = sim.getPlanet(target);
			if (planet.hasEnemyCreatures()) {
				AliveGameObject target = selectTarget();
				if (target != null) {
					action = ACTION_ATTACK;
					steer(SteeringBehaviour.Attack, target.getId());

					if (target instanceof Creature) {
						if (((Creature)target).action != ACTION_ATTACK) {
							((Creature)target).action = ACTION_ATTACK;
							((Creature)target).steer(SteeringBehaviour.Attack, id);
						}
					}
				}
			}
		}

		if (action == ACTION_ATTACK) {
			if (!checkTargetDead()) {
				AliveGameObject object = sim.getAliveGameObject(target);
				if (object.getPosition().dst(pos) < Constants.BOID_SIZE * Constants.BOID_ATTACK_RADIUS && shot == 0) {
					object.adjustHealth(-this.strength * Constants.STRENGTH_ADJUSTMENT);
					shot++;
				}

				if (object instanceof Creature) {
					if (((Creature)object).action != ACTION_ATTACK) {
						((Creature)object).action = ACTION_ATTACK;
						((Creature)object).steer(SteeringBehaviour.Attack, id);
					}
				}

				if (shot != 0) {
					if (shot == Constants.BOID_SHOOT_INTERVAL)
						shot = 0;
					else
						shot++;
				}
			} else {
				action = ACTION_ORBIT;
				this.steer(SteeringBehaviour.Circle, planet);
				shot = 0;
			}
		}

		if (action == ACTION_MOVE) {
			Planet planet = sim.getPlanet(planet_target);
			tmp.set(planet.getPosition()).sub(pos);
			float len = tmp.len();

			if (len < sim.getPlanet(this.planet).getPosition().dst(planet.getPosition()))
				this.steer(SteeringBehaviour.Seek, planet_target);

			if (len - planet.radius < pos.dst(sim.getPlanet(this.planet).getPosition()) - sim.getPlanet(this.planet).getRadius()) {
				sim.getPlanet(this.planet).removeCreature(this);
				planet.addCreature(this);
				this.planet = planet.getId();
				action = ACTION_ORBIT;
				this.steer(SteeringBehaviour.Circle, planet.getId());
			}
		}
	}

	public boolean checkTargetDead () {
		AliveGameObject object = sim.getAliveGameObject(target);
		if (object == null || object.isDead() || object.getPlanet() != planet) {
			Planet planet = sim.getPlanet(this.planet);
			if (planet.hasEnemyCreatures()) {
				AliveGameObject target = selectTarget();
				if (target != null) {
					action = ACTION_ATTACK;
					steer(SteeringBehaviour.Seek, target.getId());

					if (target instanceof Creature) {
						if (((Creature)target).action != ACTION_ATTACK) {
							((Creature)target).action = ACTION_ATTACK;
							((Creature)target).steer(SteeringBehaviour.Attack, id);
						} else {
							if (sim.getAliveGameObject(((Creature)target).target) != null
								&& sim.getAliveGameObject(((Creature)target).target).getPosition().dst2(target.getPosition()) > target
									.getPosition().dst2(pos)) {
								((Creature)target).action = ACTION_ATTACK;
								((Creature)target).steer(SteeringBehaviour.Attack, id);
							}
						}
					}
				} else {
					action = ACTION_ORBIT;
					steer(SteeringBehaviour.Circle, planet.id);
					shot = 0;
				}
			} else {
				action = ACTION_ORBIT;
				steer(SteeringBehaviour.Circle, planet.id);
				shot = 0;
			}

			return true;
		} else
			return false;
	}

	public AliveGameObject selectTarget () {
		Planet planet = sim.getPlanet(this.planet);

		float min_dist = Float.MAX_VALUE;
		AliveGameObject nearest_target = null;

		int samples = Math.min(planet.getCreatures().size(), 50);

		for (int i = 0; i < samples; i++) {
			Creature creature = planet.getCreatures().get((int)sim.rand(0, planet.getCreatures().size() - 1));
			if (creature.isDead() || !creature.isBorn() || creature.getOwner() == owner) {
				samples--;
				continue;
			}

			float dst = creature.getPosition().dst2(pos);
			if (dst < min_dist) {
				min_dist = dst;
				nearest_target = creature;
			}
		}

		// no creature found check for trees
		if (nearest_target == null && could_attack == 0) {
			min_dist = Float.MAX_VALUE;

			for (Tree tree : planet.getTrees()) {
				if (tree.isDead() || planet.getOwner() == owner) continue;

				float dst = tree.getPosition().dst2(pos);
				if (dst < min_dist) {
					nearest_target = tree;
					min_dist = dst;
				}
			}
		}

		return nearest_target;
	}

	boolean isAttacking () {
		return action == ACTION_ATTACK;
	}

	public void setStrength (float strength) {
		this.strength = strength;
		generateTriangles();
	}

	public void setHealth (float health) {
		this.health = health;
		generateTriangles();
	}

	public void adjustHealth (float health) {
		this.health += health;
	}

	public void setSpeed (float speed) {
		this.speed = Math.max(Math.min(speed, 1), Constants.BOID_MIN_SPEED_FRAC);
		max_speed = Constants.BOID_MAX_SPEED * speed;
		generateTriangles();
	}

	public boolean isBorn () {
		return scale >= 1;
	}

	public boolean isDead () {
		if (!dead && health > 0)
			return false;
		else
			return scale <= 0.1f;
	}

	public boolean isDying () {
		return health <= 0;
	}

	public boolean isOrbiting () {
		return action == ACTION_ORBIT;
	}

	public boolean isMoving () {
		return action == ACTION_MOVE;
	}

	public void setScale (float scale) {
		this.scale = scale;
	}

	public void move (int destinationPlanet) {
		// we have to reset to circle behaviour, in case
		// the creature is currently attacking an enemy
		// and is farther away from the target planet
		// than it's current planet is it will fly out
		// to infinity...
		steer(SteeringBehaviour.Circle, planet);
		action = ACTION_MOVE;
		planet_target = destinationPlanet;
	}

	public float getScale () {
		return scale;
	}

	public int getTarget () {
		return planet_target;
	}

	public int getAttackTarget () {
		return target;
	}
}
