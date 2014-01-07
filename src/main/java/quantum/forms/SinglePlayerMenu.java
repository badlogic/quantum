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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;

import quantum.Quantum;
import quantum.game.Bot;
import quantum.game.Player;
import quantum.game.Simulation;
import quantum.gfx.Color;
import quantum.gfx.Renderer;
import quantum.gui.Button;
import quantum.gui.ClickedListener;
import quantum.gui.Gui;
import quantum.gui.HorizontalAlignement;
import quantum.gui.HorizontalBoxContainer;
import quantum.gui.Image;
import quantum.gui.Label;
import quantum.gui.List;
import quantum.gui.ScreenAlignementContainer;
import quantum.gui.SelectedListener;
import quantum.gui.Spacer;
import quantum.gui.VerticalAlignement;
import quantum.gui.VerticalBoxContainer;
import quantum.gui.Widget;
import quantum.net.Client;
import quantum.utils.FileManager;
import quantum.utils.Log;

public class SinglePlayerMenu 
{
	class Map
	{
		String filename;
		String name;
		
		public Map( String filename, String name )
		{
			this.filename = filename;
			this.name = name;
		}
		
		public String toString( )
		{
			return name;
		}
		
		public String getFilename( )
		{
			return filename;
		}
	}
	
	Quantum quantum;
	Gui gui;
	ArrayList<Bot> bots = new ArrayList<Bot>();
	Client client;
	Renderer renderer;
	Simulation sim;
	
