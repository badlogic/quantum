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

public strictfp class VoteMessage extends Message
{
	int id;
	String user;
	String name;
	
	public VoteMessage() 
	{
		super(MessageTypes.VOTE);
	}
	
	public VoteMessage( int id, String user, String name )
	{
		super(MessageTypes.VOTE);
		this.id = id;
		this.user = user;
		this.name = name;
	}
	
	public int getId( )
	{
		return id;
	}
	
	public String getUser( )
	{
		return user;
	}
	
	public String getName( )
	{
		return name;
	}

	@Override
	public void read(DataInputStream in) throws Exception 
	{	
		id = in.readShort( );
		user = readString( in );
		name = readString( in );
	}

	@Override
	public void write(DataOutputStream out) throws Exception 
	{	
		out.writeInt( type );
		out.writeShort( id );
		writeString( out, user );
		writeString( out, name );
	}

}
