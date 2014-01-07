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
import java.io.FileInputStream;

import quantum.game.Tree.Branch;


public class SimulationComparator 
{		
	public static void log( String text )
	{
		System.out.println( text );
	}
	
	public static boolean isEqual( Simulation o_sim, Simulation sim )
	{
		if( o_sim.seed != sim.seed )
		{
			log( "seed not equal" );
			return false;
		}
		
		if( o_sim.rand() != sim.rand() )
		{
			log( "rand not equal" );
			return false;
		}
		
		if( o_sim.getCommandTurnIncrement() != sim.getCommandTurnIncrement() )
		{
			log( "command turn inc not equal" );
			return false;
		}
		
		if( o_sim.getNextCommandTurn() != sim.getNextCommandTurn() )
		{
			log( "next command turn not equal" );
			return false;
		}
			
		if( o_sim.turn != sim.turn )
		{
			log( "turn not equal" );
			return false;
		}
				
		if( o_sim.getRandomCalls() != sim.getRandomCalls() )
		{
			log("random calls count not equal: " + o_sim.getRandomCalls() + "!=" + sim.getRandomCalls()  );
			return false;
		}
		
		for( int i = 0; i < o_sim.getPlanets().size(); i++ )
		{
			Planet o_planet = o_sim.getPlanets().get(i);
			Planet planet = sim.getPlanets().get(i);
			
			log( "checking planet--" );
			if( o_planet.owner != planet.owner )
			{
				log( "planet owner not equal, order probably fucked up" );
				return false;
			}
			
			if( o_planet.enemy != planet.enemy )
			{
				log( "enemy count not equal" );
				return false;
			}
			
			if( o_planet.friendly != planet.friendly )
			{
				log( "friendly count not equal" );
				return false;
			}
			
			if( o_planet.full_resources != planet.full_resources )
			{
				log( "full resources not equal" );
				return false;
			}
			
			if( o_planet.health != planet.health )
			{
				log( "health not equal" );
				return false;
			}
			
			if( o_planet.id != planet.id )
			{
				log( "id not equal" );
				return false;
			}
			
			if( o_planet.is_start_planet != planet.is_start_planet )
			{
				log( "start planet not equal" );
				return false;
			}
			
			if( o_planet.last_idx != planet.last_idx )
			{
				log( "last idx not equal" );
				return false;
			}
			
			if( o_planet.pos.x != planet.pos.x || o_planet.pos.y != planet.pos.y )
			{
				log( "position not equal" );
				return false;
			}
			
			if( o_planet.radius != planet.radius )
			{
				log( "radius not equal" );
				return false;
			}
			
			if( o_planet.resources != planet.resources )
			{ 
				log( "resources not equal" );
				return false;
			}
			
			if( o_planet.speed != planet.speed )
			{
				log( "speed not equal" );
				return false;
			}
			
			if( o_planet.strength != planet.strength )
			{
				log( "strength not equal" );
			}
			
			log( "checking creatures--" );
			for( int j = 0; j < o_planet.getCreatures().size(); j++ )
			{
				if( !isEqual( o_planet.getCreatures().get(j), planet.getCreatures().get(j)))
					return false;
			}
			
			log( "checking trees--" );
			for( int j = 0; j < o_planet.getTrees().size(); j++ )
			{
				if( !isEqual( o_planet.getTrees().get(j), planet.getTrees().get(j) ) )
					return false;
			}			
		}
		
		return true;
	}
	
	public static boolean isEqual( Creature a, Creature b )
	{
		if( a.action != b.action )
		{
			log( "action not equal" );
			return false;					
		}		
		
		if( a.angle != b.angle )
		{
			log( "angle not equal" );
			return false;
		}
		
		if( a.behaviour != b.behaviour )
		{
			log( "behaviour not equal" );
			return false;
		}
		
		if( a.dead != b.dead )
		{
			log( "dead not equal" );
			return false;
		}
		
		if( a.death_time != b.death_time )
		{
			log( "death time not equal" );
			return false;
		}
		
		if( a.force.x != b.force.x || a.force.y != b.force.y )
		{
			log( "force not equal" );
			return false;
		}
		
		if( a.health != b.health )
		{
			log( "health not equal" );
			return false;
		}
				
		
		if( a.id != b.id )
		{
			log( "id not equal" );
			return false;
		}
		
		if( a.max_speed != b.max_speed )
		{
			log( "max speed not equal" );
			return false;
		}
		
		if( a.owner != b.owner )
		{
			log( "owner not equal" );
			return false;
		}
		
		if( a.planet != b.planet )
		{
			log( "planet not equal" );
			return false;
		}		
		
		if( a.pos.x != b.pos.x || a.pos.y != b.pos.y )
		{
			log( "position not equal" );
			return false;
		}
		
		if( a.scale != b.scale )
		{
			log( "scale not equal" );
			return false;
		}
		
		if( a.target != b.target )
		{
			log( "target not equal" );
			return false;
		}
		
		if( a.shot != b.shot )
		{
			log( "shot not equal" );
			return false;
		}
		
		if( a.speed != b.speed )
		{
			log( "speed not equal" );
			return false;
		}
		
		if( a.strength != b.strength )
		{
			log( "strength not equal" );
			return false;
		}
		
		if( a.vel.x != b.vel.x || a.vel.y != b.vel.y )
		{
			log( "velocity not equal" );
			return false;
		}
			
		return true;
	}
	
