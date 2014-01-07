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

public strictfp class Constants 
{		
	public static int VERSION = 131;
	public static String VERSION_STRING = "1.33";
	
	public static int TURN_TIME = 16;
	public static int COMMAND_TURNS = 30;	
	
	public static float BOID_MAX_SPEED = 16f;
	public static float BOID_MAX_FORCE = 0.3f;
	public static float BOID_DECCELARATION = 0.90f;
	public static float BOID_MIN_SPEED = 0.3f;
	public static final float BOID_MIN_SPEED_FRAC = 0.7f;
	public static float BOID_SIZE = 12;	
	public static float BOID_GROWTH = 0.003f;
	public static int BOID_MAX_AGE = 45 * 1000 / TURN_TIME;
	public static int BOID_DEATH_TIME = 1000 / TURN_TIME;
	
	public static float BOID_MIN_ORBIT = BOID_SIZE * 5;
	public static float BOID_MAX_ORBIT = BOID_SIZE * 20;
	public static float BOID_MIN_ORBIT2 = BOID_MIN_ORBIT * BOID_MIN_ORBIT;
	public static float BOID_MAX_ORBIT2 = BOID_MAX_ORBIT * BOID_MAX_ORBIT;
	
	public static float TREE_GROWTH = 0.1f;
	public static final float TREE_ANGLE = 34;
	public static final float TREE_DELATION = 0.9f;
	public static final float TREE_HEIGHT = 100;
	public static final int TREE_DEPTH = 6;
	public static final int TREE_MAX_CREATURES = 1;	
	public static final int TREE_COST = 10;
	public static final float STRENGTH_ADJUSTMENT = 0.5f;
	public static final int BOID_SHOOT_INTERVAL = (int)(0.09 * 1000 / TURN_TIME);
	public static final float BOID_ATTACK_RADIUS = 10;
	public static final float BOID_DEFENSE_BONUS = 1f;
	public static final float BRANCH_HEALTH = 4;
	public static final float TREE_DEATH_DECREASE = 1 / (1000.0f / TURN_TIME);
	public static final int PLANET_STRENGTH_HILLS = 7;
	public static final int PLANET_STRENGTH_HILLS_MIN = 3;
	public static final float PLANET_MIN_SIZE = 200;
	public static final float PLANET_MAX_SIZE = 1200;
	public static final int PLANET_MAX_CREATURES = 100;
	public static final int PLANET_REGROWTH = (int)(0.25f * 1000 / TURN_TIME );

	public static final int PLANET_MAX_TARGET_LIMIT = 50;
	public static final int MAX_CREATURES = 1500;
	public static final int BOID_WAIT_FOR_ATTACK = 60;
	
	public static int LOG_INTERVAL = 2000;
	public static float PLANET_PULSE_INCREASE = 0.01f;
}
