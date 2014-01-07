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
package quantum.forms;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.zip.GZIPInputStream;

import javax.media.opengl.GLCanvas;

import quantum.Quantum;
import quantum.Quantum.DisplayListener;
import quantum.game.GameLoop;
import quantum.game.Simulation;
import quantum.gui.Button;
import quantum.gui.ClickedListener;
import quantum.gui.CustomDialog;
import quantum.gui.Gui;
import quantum.gui.HorizontalAlignement;
import quantum.gui.List;
import quantum.gui.ScreenAlignementContainer;
import quantum.gui.VerticalAlignement;
import quantum.gui.VerticalBoxContainer;
import quantum.gui.Widget;
import quantum.net.Client;
import quantum.net.messages.CommandBufferMessage;
import quantum.net.messages.MessageDecoder;
import quantum.net.messages.PlayerListMessage;
import quantum.net.messages.SimulationMessage;
import quantum.utils.FileManager;
import quantum.utils.Log;

public class Replay implements DisplayListener
{	
	private Simulation sim;
	private GameLoop loop;
	private Replay self = this;
	
	public Replay( final Quantum quantum, final Gui gui )
	{
		quantum.addDisplayListener( this );
		VerticalBoxContainer v_box = new VerticalBoxContainer( gui );
		Button load = new Button( gui, "Load" );
		Button back = new Button( gui, "Back" );
		load.setSize( 75, 25 );
		back.setSize( 75, 25 );		
		
		String[] files = FileManager.newFile( "dat/recordings/" ).list( new FilenameFilter() 
		{
			public boolean accept(File dir, String name) {
				return name.endsWith( ".rec" );
			}			
		});
		
		final List replays = new List( gui );
		replays.setBackgroundColor( 0, 0, 0, 1 );
		replays.setSize( 250, 120 );
		for( String file: files )
			replays.addItem( file );
		
		if( files.length > 0 )
			replays.setSelectedItem( 0 );
		v_box.addWidget( replays );				
		
		final CustomDialog dialog = new CustomDialog( gui, 250, "Load Replay File", v_box, load, back );
		gui.add( dialog );		
		
		back.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) {
				quantum.removeDisplayListener( self );
				gui.remove( dialog );					
				new StartMenu( quantum, gui );				
			}			
		});		
		
		load.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) 
			{			
				try
				{
					load( FileManager.getPath() + "dat/recordings/" + replays.getSelectedItem().toString() );
					gui.remove( dialog );		
					
					final ScreenAlignementContainer cont = new ScreenAlignementContainer( gui, HorizontalAlignement.RIGHT, VerticalAlignement.TOP );
					Button back = new Button( gui, "Back" );
					back.setSize( 75, 25 );
					cont.addWidget( back );
					gui.add( cont );
					
					back.setClickedListener( new ClickedListener() {

						public void clicked(Widget widget) {
							gui.remove( cont );
							quantum.removeDisplayListener( self );
							loop.dispose();		
							new StartMenu( quantum, gui );
						}
						
					});
				}
				catch( Exception ex )
				{
					Log.println( "[Replay] couldn't load replay: " + Log.getStackTrace( ex ) );
					gui.showConfirmDialog("Couldn't load replay", "Error" );
				}
			}			
		});
	}	
	
	public void load( String file ) throws Exception
	{
		Log.println( "[Replay] trying to open '" + file + "'" );
		DataInputStream in = new DataInputStream( new GZIPInputStream( new FileInputStream( file ) ) );
		PlayerListMessage player_msg = (PlayerListMessage)MessageDecoder.decode( in );
		SimulationMessage sim_msg = (SimulationMessage)MessageDecoder.decode( in );
		Client client = new Client( "Replay" );
		client.setPlayerList( player_msg );
		sim = sim_msg.getSimulation();
		//sim.setLocalInput( true );
		loop = new GameLoop( client, sim );		
		
		try
		{
			while( in.available() > 0 )
			{			
				CommandBufferMessage msg = (CommandBufferMessage)MessageDecoder.decode(in);						
				sim.enqueueTurnCommandBufferMessage( msg );
			}
		}
		catch( EOFException ex )
		{
			
		}
		in.close();	
	}

	public void display(GLCanvas canvas) 
	{	
		if( loop != null )
		{
			try {			
				loop.update((GLCanvas)canvas);
				loop.render((GLCanvas)canvas);
				loop.getGameInterface().setIsReplay( true );
					
				Thread.sleep( 0 );
			} catch (InterruptedException e) 
			{
				Log.println( "[Replay] couldn't do replay: " + Log.getStackTrace( e ) );
				e.printStackTrace();
			}	
		}
	}
}
