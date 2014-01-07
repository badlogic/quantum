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
package quantum.game.commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public strictfp class MoveCreatureCommand extends Command
{
	int id = 0;
	int src_planet;
	int dst_planet;
	int units;
	
	public MoveCreatureCommand( )
	{
		super( CommandTypes.MOVE_CREATURE );
	}
	
	public MoveCreatureCommand( int id, int src_planet, int dst_planet, int units )
	{
		super( CommandTypes.MOVE_CREATURE );
		this.id = id;
		this.src_planet = src_planet;
		this.dst_planet = dst_planet;
		this.units = units;
	}	
	
	public int getId( )
	{
		return id;
	}
	
	public int getSourcePlanet( )
	{
		return src_planet;
	}
	
	public int getDestinationPlanet( )
	{
		return dst_planet;
	}
	
	public int getUnits( )
	{
		return units;
	}
	
	@Override
	public void read(DataInputStream in) throws Exception 
	{
		id = in.readShort();
		src_planet = in.readShort();
		dst_planet = in.readShort();
		units = in.readShort();
		
	}
	
	@Override
	public void write(DataOutputStream out) throws Exception 
	{	
		out.writeByte( type );
		out.writeShort( id );
		out.writeShort( src_planet );
		out.writeShort( dst_planet );
		out.writeShort( units );
	}
}
