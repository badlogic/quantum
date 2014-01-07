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
package quantum.game;

import javax.media.opengl.GLCanvas;

import quantum.gfx.Renderer;
import quantum.net.Client;
import quantum.net.messages.CommandBufferMessage;
import quantum.net.messages.GameOverMessage;
import quantum.net.messages.Message;
import quantum.net.messages.PingMessage;
import quantum.utils.Log;
import quantum.utils.Timer;

public strictfp class GameLoop 
{
	private Client client;
	Simulation sim;
	PingMessage ping = new PingMessage( );		
	double rest_time = 0;	
	Renderer renderer;
	Timer timer = new Timer();
	GameInterface gui;
	boolean game_over;
	boolean shader_off;
	private boolean is_disconnected = false;
	Timer time_out = new Timer();	
	int last_written_turn = -1;
	boolean log_enabled = false;
	GameRecorder recorder;
	
	public GameLoop( Client client, Simulation sim, boolean shader_off )
	{
		this.setClient(client);		
		this.sim = sim;
		timer.start( );
		try {
			recorder = new GameRecorder( sim, client.getPlayerList() );
		} catch (Exception e1) {
			Log.println( "[GameLoop] couldn't create recorder: " + Log.getStackTrace( e1 ) );
			recorder = null;
		}
		this.shader_off = shader_off;
	}
	
	public GameLoop( Client client, Simulation sim )
	{
		this.setClient(client);
		this.sim = sim;
		timer.start( );
		time_out.start( );
		try {
			recorder = new GameRecorder( sim, client.getPlayerList() );			
		} catch (Exception e1) {
			Log.println( "[GameLoop] couldn't create recorder: " + Log.getStackTrace( e1 ) );
			recorder = null;
		}
	}
	
	public void setLogging( boolean value )
	{
		log_enabled = value;
	}
	
	public Renderer getRenderer( )
	{
		return renderer;
	}
	
	/**
	 * processes network inputs, updates the simulation
	 * and send the ping periodically to the server. renders
	 * the simulation afterwards. will try to update at the
	 * target framerate Constants.TURN_TIME
	 * 
	 * @param engine
	 * @param canvas
	 */
	public void update( GLCanvas canvas )
	{	
		rest_time += timer.getElapsedSeconds() * 1000;		
		rest_time = Math.min( rest_time, 250 );
		timer.stop();
		timer.start();
		while( rest_time > Constants.TURN_TIME )
		{
			update( );
			rest_time -= Constants.TURN_TIME;
		}				
	}		

	/**
	 * 
	 */
	protected void update( )
	{
		try 
		{	
			//
			// get network input
			//
			if( getClient() != null )
				handleMessages( );						
			
			//
			// update simulation
			//
			sim.update();										
			
			// send ping
			if( sim.getTurn() % 30 == 0 )
			{
				ping.updateTimeStamp();		
				if( getClient() != null )
					getClient().sendMessage( ping );
			}			
		
			
			//
			// check when we received the last message form 
			// the server
			//
			if( time_out.getElapsedSeconds() > 10 )
				is_disconnected = true;
			
		}
		catch (Exception e) 
		{			
			Log.println( "[GameLoop] error in update: " + e.getMessage() + "\n" + Log.getStackTrace( e ) );			
			is_disconnected = true;
		}
	}
	
	public boolean isDisconnected( )
	{		
		return is_disconnected ;
	}
	
	public void render( GLCanvas canvas )
	{		
		if( renderer == null )
		{
			renderer = new Renderer();
			if( shader_off )
				renderer.useGlow( false );
		}
		if( gui == null )
			gui = new GameInterface( this, canvas, sim, renderer );
		
		renderer.render(sim, gui, canvas);		
			
	}
	
	protected void handleMessages( ) throws Exception
	{
		Message msg = getClient().readMessage();							
		
		if( is_disconnected )
			return;
		
		if( msg == null )
			return;		
		
		if( msg instanceof CommandBufferMessage )
		{
			CommandBufferMessage cmd_msg = (CommandBufferMessage)msg;			
			sim.enqueueTurnCommandBufferMessage( cmd_msg );
			time_out.stop();
			time_out.start();
		}
		
		if( msg instanceof PingMessage )			
			ping.setLastPing( (System.nanoTime() - ((PingMessage)msg).getTimeStamp()) / 1000000.0 );
				
		if( msg instanceof GameOverMessage )
			game_over = true;
	}	
	
	public boolean isGameOver( )
	{
		return game_over;
	}

	public void setSimulation(Simulation sim) 
	{
		this.sim = sim;
		gui.setSimulation( sim );
		renderer.setSimulation( sim );
		game_over = false;
	}

	public void dispose() 
	{
		if( renderer != null )
			renderer.dispose();
		
		if( gui != null )
			gui.dispose();	
		
		if( recorder != null )
			recorder.dispose();
	}

	public void saveRecording( String file ) throws Exception
	{
		if( recorder != null )
			recorder.dispose( file );		
	}
	
	public void saveRecordingInGame( String file ) throws Exception
	{
		if( recorder != null )
			recorder.save( file );
	}
	
	public Simulation getSimulation() 
	{	
		return sim;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public Client getClient() {
		return client;
	}

	public GameInterface getGameInterface() 
	{	
		return gui;
	}
}
