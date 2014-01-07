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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import quantum.utils.Log;

/**
 * a simple irc client
 * 
 * @author Administrator
 *
 */
public class IRC 
{
	private class Channel
	{
		ArrayList<String> messages = new ArrayList<String>( );
		ArrayList<String> new_messages = new ArrayList<String>( );
		ArrayList<String> users = new ArrayList<String>( );
		String name = "";
		
		public Channel( String channel )
		{
			this.name = channel;
		}
		
		public void addMessage( String message )
		{
			messages.add( message );
			new_messages.add( message );
		}
		
		public void addUser( String user )
		{
			users.add( user );
		}
		
		public void clearUsers( )
		{
			users.clear( );
		}
		
		public void removeUser( String user )
		{
			Iterator<String> iter = users.iterator();
			while( iter.hasNext() )
				if( iter.next().equals( user ) )
					iter.remove();
		}
		
		public List<String> getUsers( )
		{
			return  users;
		}
		
		public List<String> getNewMessages( )
		{
			ArrayList<String> tmp = new ArrayList<String>();
			tmp.addAll( new_messages );
			new_messages.clear();
			return tmp;					
		}
		
		public List<String> getMessages( )
		{
			return messages;
		}

		public void addMessageExclusive(String receiver) {
			// TODO Auto-generated method stub
			messages.add( receiver );
		}

		public boolean hasNewMessages() {
			// TODO Auto-generated method stub
			return new_messages.size() > 0 ;
		}
	}
	
	final Socket socket;
	final BufferedReader in;
	final BufferedWriter out;
	String nick_name = "";
	String host = "";
	HashMap<String, Channel> channels = new HashMap<String, Channel>( );	
	ArrayList<String> users = new ArrayList<String>();
	Thread thread_recv;	
	boolean disconnected = false;
	
	public IRC( String url, int port, String nick_name ) throws Exception
	{
		socket = new Socket( );
		socket.connect( new InetSocketAddress( url, port ), 2000 );
		in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
		out = new BufferedWriter( new OutputStreamWriter( socket.getOutputStream() ) );											
		
		String line = in.readLine();		
				
		out.write( "NICK " + nick_name + "\r\n");
		out.write( "USER " + nick_name + " 0 * :" + nick_name + "\r\n" );				
		out.flush();
		
		while( ( line = in.readLine() ) != null )
		{			
			if( line.contains( "433" ) )
			{
				nick_name += "_";
			
				out.write( "NICK " + nick_name + "\r\n");
				out.write( "USER " + nick_name + " 0 * :" + nick_name + "\r\n" );	
				out.flush();
			}
			
			if( line.contains( "001" ) )
				break;
		}		

		this.nick_name = nick_name;
		this.host = url;				
		
		send( "join #quantum" );		
		out.flush( );
		
		Channel channel_quantum = new Channel( "#quantum" );
		//Channel channel_idle = new Channel( "#idle" );
		
		channels.put( "#quantum", channel_quantum );
		//channels.put( "#idle", channel_idle );

		thread_recv = new Thread( new Runnable() {

			public void run() {
								
				while( true )
				{
					try {
						String line = in.readLine();						
						synchronized( this )
						{							
							if( line.toUpperCase().contains( "PING" ) )
							{
								send( "PONG " + "irc." + host );
							}
							
							if( line.toUpperCase().contains( "JOIN" ) )
							{
								line = line.substring( 1 );
								String channel = line.split( ":" )[1];
								Channel ch = channels.get( channel );
								if( ch != null )
								{
									ch.addMessage( "** " + line.substring( 0, line.indexOf( "!" ) ) + " joined " + channel + " ** " );
									ch.addUser( line.substring( 0, line.indexOf( "!" ) ) );
								}								
							}
							
							if( line.toUpperCase().contains( "PART" ) )
							{		
								line = line.substring( 1 );
								String user =  line.substring( 0, line.indexOf( "!" ) );
								String channel;								
								channel = line.split( " " )[2]; 																
								Channel ch = channels.get( channel );
								if( ch != null )
								{
									ch.removeUser( user );
									ch.addMessage( "** " + user + " left " + channel + " **" );
								}
							}
							
							if( line.toUpperCase().contains( "QUIT" ))
							{		
								line = line.substring( 1 );
								String user =  line.substring( 0, line.indexOf( "!" ) );
																								 							
								
								for( Channel ch: channels.values() )
								{
									ch.removeUser( user );
									ch.addMessage( "** " + user + " quit **" );
								}
							}
							
							if( line.startsWith( ":" ) )
							{
								line = line.substring(1);
								String[] tokens = line.split( ":" );								
								if( tokens[0].contains( " 353 ")  )
								{
									String channel = tokens[0].split( " " )[4];
									Channel ch = channels.get( channel );
									if( ch != null )
									{
										ch.clearUsers();
										String[] usrs = tokens[1].split( " " );
										for( String user: usrs )
											ch.addUser( user );
									}									
								}
								
								if( tokens[0].contains( "PRIVMSG" ) )
								{
									
									String sender = tokens[0].split( "!" )[0];
									String receiver = line.split( " " )[2];
									String msg = tokens[1];
									Channel ch = channels.get( receiver );
									if( ch != null )
										ch.addMessage( sender + ": " + msg );
									else
									{
										for( Channel channel: channels.values() )
											channel.addMessage( sender + ": " + msg );
									}
								}
							}
														
						}
						
					} catch (Exception e) {						
						dispose();
						return;
					}
				}
			}
			
		} );		
		
		thread_recv.start();	
		Log.println( "[IRC] connected to apistudios, #quantum" );
	}	
	
