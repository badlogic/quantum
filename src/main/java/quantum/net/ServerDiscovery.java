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
package quantum.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import quantum.utils.Log;

public strictfp class ServerDiscovery implements Runnable
{
	public strictfp class ServerEntry
	{
		public String name = "";
		public String ip = "";
		public int port = 0;
		public boolean is_lan = false;
		public long time_stamp;
		
		public ServerEntry( String name, String ip, int port, boolean is_lan )
		{
			this.name = name;
			this.ip = ip;
			this.port = port;
			this.time_stamp = System.nanoTime();
			this.is_lan = is_lan;
		}
		
		public boolean isLan( )
		{
			return is_lan;
		}
		
		public String toString( )
		{
			return name;
		}
		
		public boolean equals( ServerEntry entry )
		{
			return name.equals(entry.name) && ip.equals(entry.ip) && port == entry.port;
		}
	}
	
	byte[] buf = new byte[20000];
	
	ArrayList<ServerEntry> servers = new ArrayList<ServerEntry>();	
	MulticastSocket broadcast_socket;
	InetAddress group;
	Thread thread;
	
	public ServerDiscovery( ) throws Exception
	{		
		group = InetAddress.getByName("230.0.0.1");
		broadcast_socket = new MulticastSocket( 4446 );		
		broadcast_socket.joinGroup( group );
		thread = new Thread( this );
		thread.start();
	}
	
	@SuppressWarnings("deprecation")
	public void dispose()
	{
		thread.stop();
		broadcast_socket.close();
	}
	
	public void run( )
	{
		while( true )
		{
			DatagramPacket packet;		
			packet = new DatagramPacket(buf, buf.length);
			try {
				broadcast_socket.receive(packet);
				String received = new String(packet.getData(), 0, packet.getLength());
				String[] tokens = received.split( ":" );
				ServerEntry entry = new ServerEntry( tokens[2], tokens[0], Integer.parseInt(tokens[1]), true );
				synchronized( servers )
				{
					boolean found = false;
					for( ServerEntry server: servers )
					{
						if( server.equals(entry) )
						{
							server.time_stamp = System.nanoTime();
							found = true;
							break;
						}
					}
					if( !found )
						servers.add( entry );											
				}
			} catch (Exception e) {
				break;
			}		 			
		}
	}	
	
	public List<ServerEntry> getServers( )
	{
		ArrayList<ServerEntry> entries = new ArrayList<ServerEntry>( );
		
		synchronized( servers )
		{							
			Iterator<ServerEntry> iter = servers.iterator();
			while( iter.hasNext() )
				if( (System.nanoTime() - iter.next().time_stamp) / 1000000000.0 > 10 )
					iter.remove();
			
			entries.addAll( servers );	
		}
			
		if( !dont_check_inet )
		{
			try {
				URL url_apiservers = new URL( "http://www.apistudios.com/quantum/list.php" );
				URLConnection connection = url_apiservers.openConnection();
				connection.setConnectTimeout( 1500 );
				connection.connect();
				BufferedReader in = new BufferedReader(	new InputStreamReader( connection.getInputStream()));
				String line = null;
				while( (line = in.readLine() ) != null )
				{
					String tokens[] = line.split( ";" );
					String ip[] = tokens[1].split(":");
					ServerEntry entry = new ServerEntry( tokens[0], ip[0], Integer.parseInt( ip[1] ), false );
					entries.add( entry );
				}				
			} catch (Exception e) {
				Log.println( "[ServerDiscovery] not checking servers on apistudios.com: " + Log.getStackTrace( e ) );
				dont_check_inet = true;
			}			
		}
		
		return entries;		
	}
	
	boolean dont_check_inet = true;
	
	public static void main( String[] argv ) throws Exception
	{
		ServerDiscovery discovery = new ServerDiscovery( );
		
		while( true )
		{
			for( ServerEntry entry: discovery.getServers() )
				System.out.print( entry + " " );
			System.out.println();
			Thread.sleep( 2000 );
		}
	}
}
