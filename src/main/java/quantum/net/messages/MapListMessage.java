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

public strictfp class MapListMessage extends Message
{
	ArrayList<String> maps = new ArrayList<String>( );
	ArrayList<String> names = new ArrayList<String>( );
	public MapListMessage( ) 
	{
		super(MessageTypes.MAP_LIST);
		
	}
	
	public MapListMessage( String[] maps, String[] names )
	{
		super(MessageTypes.MAP_LIST);
		for( int i = 0; i < maps.length; i++ )
		{
			this.maps.add( maps[i] );
			this.names.add( names[i] );
		}
	}
	
	public List<String> getMaps( )
	{
		return maps;
	}	
	
	public List<String> getNames( )
	{
		return names;
	}

	@Override
	public void read(DataInputStream in) throws Exception 
	{			
		String[] files = readString(in).split("\n");
		for( int i = 0; i < files.length / 2; i++ )
			maps.add( files[i] );
		for( int i = files.length / 2; i < files.length; i++ )
			names.add( files[i] );
	}

	@Override
	public void write(DataOutputStream out) throws Exception 
	{
		out.writeInt( type );
		String files = "";
		
		for( String map: maps )
			files += map + "\n";	
		
		for( String name: names )
			files += name + "\n";
		
		writeString( out, files );
	}
}
