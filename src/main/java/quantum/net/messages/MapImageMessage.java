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

public strictfp class MapImageMessage extends Message 
{
	Simulation sim;
	String name;
	
	public MapImageMessage() 
	{
		super(MessageTypes.MAP_IMAGE);	
	}
	
	public MapImageMessage( String name, Simulation sim )
	{
		super(MessageTypes.MAP_IMAGE);
		this.sim = sim;
		this.name = name;
	}

	@Override
	public void read(DataInputStream in) throws Exception 
	{
		sim = new Simulation( false );
		sim.readState( in );
		name = readString( in );
	}

	@Override
	public void write(DataOutputStream out) throws Exception 
	{
		out.writeInt( type );
		if( sim == null )
			new Simulation( false ).writeState( out );
		else
			sim.writeState( out );
		writeString( out, name );
	}

	public String getName() 
	{
		return name;		
	}

	public Simulation getSimulation() 
	{	
		return sim;
	}
	
}
