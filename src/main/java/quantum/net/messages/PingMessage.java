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

public strictfp class PingMessage extends Message
{	
	int id = 0;
	long time_stamp = 0;
	double last_ping = 0;
	
	public PingMessage( ) 
	{
		super(MessageTypes.PING);	
	}
	
	public PingMessage( int id )
	{
		super( MessageTypes.PING );
		this.id = id;
		this.time_stamp = System.nanoTime();
	}
	
	public int getId( )
	{
		return id;
	}
	
	public long getTimeStamp( )
	{
		return time_stamp;
	}
	
	public void updateTimeStamp( )
	{
		time_stamp = System.nanoTime();
	}
	
	public void setLastPing( double ping )
	{
		last_ping = ping;
	}
	
	public double getLastPing( )
	{
		return last_ping;
	}

	@Override
	public void read(DataInputStream in) throws Exception 
	{			
		id = in.readShort();
		time_stamp = in.readLong();
		last_ping = in.readDouble();
	}

	@Override
	public void write(DataOutputStream out) throws Exception 
	{	
		out.writeInt( type );
		out.writeShort( id );
		out.writeLong( time_stamp );
		out.writeDouble( last_ping );
		out.flush();
	}
}
