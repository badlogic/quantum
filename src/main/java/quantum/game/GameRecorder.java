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

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

import quantum.net.messages.CommandBufferMessage;
import quantum.net.messages.PlayerListMessage;
import quantum.net.messages.SimulationMessage;
import quantum.utils.FileManager;
import quantum.utils.Log;

/**
 * class responsible for saving games. records the initial map setup
 * the names of the players as well as the continuous stream of 
 * commandbuffers.
 * 
 * @author Administrator
 *
 */
public class GameRecorder implements CommandTurnListener
{
	private DataOutputStream out;
	private ArrayList<CommandBufferMessage> commands = new ArrayList<CommandBufferMessage>();
	private String tmp_file;
	private Simulation sim;
	
	/**
	 * constructor, specify the file to save the game to.
	 * @param file
	 */
	public GameRecorder( SimulationMessage sim, PlayerListMessage players ) throws Exception
	{
		tmp_file = "tmp" + System.nanoTime() + ".rec";
		out = new DataOutputStream( new FileOutputStream( FileManager.newFile( tmp_file ) ) );
		Log.println( "[GameSaver] opened file '" + tmp_file + "' for output" );
		players.write( out );
		sim.write( out );	
		sim.getSimulation().addCommandTurnListener( this );
		this.sim = sim.getSimulation();
		FileManager.newFile( tmp_file ).deleteOnExit();
	}
	
	public GameRecorder( Simulation sim, PlayerListMessage players ) throws Exception
	{
		tmp_file = "tmp" + System.nanoTime() + ".rec";
		out = new DataOutputStream( new FileOutputStream( FileManager.newFile( tmp_file ) ) );
		Log.println( "[GameSaver] opened file '" + tmp_file + "' for output" );
		players.write( out );
		
		SimulationMessage sim_msg = new SimulationMessage( -1, sim );
		sim_msg.write( out );
		sim.addCommandTurnListener( this );
		this.sim = sim;
		FileManager.newFile( tmp_file ).deleteOnExit();
	}	
		
	
	public void dispose( String save_file ) throws Exception
	{
		sim.removeCommandTurnListener( this );
		
		for( CommandBufferMessage msg: commands )
			try {
				msg.write( out );
			} catch (Exception ex) {
				Log.println( "[GameSaver] couldn't write command buffers! " + Log.getStackTrace( ex ) );					
			}
		commands.clear();
		out.close();
		
		try
		{
			FileInputStream in = new FileInputStream( FileManager.newFile(tmp_file) );
			GZIPOutputStream out = new GZIPOutputStream( new FileOutputStream( FileManager.newFile( save_file ) ) ); 
		
		
			byte[] buf = new byte[4 * 1024]; 
			int bytesRead;		
			while ((bytesRead = in.read(buf)) != -1)
				out.write(buf, 0, bytesRead);			

			in.close();
			out.close();
			FileManager.newFile( tmp_file ).delete();
		}
		catch( Exception ex )
		{
			Log.println( "[GameRecorder] couldn't save file: " + Log.getStackTrace( ex ) );
			throw ex;
		}
	}
	
	public void dispose( )
	{
		sim.removeCommandTurnListener( this );
		
		try {
			for( CommandBufferMessage msg: commands )
				try {
					msg.write( out );
				} catch (Exception ex) {
					Log.println( "[GameSaver] couldn't write command buffers! " + Log.getStackTrace( ex ) );					
				}
			commands.clear();
			out.close();
			FileManager.newFile( tmp_file ).delete();
		} catch (IOException e) {
			// TODO Auto-generated catch block			
			e.printStackTrace();
		}
	}

	public void commandTurn(CommandBufferMessage messages) throws Exception
	{
		commands.add( messages );		
		if( commands.size() == 10 )
		{
			for( CommandBufferMessage msg: commands )
				try {
					msg.write( out );
				} catch (Exception e) {
					Log.println( "[GameSaver] couldn't write command buffers! " + Log.getStackTrace( e ) );
					throw e;
				}
			commands.clear();
		}
		
	}

	public void save(String file) throws Exception
	{
		try
		{
			FileInputStream in = new FileInputStream( FileManager.newFile(tmp_file) );
			GZIPOutputStream out = new GZIPOutputStream( new FileOutputStream( FileManager.newFile( file ) ) ); 
		
		
			byte[] buf = new byte[4 * 1024]; 
			int bytesRead;		
			while ((bytesRead = in.read(buf)) != -1)
				out.write(buf, 0, bytesRead);			

			in.close();
			out.close();
			FileManager.newFile( tmp_file ).delete();
		}
		catch( Exception ex )
		{
			Log.println( "[GameRecorder] couldn't save file: " + Log.getStackTrace( ex ) );
			throw ex;
		}		
	}
}