	public SinglePlayerMenu( final Quantum quantum, final Gui gui )
	{
		this.quantum = quantum;
		this.gui = gui;
		this.client = new Client( "Player" );
		this.renderer = new Renderer( );
		
		final ScreenAlignementContainer cont = new ScreenAlignementContainer( gui, HorizontalAlignement.CENTER, VerticalAlignement.TOP );
		Image image;
		try {
			gui.getCanvas().getContext().makeCurrent();
			image = new Image( gui, FileManager.readFile( "quantum.png" ) );
			cont.addWidget( new Spacer( gui, 0, 50 ) );
			cont.addWidget( image, HorizontalAlignement.CENTER );
		} catch (Exception e) {
			Log.println( "Couldn't load image 'quantum.png'" );
			gui.showConfirmDialog( "Couldn't load image 'quantum.png'. Your setup is probably borked.", "Error" );			
		}				
		gui.add( cont );
		
		//
		// bot & map selection
		//
		HorizontalBoxContainer h_box = new HorizontalBoxContainer( gui );		
		VerticalBoxContainer v_box = new VerticalBoxContainer( gui );
				
		final List available_bots = new List( gui );
		available_bots.setSize( 200, 200 );	
		available_bots.setBackgroundColor( 0, 0, 0, 1 );
		
		File file = FileManager.newFile( "dat/scripts/" );
		Log.println( "[SinglePlayerMenu] listing bots files in '" + FileManager.getPath() + "dat/scripts/" );
		String[] bot_files = file.list( new FilenameFilter( ) {
			
			public boolean accept(File dir, String name) {					
				return name.endsWith(".bsh");
			}				
		} );
		
		for( String bot_file: bot_files )
		{
			available_bots.addItem( bot_file );
		}		
		
		available_bots.setSelectedItem( 0 );
		
		v_box.addWidget( new Label( gui, "Available Bots" ), HorizontalAlignement.LEFT );
		v_box.addWidget( new Spacer( gui, 0, 5 ) );
		v_box.addWidget( available_bots );		
		
		Button add_bot = new Button( gui, "Add Bot" );
		add_bot.setSize( 70, 25 );
		
		v_box.addWidget( new Spacer( gui, 0, 5 ) );
		v_box.addWidget( add_bot, HorizontalAlignement.LEFT );		
		h_box.addWidget( v_box );
		
		
		v_box = new VerticalBoxContainer( gui );
		final List selected_bots = new List( gui );
		selected_bots.setSize( 200, 200 );
		selected_bots.setBackgroundColor( 0, 0, 0, 1 );
		v_box.addWidget( new Label( gui, "Selected Bots" ), HorizontalAlignement.LEFT );
		v_box.addWidget( new Spacer( gui, 0, 5 ) );
		v_box.addWidget( selected_bots );
		h_box.addWidget( new Spacer( gui, 10, 0 ) );
		h_box.addWidget( v_box );
		
		Button remove_bot = new Button( gui, "Remove Bot" );
		remove_bot.setSize( 70, 25 );
		v_box.addWidget( new Spacer( gui, 0, 5 ) );
		v_box.addWidget( remove_bot );
		
		cont.addWidget( h_box, HorizontalAlignement.CENTER );
		
		add_bot.setClickedListener( new ClickedListener( ) 
		{
			public void clicked(Widget widget) 
			{			
				if( available_bots.getSelectedItem() == null )
					return;
				
				if( bots.size() + 1 >= sim.getPlanets().size() )
				{
					gui.showConfirmDialog( "Too many players, not enough planets!", "Error" );
					return;
				}
				
				try {
					Bot bot = new Bot( FileManager.getPath() + "dat/scripts/" + available_bots.getSelectedItem().toString(), bots.size() + 1 );					
					Player player =  new Player(available_bots.getSelectedItem().toString().replace( ".bsh", "" ) + " " + bot.getId(), bot.getId() );
					bots.add( bot );
					client.addPlayer( player );
					selected_bots.addItem( player );
				} 
				catch (Exception e) 
				{
					Log.println( "[SinglePlayerMenu] couldn't load bot: " + Log.getStackTrace( e ) );
					gui.showConfirmDialog( "couldn't load bot\n" + e.getMessage(), "Error" );					
				}
			}			
		});
		
		remove_bot.setClickedListener( new ClickedListener( ){

			public void clicked(Widget widget) 
			{				 
				if( selected_bots.getSelectedItem() == null )
					return;
				
				Player player = (Player)selected_bots.getSelectedItem();
				Iterator<Bot> iter = bots.iterator();
				while( iter.hasNext() )
					if( iter.next().getId() == player.getId() )
						iter.remove();
				client.getPlayers().remove( player );
				selected_bots.removeItem( selected_bots.getSelectedItem() );
			}			
		});
		
		List map_list = new List( gui );
		map_list.setSize( 200, 200 );
		map_list.setBackgroundColor( 0, 0, 0, 1 );
		
		file = FileManager.newFile( "dat/maps/" );
		final String[] map_files = file.list( new FilenameFilter( ) {
			
			public boolean accept(File dir, String name) {					
				return name.endsWith(".map");
			}				
		} );
		
		for( String f: map_files )
		{
			Simulation sim = new Simulation( false );
			try {
				sim.load( FileManager.getPath() + "dat/maps/" + f );
				map_list.addItem( new Map( f, sim.getName() + " - " + sim.getAuthor() ) );
			} catch (Exception e) {
				Log.println( "[SinglePlayerMenu] map file corrupt '" + f + "': " + Log.getStackTrace( e ) );
			}			
		}
		
		map_list.setSelectedItem( 0 );
		
		v_box = new VerticalBoxContainer( gui );
		v_box.addWidget( new Label( gui, "Maps" ) );
		v_box.addWidget( new Spacer( gui, 0, 5 ) );
		v_box.addWidget( map_list );
		
		h_box.addWidget( new Spacer( gui, 10, 0 ) );
		h_box.addWidget( v_box );
		
		v_box = new VerticalBoxContainer( gui );
		final Image map_image = new Image( gui, new BufferedImage( 256, 256, BufferedImage.TYPE_3BYTE_BGR) );
		map_image.setWidth(200);
		map_image.setHeight(200);
		map_image.setBorderColor( new Color( 1, 1, 1, 1 ) );
		
		v_box.addWidget( new Label( gui, "Selected Map" ) );
		v_box.addWidget( new Spacer( gui, 0, 5 ) );
		v_box.addWidget( map_image );		
		
		h_box.addWidget( new Spacer( gui, 10, 0 ) );		
		h_box.addWidget( v_box );
		
		sim = new Simulation( true );
		try {
			sim.load(  FileManager.getPath() + "dat/maps/" + map_files[0] );
			map_image.setImage( renderer.takeCenteredScreenShot( gui.getCanvas(), sim ) );
		} catch (Exception e) {
			Log.println( "[SinglePlayerMenu] couldn't take map screenshot: " + Log.getStackTrace( e ) );			
		}		
		
		map_list.setSelectedListener( new SelectedListener( ) {

			public void selected(Widget widget, Object selection) 
			{
				if( selection == null )
					return;
				
				try {
					sim.load( FileManager.getPath() + "dat/maps/" + ((Map)selection).getFilename() );
					map_image.setImage( renderer.takeCenteredScreenShot( gui.getCanvas(), sim ) );
				} catch (Exception e) {
					Log.println( "[SinglePlayerMenu] couldn't take map screenshot: " + Log.getStackTrace( e ) );
				}				
			}			
		});
		
		//
		// Buttons for start and back to main menu
		//
		Button back = new Button( gui, "Back" );			
		back.setSize( 70, 25 );		
		back.setClickedListener( new ClickedListener() 
		{					
			public void clicked(Widget widget) 
			{
				renderer.dispose();
				gui.remove( cont );							
				new StartMenu(quantum, gui);			
			}			
		});		
		
		Button start = new Button( gui, "Start" );
		start.setSize( 70, 25 );
		start.setClickedListener( new ClickedListener( ) 
		{
			public void clicked(Widget widget) 
			{			
				if( bots.size() == 0 )
				{
					gui.showConfirmDialog( "You have to add at least one bot!" , "Information" );
					return;
				}
				
				renderer.dispose();
				gui.remove( cont );							
				new LocalGame(quantum, gui, sim, client, bots );
			}			
		});
		
		h_box = new HorizontalBoxContainer( gui );
		h_box.addWidget( start );
		h_box.addWidget( back);
		cont.addWidget( new Spacer( gui, 0, 20 ) );
		cont.addWidget( h_box, HorizontalAlignement.RIGHT );
	}
}
