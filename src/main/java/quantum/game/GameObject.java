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

import javax.media.opengl.GLCanvas;

import quantum.math.Vector2D;

public strictfp class GameObject 
{
	Vector2D pos = new Vector2D( );
	Simulation sim;
	int id = 0;	
	
	protected GameObject( Simulation sim )
	{
		this.sim = sim;
		this.id = sim.getNextId( );
	}
	
	public GameObject( Simulation sim, Vector2D pos )
	{
		this.sim = sim;
		this.pos.set(pos);
		this.id = sim.getNextId( );
	}
	
	public void setSimulation( Simulation sim )
	{
		this.sim = sim;
	}
	
	public Simulation getSimulation( )
	{
		return sim;
	}
	
	
	public int getId( )
	{
		return id;
	}
	
	public Vector2D getPosition( )
	{
		return pos;
	}	
	
	public void update( ) { };
	
	public void render( GLCanvas canvas ) { };		
	
	public void read( DataInputStream in ) throws Exception
	{
		id = in.readInt( );
		pos.x = in.readFloat();
		pos.y = in.readFloat();
	}
	
	public void write( DataOutputStream out ) throws Exception
	{
		out.writeInt( id );
		out.writeFloat( pos.x );
		out.writeFloat( pos.y );
	}
}
