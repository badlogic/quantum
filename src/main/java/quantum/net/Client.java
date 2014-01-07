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


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import quantum.game.Constants;
import quantum.game.Player;
import quantum.math.WindowedMean;
import quantum.net.messages.DisconnectedMessage;
import quantum.net.messages.Message;
import quantum.net.messages.MessageDecoder;
import quantum.net.messages.PlayerListMessage;
import quantum.net.messages.PlayerMessage;
import quantum.net.messages.TextMessage;
import quantum.net.messages.VersionMessage;
import quantum.utils.ByteArrayStream;

public strictfp class Client 
{
	private ArrayList<Player> players = new ArrayList<Player>();;	
	public boolean ready = false;
	private DataInputStream in;
	private DataOutputStream out;
	public Socket socket;
	protected ByteArrayStream bytes = new ByteArrayStream( );
	protected DataOutputStream out_internal = new DataOutputStream( bytes );
	protected int bytes_to_read = -1;
	public WindowedMean ping_mean = new WindowedMean( 10 );	
	private PlayerListMessage player_list;
	private ArrayList<String> log = new ArrayList<String>( );		
	
	public Client( )
	{				
	}
	
	public Client( Socket socket ) throws Exception
	{
		socket.setTcpNoDelay(true);		
		this.socket = socket;
		this.in = new DataInputStream( socket.getInputStream() );
		this.out = new DataOutputStream( socket.getOutputStream() );				
	}
	
	public Client( String name, String ip, int port ) throws Exception
	{
		Player player = new Player( name, -1 );
		players.add( player );
		this.socket = new Socket( );
		this.socket.connect( new InetSocketAddress( ip, port ), 5000 );
		this.socket.setTcpNoDelay(true);
		this.in = new DataInputStream( socket.getInputStream() );
		this.out = new DataOutputStream( socket.getOutputStream() );
		sendMessage( new VersionMessage( ) );
		VersionMessage v_msg = null;
		while( v_msg == null )
			v_msg = (VersionMessage)readMessage();		
		if( v_msg.getVersion() != Constants.VERSION )
			throw new RuntimeException( "Version mismatch" );
		
		sendMessage( new PlayerMessage( name ) );
		
		PlayerMessage msg = null;
		while( msg == null )
			msg = (PlayerMessage)readMessage();
					
		getPlayer().setId(msg.getId());
	}
	
	public Client(String name) 
	{
		Player player = new Player(name, 0 );
		players.add( player );
	}

	public void sendMessage( Message msg ) throws Exception
	{
		if( socket == null )
			return;
		
		bytes.reset();
		msg.write(out_internal);
		out.writeInt(bytes.size());		
		out.write(bytes.getArray(), 0, bytes.size() );
		out.flush();
	}		
	
	public Message readMessage( ) throws Exception
	{						
		if( socket == null )
			return null;		
		
		Message msg = internalReadMessage( );
		
		if( msg == null )
			return null;
		
		if( msg instanceof PlayerListMessage )
			player_list = (PlayerListMessage)msg;
		
		if( msg instanceof TextMessage )
			log.add( ((TextMessage)msg).getName() + ": " + ((TextMessage)msg).getMessage() );
		
		if( msg instanceof DisconnectedMessage )
			log.add( ((DisconnectedMessage)msg).getName() + ": disconnected" );				
		
		return msg;
	}
	
	private Message internalReadMessage( ) throws Exception
	{			
		if( bytes_to_read != -1 )
		{
			if( in.available() >= bytes_to_read || in.available() == socket.getReceiveBufferSize() )
			{
				bytes_to_read = -1;
				Message msg = MessageDecoder.decode(in);
				
				return msg;
			}
			else
				return null;
		}
		
		if( in.available() >= Integer.SIZE / 8 )
		{
			bytes_to_read = in.readInt();
			if( in.available() >= bytes_to_read )
			{
				bytes_to_read = -1;
				Message msg = MessageDecoder.decode(in);				
				return msg;
			}			
		}
		
		return null;
	}
		
	
	public void dispose( )
	{
		if( socket == null )
			return;
		
		DisconnectedMessage msg = new DisconnectedMessage();
		try {
			sendMessage(msg);
			socket.close();
		} catch (Exception e) {
		}						
	}

	public void removeClient( int id )
	{
		if( player_list == null )
			return;
		
		int idx = 0;
		for( int i = 0; i < player_list.getIds().size(); i++ )
		{
			if( player_list.getIds().get(i) == id )
			{
				idx = i;
				break;
			}
		}
		
		player_list.getIds().remove(idx);
		player_list.getNames().remove(idx);
	}
	
	public void setPlayerList(PlayerListMessage cm) 
	{ 
		player_list = cm;
	}
	
	public PlayerListMessage getPlayerList( )
	{
		return player_list;
	}

	public List<String> getLog() 
	{	
		return log;
	}

	public Player getPlayer() 
	{	
		return players.get(0);
	}
	
	public List<Player> getPlayers( )
	{
		return players;
	}

	public void addPlayer(Player player) 
	{
		players.add( player );		
	}
	
	public void removePlayer( Player player )
	{
		players.remove( player );
	}
}