	public static boolean isEqual( Tree a, Tree b )
	{
		if( a.health != b.health )
		{
			log( "health not equal" );
			return false;
		}
		
		if( a.id != b.id )
		{
			log( "id not equal" );
			return false;
		}
		
		if( a.planet != b.planet )
		{
			log( "planet not equal" );
			return false;
		}
		
		if( a.pos.x != b.pos.x || a.pos.y != b.pos.y )
		{
			log( "position not equal" );
			return false;
		}
		
		if( a.scale != b.scale )
		{
			log( "scale not equal" );
			return false;
		}
		
		log( "checking branches -" );
		if(! isEqual( a.root, b.root ) )
		{
			log( "branches not equal" );
			return false;
		}
		return true;
	}
	
	public static boolean isEqual( Branch a, Branch b )
	{
		if( a == null && b == null )
			return true;
		
		if( a == null && b != null )
			return false;
		
		if( a != null && b == null )
			return false;
		
		if( a.angle != b.angle )
		{
			log( "angles not equal" );
			return false;
		}
		
		if( a.creature != b.creature )
		{
			log( "creatures not equal" );
			return false;
		}
		
		if( a.curr_height != b.curr_height )
		{
			log( "current heigth not equal" );
			return false;
		}
		
		if( a.depth != b.depth )
		{
			log( "depth not equal" );
			return false;
		}
		
		if( a.dir.x != b.dir.x || a.dir.y != b.dir.y )
		{
			log( "dir not equal" );
			return false;
		}
		
		if( a.grown_creatures != b.grown_creatures )
		{
			log( "grown creatures not equal" );
			return false;
		}
		
		if( a.height != b.height )
		{
			log( "height not equal" );
			return false;
		}
		
		if( a.planet != b.planet )
		{
			log( "planet not equal" );
			return false;			
		}
		
		if( a.pos.x != b.pos.x || a.pos.y != b.pos.y )
		{
			log( "position not equal" );
			return false;
		}
		
		if( a.wither != b.wither )
		{
			log( "wither not equal" );
			return false;
		}
		
		if( !isEqual( a.children[0], b.children[0] ) )
		{
			log( "left children not equal" );
			return false;
		}
		
		if( !isEqual( a.children[1], b.children[1] ) )
		{
			log( "right children not equal" );
			return false;
		}		
		
		return true;
	}
	
	public static void main( String argv[] ) throws Exception
	{
		DataInputStream o_in = new DataInputStream(new FileInputStream( argv[0] ));
		DataInputStream in = new DataInputStream( new FileInputStream( argv[1] ) );
				
			while( o_in.available() != 0 && in.available() != 0 )
			{				
				Simulation o_sim = new Simulation( false );
				Simulation sim = new Simulation( false );							

				o_sim.readState( o_in );
				o_sim.setCommandTurnIncrement(o_in.readInt());
				o_sim.setNextCommandTurn( o_in.readInt() );
				o_sim.setTurn( o_in.readInt() );
				o_sim.setRandomCalls( o_in.readInt() );
				sim.readState( in );
				sim.setCommandTurnIncrement( in.readInt());
				sim.setNextCommandTurn( in.readInt() );
				sim.setTurn( in.readInt() );
				sim.setRandomCalls( in.readInt() );
				
				System.out.println( "============= checking turn " + o_sim.getTurn() );
			
				if( !SimulationComparator.isEqual(o_sim, sim) )
				{					
					System.out.println( "difference in turn " + o_sim.getTurn() );
					System.exit( 0 );
				}
			}		
			
			System.out.println( "all good!" );
	}
}
