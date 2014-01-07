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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bsh.EvalError;
import bsh.Interpreter;
import quantum.math.Vector2D;
import quantum.net.Client;
import quantum.net.messages.Message;
import quantum.net.messages.PlayerMessage;
import quantum.utils.Log;

public class Bot 
{	
	Interpreter interpreter = new Interpreter( );
	int id = 0;
	Simulation sim;
	List<Integer> planets = new ArrayList<Integer>();
	HashMap<Integer, Integer> planets_lookup = new HashMap<Integer, Integer>( );	
	
	int[] friendly;
	int[] enemy;
	int[] moveable;		
	int total_creatures = 0;
	
	int last_taken_turn = 0;
	boolean wait = false;
	Player player;
	
	public Bot( String file, int id ) throws Exception
	{
		interpreter.source( file );
		interpreter.set( "simulation", this );
		this.id = id;
	}
	
	public Bot( String file, String name, Client client ) throws Exception
	{
		interpreter.source( file );
		PlayerMessage msg = new PlayerMessage( name );
		client.sendMessage( msg );
		msg = null;
		while( msg == null )
		{
			Message m = client.readMessage();
			if( m != null && m instanceof PlayerMessage )
				msg = (PlayerMessage) m;
		}
		
		
		player = new Player( msg.getName(), msg.getId() );
		
		this.id = msg.getId();
		
		interpreter.set( "simulation", this );		
	}
	
	public void dispose( Client client )
	{
		PlayerMessage msg = new PlayerMessage( player.getName(), player.getId(), true );
		try {
			client.sendMessage( msg );
		} catch (Exception e) {
			Log.println( "[Bot] couldn't send PlayerMessage: " + Log.getStackTrace( e ) );
			e.printStackTrace();
		}
	}
	
	public void update( Simulation sim )
	{
		if( sim.getTurn() == last_taken_turn )
			return;			
				
		if( sim.getTurn() == sim.getNextCommandTurn() )
		{
			if( wait )
			{
				wait = !wait;
				last_taken_turn = sim.getTurn();
				return;
			}
			else
			{			
				last_taken_turn = sim.getTurn();
				wait = !wait;
				
				if( this.sim == null )
				{
					this.sim = sim;
					planets.clear();
					for( Planet planet: sim.getPlanets() )					
					{
						planets_lookup.put( planet.id, planets.size() );
						planets.add( planet.id );
					}
					
					this.enemy = new int[planets.size()];
					this.friendly = new int[planets.size()];
					this.moveable = new int[planets.size()];					
				}
				
				gatherStatistics( );
				
				try {
					interpreter.eval( "update();" );
				} catch (EvalError e) {
					Log.println( "[Bot] error calling update method of bot: " + e.getErrorSourceFile() + ":" + e.getErrorLineNumber() + " - " + e.getErrorText() );					
				}		
			}
		}		
	}
	
	private void gatherStatistics( )
	{
		for( int i = 0; i < planets.size(); i++ )
		{
			enemy[i] = 0;
			friendly[i] = 0;
			moveable[i] = 0;			
		}
		
		total_creatures = 0;		
		
		for( int planet: planets )
		{
			Planet p = sim.getPlanet( planet );					
			
			int idx = planets_lookup.get( planet );
			for( Creature creature: p.getCreatures() )
			{
				if( creature.getOwner() != this.id )
					enemy[idx]++;
				else
				{
					friendly[idx]++;
					total_creatures++;
					if( creature.isBorn() && !creature.isDying() && (creature.isOrbiting() || creature.isAttacking() ) )
						moveable[idx]++;
				}			
			}
		}
		
		gatherConnectedPlanets( sim );
				
	}
	
	HashSet<Planet> visited = new HashSet<Planet>();
	List<Planet> unvisited = new ArrayList<Planet>();
	HashSet<Vector2D> components = new HashSet<Vector2D>();
	int component_size = 0;
	
	private void gatherConnectedPlanets( Simulation sim )
	{
		visited.clear();
		unvisited.clear();
		components.clear();
		
		for( Planet planet: sim.getPlanets() )
		{
			if( planet.getOwner() != -1 && planet.getOwner() != this.id )
				unvisited.add( planet);			
		}
		
		while( unvisited.size() != 0 )
		{
			component_size = 0;
			Planet planet = unvisited.remove(0);
			Vector2D center = new Vector2D( );
			components.add( center );
			int num_planets = gatherConnectedPlanetsRecursive( sim, planet, center );
			center.mul( 1.0f / num_planets );			
		}					
	}
	
