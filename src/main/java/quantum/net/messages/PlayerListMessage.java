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
import java.util.ArrayList;
import java.util.List;

public strictfp class PlayerListMessage extends Message 
{
	List<Integer> ids = new ArrayList<Integer>( );
	List<String> names = new ArrayList<String>( );
	
	public PlayerListMessage( ) 
	{
		super(MessageTypes.PLAYER_LIST);
	}
	
	public PlayerListMessage( List<Integer> ids, List<String> names )
	{
		super(MessageTypes.PLAYER_LIST);
		this.ids = ids;
		this.names = names;
	}
	
	public List<Integer> getIds( )
	{
		return ids;
	}
	
	public List<String> getNames( )
	{
		return names;
	}
	
	public String toString( )
	{
		String text = "";
		for( String name: names )
			text += name + " ";
		return text;
	}
	
	@Override
	public void read(DataInputStream in) throws Exception 
	{	
		ids.clear();
		names.clear();
		
		int n = in.readShort();
		for( int i = 0; i < n; i++ )
		{
			ids.add( (int)in.readShort() );
			names.add( readString( in ) );
		}
	}
	@Override
	public void write(DataOutputStream out) throws Exception 
	{	
		out.writeInt( type );
		out.writeShort( ids.size() );
		for( int i = 0; i < ids.size(); i++ )
		{
			out.writeShort(ids.get(i));
			writeString( out, names.get(i) );
		}
		out.flush();
	}	
}
