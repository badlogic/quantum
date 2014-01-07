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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public strictfp class CommandBuffer implements Iterable<Command>
{
	int turn_id = 0;
	ArrayList<Command> commands = new ArrayList<Command>();
	
	public CommandBuffer( int turn_id )
	{
		this.turn_id = turn_id;
	}
	
	public int getTurnId( )
	{
		return turn_id;
	}
	
	public void setTurnId( int turn_id )
	{
		this.turn_id = turn_id;
	}
		
	public void clear( )
	{
		commands.clear();
	}
	
	public void add( Command cmd )
	{
		commands.add(cmd);
	}
	
	public void add( CommandBuffer buffer )
	{
		commands.addAll( buffer.commands );
	}
	
	public Iterator<Command> iterator() 
	{		
		return commands.iterator();
	}
	
	public List<Command> getCommands( )
	{
		return commands;
	}
	
	public void read( DataInputStream in ) throws Exception
	{
		turn_id = in.readInt();
		int n = in.readShort( );
		for( int i = 0; i < n; i ++ )
		{
			int type = in.readByte();
			switch( type )
			{
			case CommandTypes.MOVE_CREATURE:
				MoveCreatureCommand cmd = new MoveCreatureCommand( );
				cmd.read( in );
				commands.add(cmd);
				break;
			case CommandTypes.PLANT_TREE:
				PlantTreeCommand p_cmd = new PlantTreeCommand( );
				p_cmd.read( in );
				commands.add(p_cmd);
				break;
			case CommandTypes.CHAIN:
				ChainCommand c_cmd = new ChainCommand( );
				c_cmd.read( in );
				commands.add( c_cmd );
				break;
			}
		}
	}
	
	public void write( DataOutputStream out ) throws Exception
	{
		out.writeInt( turn_id );
		out.writeShort( commands.size() );
		for( Command cmd: commands )
			cmd.write( out );
	}
}