	private int gatherConnectedPlanetsRecursive( Simulation sim, Planet planet, Vector2D center )
	{		
		center.add( planet.getPosition() );
		visited.add( planet );
		unvisited.remove( planet );
		int num_planets = 1;
		component_size++;
		if( component_size > 3 )
			return num_planets;
		
		for( int neighbour: planet.getReachablePlanets() )
		{
			Planet neighbour_planet = sim.getPlanet( neighbour );
			if( neighbour_planet.getOwner() == planet.getOwner() && visited.contains( neighbour_planet ) == false )			
				num_planets += gatherConnectedPlanetsRecursive( sim, neighbour_planet, center );			
		}
		
		return num_planets;
	}
	
	public boolean isOwnedPlanet( int id )
	{
		return sim.getPlanet( id ).getOwner() == this.id;
	}
	
	public boolean isEnemyPlanet( int id )
	{
		return sim.getPlanet( id ).getOwner() != this.id && sim.getPlanet( id ).getOwner() != -1;
	}	
		
	public boolean isFreePlanet( int id )
	{
		return sim.getPlanet( id ).getOwner() == -1;
	}
	
	public boolean hasCreatureMovingTo( int id, int id2 )
	{		
		for( Creature creature: sim.getPlanet( id ).getCreatures() )
		{
			if( creature.getOwner() == this.id && creature.isMoving() && creature.getTarget( ) == id2 )
				return true;
		}
		
		return false;
	}
	
	public int numberOfTrees( int id )
	{
		return sim.getPlanet( id ).getTrees().size();
	}
	
	public int numberOfCreatures( int id )
	{
		return sim.getPlanet( id ).getCreatures().size();
	}
	
	public int numberOfFriendlyCreatures( int id )
	{
		return friendly[planets_lookup.get(id)];
	}
	
	public int numberOfMoveableCreatures( int id )
	{
		return moveable[planets_lookup.get(id)];
	}
	
	public int numberOfEnemyCreatures( int id )
	{
		return enemy[planets_lookup.get(id)];
	}
	
	public int numberAvailableResources( int id )
	{
		return sim.getPlanet( id ).getResources();
	}
	
	public int numberMaximumResources( int id )
	{
		return sim.getPlanet( id ).getResources();
	}
	
	public void plantTree( int id )
	{
		sim.plantTree( this.id, id );
	}
	
	public void moveCreatures( Integer from_id, Integer to_id, Integer units )
	{
		sim.moveCreatures( this.id, from_id, to_id, units );
	}
	
	@SuppressWarnings("unchecked")
	public List getPlanets( )
	{
		return planets;
	}
	
	Vector2D tmp = new Vector2D( );
	
	public int getNearestNeighbourToEnemy( int id )
	{
		float min_dist = Float.MAX_VALUE;
		int planet = -1;			
		
		for( Vector2D center: components )
		{					
			tmp.set( center );
			
			for( int n: sim.getPlanet( id ).getReachablePlanets() )
			{
				float dst = tmp.dst2( sim.getPlanet( n ).getPosition() );
				
				if( min_dist > dst )
				{
					min_dist = dst;
					planet = n;
				}
			}
		}
	
		if( planet == -1 )
			return sim.getPlanet( id ).getReachablePlanets( ).iterator().next();
		else
			return planet;
	}
	
	public boolean creatureLimitReached( )
	{
		return total_creatures >= Constants.MAX_CREATURES;
	}
	
	@SuppressWarnings("unchecked")
	public Set getPlanetNeighbours( int id )
	{
		return sim.getPlanet(id).getReachablePlanets();
	}
	
	public float getHealth( int id )
	{
		return sim.getPlanet(id).getHealth();	
	}
	
	public float getStrength( int id )
	{
		return sim.getPlanet( id ).getStrength();
	}
	
	public float getSpeed( int id ) 
	{
		return sim.getPlanet( id ).getSpeed( );
	}
	
	public int getNearestEnemyPlanet( int id )
	{
		Planet src = sim.getPlanet( id );
		Planet dst = null;
		float min_dist = Float.MAX_VALUE;
		
		for( Planet planet: sim.getPlanets() )
		{
			if( planet.getOwner() == this.id )
				continue;
			if( planet.getOwner() == -1 )
				continue;
			
			float dist = planet.getPosition().dst2( src.getPosition() );
			if( dist < min_dist )
			{
				min_dist = dist;
				dst = planet;
			}
		}
		
		if( dst == null )
			return id;
		else
			return dst.getId();			
	}
	
