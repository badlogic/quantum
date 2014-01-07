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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.HashMap;

import javax.media.opengl.GLCanvas;

import quantum.Quantum;
import quantum.Quantum.DisplayListener;
import quantum.game.Simulation;
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
import quantum.utils.FileManager;
import quantum.utils.Log;

public class MapMenu implements DisplayListener
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
	
	MapMenu self = this;
	Renderer renderer;
	
	HashMap<String, Simulation> sims = new HashMap<String, Simulation>( );
	
	public MapMenu( final Quantum quantum, final Gui gui )
	{
		quantum.addDisplayListener( this );
		renderer = new Renderer( );
		final ScreenAlignementContainer cont2 = new ScreenAlignementContainer( gui, HorizontalAlignement.CENTER, VerticalAlignement.TOP );
		Image image;
		try {
			gui.getCanvas().getContext().makeCurrent();
			image = new Image( gui, FileManager.readFile( "quantum.png" ) );
			cont2.addWidget( new Spacer( gui, 0, 50 ) );
			cont2.addWidget( image );
		} catch (Exception e) 
		{
			Log.println( "[CreateMenu] couldn't load image 'quantum.png'" );
			gui.showConfirmDialog( "Couldn't load image 'quantum.png'. Your setup is probably borked.", "Error" );			
		}				
		gui.add( cont2 );
		
		final ScreenAlignementContainer cont = new ScreenAlignementContainer( gui, HorizontalAlignement.CENTER, VerticalAlignement.CENTER );
		Button back = new Button( gui, "Back" );
		back.setSize( 70, 25 );		
		back.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) 
			{
				quantum.removeDisplayListener( self );
				gui.remove( cont );
				gui.remove( cont2 );
				renderer.dispose();
				new StartMenu( quantum, gui );
			}
			
		} );
		
		
		final List local_maps = new List( gui );
		local_maps.setBackgroundColor( 0, 0, 0, 1 );
		local_maps.setSize( 200, 200 );
		
		String[] maps = FileManager.newFile( "dat/maps/" ).list( new FilenameFilter( ) 
		{
			public boolean accept(File arg0, String arg1) 
			{			
				return arg1.endsWith( ".map" );
			}		
		});
		
		for( String f: maps )
		{
			Simulation sim = new Simulation( false );
			try {
				sim.load( FileManager.getPath() + "dat/maps/" + f );
				local_maps.addItem( new Map( f, sim.getName() + " - " + sim.getAuthor() ) );
			} catch (Exception e) {
				Log.println( "[SinglePlayerMenu] map file corrupt '" + f + "': " + Log.getStackTrace( e ) );
			}			
		}
				
		
		Simulation sim = new Simulation( false );
		
		if( maps.length > 0 )
		{
			local_maps.setSelectedItem( 0 );			
			try {
				sim.load( FileManager.getPath() +  "dat/maps/" + maps[0] );
			} catch (Exception e) 
			{			
				Log.println( "[MapMenu] couldn't create image of map: " + Log.getStackTrace( e ) );
				gui.showConfirmDialog( "Couldn't create image of map. Map file is corrupted.", "Error" );
			}
		}
						
		final Image local_image = new Image( gui, renderer.takeCenteredScreenShot( gui.getCanvas(), sim) );
		local_image.setSize( 200, 200 );	
		local_image.setBorderColor( 1, 1, 1, 1 );
		
		Button upload = new Button( gui, "Upload" );
		upload.setSize( 80, 25 );
		
		Button delete = new Button( gui, "Delete" );
		delete.setSize( 80, 25 );
		
		HorizontalBoxContainer h_box = new HorizontalBoxContainer( gui );
		h_box.addWidget( local_maps );
		h_box.addWidget( new Spacer( gui, 10, 0 ) );
		h_box.addWidget( local_image );
		h_box.addWidget( new Spacer( gui, 10, 0 ) );
		
		VerticalBoxContainer v_box = new VerticalBoxContainer( gui );
		v_box.addWidget( upload );
		v_box.addWidget( new Spacer( gui, 0, 5 ) );
		v_box.addWidget( delete );
		
		h_box.addWidget( v_box, VerticalAlignement.CENTER );
				
		cont.addWidget( new Label( gui, "Local Maps" ) );
		cont.addWidget( new Spacer( gui, 5, 10 ) );
		cont.addWidget( h_box );
		
		
		final List net_maps = new List( gui );
		net_maps.setSize( 200, 200 );
		net_maps.setBackgroundColor( 0, 0, 0, 1 );
		
		try
		{
			String[] list = FileManager.getMapList();
			
			if( list.length != 0 )
			{							
				for( String map: list )
					net_maps.addItem( map );
				
				net_maps.setSelectedItem( 0 );
				
				sim = new Simulation( false );
				sim.load( FileManager.downloadMap( list[0] ) );
				
				sims.put( list[0], sim );
			}
			
		}
		catch( Exception ex )
		{
			Log.println( "[MapMenu] couldn't get map list: " + Log.getStackTrace( ex ) );
			gui.showConfirmDialog( "Couldn't retrieve map list from server", "Error" );
		}
		
		final Image net_images = new Image( gui, new BufferedImage( 200, 200, BufferedImage.TYPE_4BYTE_ABGR ) );
		net_images.setSize( 200, 200 );
		net_images.setBorderColor( 1, 1, 1, 1 );
		
		net_images.setImage( renderer.takeCenteredScreenShot( gui.getCanvas(), sim ) );
		
		Button download = new Button( gui, "Download" );
		download.setSize( 80, 25 );
		
		
		h_box = new HorizontalBoxContainer( gui );
		h_box.addWidget( net_maps );
		h_box.addWidget( new Spacer( gui, 10, 0 ) );
		h_box.addWidget( net_images );
		h_box.addWidget( new Spacer( gui, 10, 0 ) );
		h_box.addWidget( download, VerticalAlignement.CENTER );
		cont.addWidget( new Spacer( gui, 5, 20 ) );
		cont.addWidget( new Label( gui, "Server Maps" ) );
		cont.addWidget( new Spacer( gui, 5, 10 ) );
		cont.addWidget( h_box );
		
		cont.addWidget( new Spacer( gui, 5, 20 ) );
		cont.addWidget( back, HorizontalAlignement.CENTER );
		
		gui.add( cont );
		
		
		net_maps.setSelectedListener( new SelectedListener( ) {

			public void selected(Widget widget, Object selection) 
			{			
				if( selection == null )
					return;
				
				Simulation sim = new Simulation( true );
				if( sims.containsKey( selection.toString() ) == false )
				{					
					try {
						sim.load( FileManager.downloadMap( selection.toString() ) );
						sims.put( selection.toString(), sim );
					} catch (Exception e) 
					{
						Log.println( "[MapMenu] couldn't download map: " + Log.getStackTrace( e ) );
						gui.showConfirmDialog( "Couldn't retrieve map '" + selection.toString() + "'", "Error" );
					}
				}
				else
					sim = sims.get( selection.toString() );
				
				net_images.setImage( renderer.takeCenteredScreenShot( gui.getCanvas(), sim) );
			}
			
		});
		
		download.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) 
			{			
				if( net_maps.getSelectedItem() == null )
					return;
				
				try 
				{																				
					
					String[] maps = FileManager.newFile( "dat/maps/" ).list( new FilenameFilter( ) 
					{
						public boolean accept(File arg0, String arg1) 
						{			
							return arg1.endsWith( ".map" );
						}		
					});
					
					String map_name = net_maps.getSelectedItem().toString();
					while( true )
					{
						boolean no_conflict = true;
						for( String item: maps )
							if( item.equalsIgnoreCase( map_name ) )
							{
								map_name = map_name.replace( ".map", "_.map" );
								no_conflict = false;
								break;
							}
						
						if( no_conflict )
							break;
					}
					
					sims.get(net_maps.getSelectedItem().toString()).writeState( new DataOutputStream( new FileOutputStream( FileManager.newFile( "dat/maps/" + map_name))));
					refreshLocalList( );
				} catch (Exception e) 
				{
					Log.println( "[MapMenu] couldn't download map: " + Log.getStackTrace( e ) );
					e.printStackTrace();
				}
			}		
			
			private void refreshLocalList( ) {
				String[] maps = FileManager.newFile( "dat/maps/" ).list( new FilenameFilter( ) 
				{
					public boolean accept(File arg0, String arg1) 
					{			
						return arg1.endsWith( ".map" );
					}		
				});
				
				local_maps.removeAll();
				
				for( String f: maps )
				{
					Simulation sim = new Simulation( false );
					try {
						sim.load( FileManager.getPath() + "dat/maps/" + f );
						local_maps.addItem( new Map( f, sim.getName() + " - " + sim.getAuthor() ) );
					} catch (Exception e) {
						Log.println( "[SinglePlayerMenu] map file corrupt '" + f + "': " + Log.getStackTrace( e ) );
					}			
				}	
				
				Simulation sim = new Simulation( false );
				
				if( maps.length > 0 )
				{
					local_maps.setSelectedItem( 0 );			
					try {
						sim.load( FileManager.getPath() +  "dat/maps/" + maps[0] );
					} catch (Exception e) 
					{			
						Log.println( "[MapMenu] couldn't create image of map: " + Log.getStackTrace( e ) );
						gui.showConfirmDialog( "Couldn't create image of map. Map file is corrupted.", "Error" );
					}
				}
				local_image.setImage( renderer.takeCenteredScreenShot( gui.getCanvas(), sim ) );
			}
		});
		
		delete.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) 
			{
				if( local_maps.getSelectedItem() == null )
					return;
				
				FileManager.newFile( "dat/maps/" + ((Map)local_maps.getSelectedItem()).getFilename() ).delete();
				refreshLocalList( );
			}
			
			private void refreshLocalList( ) {
				String[] maps = FileManager.newFile( "dat/maps/" ).list( new FilenameFilter( ) 
				{
					public boolean accept(File arg0, String arg1) 
					{			
						return arg1.endsWith( ".map" );
					}		
				});
				
				local_maps.removeAll();
				
				for( String f: maps )
				{
					Simulation sim = new Simulation( false );
					try {
						sim.load( FileManager.getPath() + "dat/maps/" + f );
						local_maps.addItem( new Map( f, sim.getName() + " - " + sim.getAuthor() ) );						
					} catch (Exception e) {
						Log.println( "[SinglePlayerMenu] map file corrupt '" + f + "': " + Log.getStackTrace( e ) );
					}			
				}			
				
				Simulation sim = new Simulation( false );
				
				if( maps.length > 0 )
				{
					local_maps.setSelectedItem( 0 );			
					try {
						sim.load( FileManager.getPath() +  "dat/maps/" + maps[0] );
					} catch (Exception e) 
					{			
						Log.println( "[MapMenu] couldn't create image of map: " + Log.getStackTrace( e ) );
						gui.showConfirmDialog( "Couldn't create image of map. Map file is corrupted.", "Error" );
					}
				}
				local_image.setImage( renderer.takeCenteredScreenShot( gui.getCanvas(), sim ) );
			}
			
		});
		
		local_maps.setSelectedListener( new SelectedListener( ) {

			public void selected(Widget widget, Object selection) 
			{			
				if( selection == null )
					return;
				
				Simulation sim = new Simulation( false );
				try {
					sim.load( FileManager.getPath() +  "dat/maps/" + ((Map)selection).getFilename() );
					local_image.setImage( renderer.takeCenteredScreenShot( gui.getCanvas(), sim) );
				} catch (Exception e) 
				{
					Log.println( "[MapMenu] couldn't create image of map: " + Log.getStackTrace( e ) );
					gui.showConfirmDialog( "Couldn't create image of map. Map file is corrupted.", "Error" );
				}								
			}
			
		});
		
		upload.setClickedListener( new ClickedListener( ) {

			public void clicked(Widget widget) 
			{				
				if( local_maps.getSelectedItem() == null )
					return;
				
				try {
					Simulation sim = new Simulation( false );
					try
					{
						sim.load( new FileInputStream( FileManager.getPath() + "dat/maps/" + ((Map)local_maps.getSelectedItem()).getFilename()) );
					}
					catch( Exception ex )
					{
						Log.println( "[MapMenu] tried to upload corrupt file" );
						gui.showConfirmDialog( "Couldn't upload map, file is corrupt", "Error" );
						return;
					}
					
					String map_name = ((Map)local_maps.getSelectedItem()).getFilename();
					while( true )
					{
						boolean no_conflict = true;
						for( Object item: net_maps.getItems( ) )
							if( item.toString().equalsIgnoreCase( map_name ) )
							{
								map_name = map_name.replace( ".map", "_.map" );
								no_conflict = false;
								break;
							}
						
						if( no_conflict )
							break;
					}
					
					FileManager.uploadMap( ((Map)local_maps.getSelectedItem()).getFilename(), map_name );
					Thread.sleep( 1000 );
					refreshNetList( );
				} catch (Exception e) {
					Log.println( "[MapMenu] couldn't upload map '" + local_maps.getSelectedItem().toString() + "': " + Log.getStackTrace( e ) );
					gui.showConfirmDialog( "Couldn't upload map '" + local_maps.getSelectedItem().toString() + "'", "Error" );
				}
			}
			
			private void refreshNetList( )
			{
				net_maps.removeAll();
				
				Simulation sim = new Simulation( false );
				try
				{
					String[] list = FileManager.getMapList();
					
					if( list.length != 0 )
					{							
						for( String map: list )
							net_maps.addItem( map );
						
						net_maps.setSelectedItem( 0 );
						
						sim.load( FileManager.downloadMap( list[0] ) );						
						sims.put( list[0], sim );
					}
					
				}
				catch( Exception ex )
				{
					Log.println( "[MapMenu] couldn't get map list: " + Log.getStackTrace( ex ) );
					gui.showConfirmDialog( "Couldn't retrieve map list from server", "Error" );
					return;
				}
				
				net_images.setImage( renderer.takeCenteredScreenShot( gui.getCanvas(), sim ) );
			}
			
		});
	}	
	
	public void display(GLCanvas canvas) 
	{	
		try {
			Thread.sleep( 10 );
		} catch (InterruptedException e) {
		}
	}
}
