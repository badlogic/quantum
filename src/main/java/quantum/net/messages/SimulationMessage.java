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
package quantum.net.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import quantum.game.Simulation;

public strictfp class SimulationMessage extends Message 
{
	int id = 0;
	Simulation sim = new Simulation( false );
	
	public SimulationMessage( )
	{
		super( MessageTypes.SIMULATION );
	}
	
	public SimulationMessage( int id, Simulation sim )
	{
		super( MessageTypes.SIMULATION );
		this.id = id;
		this.sim = sim;
	}

	public int getId( )
	{
		return id;
	}
	
	public Simulation getSimulation( )
	{
		return sim;
	}
	
	@Override
	public void read(DataInputStream in) throws Exception 
	{
		id = in.readShort();
		sim.readState( in );
	}

	@Override
	public void write(DataOutputStream out) throws Exception 
	{
		out.writeInt( type );
		out.writeShort( id );
		sim.writeState( out );
		out.flush();
	}
}