	public String getNickName( )
	{
		return nick_name;
	}
	
	public List<String> getUsers( String channel )
	{
		synchronized( this )
		{
			ArrayList<String> tmp = new ArrayList<String>();
			Channel ch = channels.get( channel );
			if( ch != null )
				return ch.getUsers();
			else
				return tmp;
		}
	}
	
	public List<String> getMessages( String channel )
	{
		synchronized( this )
		{
			ArrayList<String> tmp = new ArrayList<String>();
			Channel ch = channels.get( channel );
			if( ch != null )
				return ch.getMessages();
			else
				return tmp;			
		}
	}
	

	public List<String> getNewMessages(String channel) {
		synchronized( this )
		{
			ArrayList<String> tmp = new ArrayList<String>();
			Channel ch = channels.get( channel );
			if( ch != null )
				return ch.getNewMessages();
			else
				return tmp;			
		}
	}
	
	public boolean isDisconnected( )
	{
		return disconnected;
	}
	
	public void sendText( String receiver, String sender, String msg ) throws Exception
	{
		synchronized( this )
		{
			if( channels.containsKey( receiver ) )
				channels.get( receiver ).addMessageExclusive( sender + ": " + msg );
			out.write( "PRIVMSG " + receiver + " :"  + msg + "\r\n" );
			out.flush();
		}
	}
	
	public void send( String msg ) throws Exception
	{
		synchronized( this )
		{
			out.write( msg + "\r\n" );
			out.flush();
		}
	}
	
	@SuppressWarnings("deprecation")
	public void dispose( )
	{		
		try {
			disconnected = true;
			if( socket != null )			
				socket.close();
			if( in != null )
				in.close();
			if( out != null )
				out.close();						
			if( thread_recv != null )
				thread_recv.stop();
		} catch (IOException e) {			
		}
		Log.println( "[IRC] disposed" );
	}
	
	public static void main( String[] argv ) throws Exception
	{
		IRC irc = new IRC( "apistudios.com", 6667, "marzec" );
		
		BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
		
		while( true )
		{
			System.out.println( "> s" );
			String line = in.readLine();
			if( line.equals( "quit" ) )
				break;
			else
				irc.send( line );
			
			System.out.println( "=====" );
			for( String user: irc.getUsers( "#quantum" ) )
				System.out.println( user );
			System.out.println( "=====" );
		}
		
		irc.dispose();
	}

	public boolean hasNewMessages(String channel) 
	{
		synchronized( this  )
		{
		
			if( channels.containsKey( channel ) )
				return channels.get( channel ).hasNewMessages( );
			else
				return false;
		}
	}
}
