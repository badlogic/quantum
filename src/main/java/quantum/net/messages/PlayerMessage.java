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

public class PlayerMessage extends Message
{
	String name = "";
	int id = -1;
	boolean remove = false;

	public PlayerMessage() {
		super(MessageTypes.PLAYER);
	}

	public PlayerMessage( String name )
	{
		super(MessageTypes.PLAYER);
		this.name = name;
		this.remove = false;
	}
	
	public PlayerMessage( String name, int id, boolean remove )
	{
		super(MessageTypes.PLAYER);
		this.name = name;
		this.id = id;
		this.remove = remove;
	}
	
	public String getName( )
	{
		return name;
	}
	
	public boolean isRemove( )
	{
		return remove;
	}
	
	public int getId( )
	{
		return id;
	}
	
	public void setId( int id )
	{
		this.id = id;
	}
	
	@Override
	public void read(DataInputStream in) throws Exception 
	{
		name = readString( in );
		remove = in.readBoolean();		
		id = in.readShort();
	}

	@Override
	public void write(DataOutputStream out) throws Exception 
	{		
		out.writeInt( type );
		writeString( out, name );
		out.writeBoolean( remove );
		out.writeShort( id );
	}

}