	HashSet<Planet> open_list = new HashSet<Planet>( );
	HashSet<Planet> closed_list = new HashSet<Planet>( );
	HashMap<Planet, Planet> parent = new HashMap<Planet, Planet>( );
	HashMap<Planet, Float> g_cost = new HashMap<Planet, Float>( );
	HashMap<Planet, Float> f_cost = new HashMap<Planet, Float>( );
	
	public int findPath( int from, int to )
	{
		if( from == to )
			return from;
		
		Planet start = sim.getPlanet( from );
		Planet end = sim.getPlanet( to );
		
		open_list.clear( );
		closed_list.clear( );
		parent.clear();
		g_cost.clear();
		f_cost.clear();
		
		open_list.add( start );
		f_cost.put( start, start.getPosition().dst2( end.getPosition() ) );
		g_cost.put( start, 0.0f );
		
		boolean found_end = false;
		
		while( open_list.size() > 0 )
		{
			Planet p = getLowestFScorePlanet( end );
			open_list.remove( p );
			closed_list.add( p );
			if( p == end )
			{
				found_end = true;
				break;
			}
			
			for( int neighbour: p.getReachablePlanets() )
			{
				Planet n = sim.getPlanet( neighbour );
				if( closed_list.contains( n ) )
					continue;
				
				if( open_list.contains( n ) == false )
				{
					open_list.add( n );
					float g = g_cost.get(p) + n.getPosition().dst2( n.getPosition() );				
					g_cost.put( n, g );
					f_cost.put( n, n.getPosition().dst2( end.getPosition() ) + g );
					parent.put( n, p );
				}
				else
				{
					float g = g_cost.get(p) + n.getPosition().dst2( n.getPosition() );
					if( g < g_cost.get(n) )
					{				
						g_cost.put( n, g );
						f_cost.put( n, n.getPosition().dst2( end.getPosition() ) + g );
						parent.put( n, p );
					}
				}
			}
		}
		
		if( found_end == false )
			return from;
		
		Planet cur = end;
		while( true )
		{
			Planet p = parent.get( cur );
			if( p == start )			
				return cur.getId();			
				
			cur = p;
		}
	}
	
	public int findSafePath( int from, int to )
	{
		if( from == to )
			return from;
		
		
		Planet start = sim.getPlanet( from );
		Planet end = sim.getPlanet( to );
		
		open_list.clear( );
		closed_list.clear( );
		parent.clear();
		g_cost.clear();
		f_cost.clear();
		
		open_list.add( start );
		f_cost.put( start, start.getPosition().dst2( end.getPosition() ) );
		g_cost.put( start, 0.0f );
		
		boolean found_end = false;
		
		while( open_list.size() > 0 )
		{
			Planet p = getLowestFScorePlanet( end );
			open_list.remove( p );
			closed_list.add( p );
			if( p == end )
			{
				found_end = true;
				break;
			}
			
			for( int neighbour: p.getReachablePlanets() )
			{
				Planet n = sim.getPlanet( neighbour );
				if( closed_list.contains( n ) || (n.getOwner() != -1 && n.getOwner() != this.id && n != end ))
					continue;
				
				if( open_list.contains( n ) == false )
				{
					open_list.add( n );
					float g = g_cost.get(p) + n.getPosition().dst2( n.getPosition() );				
					g_cost.put( n, g );
					f_cost.put( n, n.getPosition().dst2( end.getPosition() ) + g );
					parent.put( n, p );
				}
				else
				{
					float g = g_cost.get(p) + n.getPosition().dst2( n.getPosition() );
					if( g < g_cost.get(n) )
					{				
						g_cost.put( n, g );
						f_cost.put( n, n.getPosition().dst2( end.getPosition() ) + g );
						parent.put( n, p );
					}
				}
			}
		}
		
		if( found_end == false )
			return from;
		
		Planet cur = end;
		while( true )
		{
			Planet p = parent.get( cur );
			if( p == start )			
				return cur.getId();			
				
			cur = p;
		}
	}
	
	private Planet getLowestFScorePlanet( Planet end )
	{
		Planet lowest = null;
		float score = Float.MAX_VALUE;
		
		for( Planet planet: open_list )
		{
			float f = f_cost.get( planet );
			if( score > f_cost.get( planet ) )
			{
				lowest = planet;
				score = f;
			}
		}
		
		return lowest;
	}

	public int getId() 
	{	
		return id;
	}
}
